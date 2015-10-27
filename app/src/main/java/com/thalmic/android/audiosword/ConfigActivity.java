package com.thalmic.android.audiosword;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.glass.companion.Proto;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class ConfigActivity extends Activity implements GlassDevice.GlassConnectionListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        TextToSpeech.OnInitListener {
    private static final String TAG = "ConfigActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY = "LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "LAST_UPDATED_TIME_STRING_KEY";
    private static final String SPEAK_BOOLEAN = "SPEAK_BOOLEAN";


    private MyoRemoteService mService;
    private StopReceiver mStopReceiver = new StopReceiver();

    private AppPrefs mPrefs;
    private DeviceListener mListener;

    private TextView mMyoStatusView;
    private TextView mGlassStatusView;
    private ImageView mScreencastView;
    private Button mScreencastButton;
    private TextView mArmView;
    private TextView mPoseView;

    private GlassDevice mGlass;
    private boolean mScreencastEnabled = false;


    // Location Services
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mLatitudeText;
    private String mLongitudeText;
    private Pose pose;
    private TextToSpeech tts;
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    protected boolean mAddressRequested;

    public static final String MyPREFERENCES = "MyPrefs";
    SharedPreferences sharedpreferences;
    public static final String SpeakBoolean = "speakBoolean";
    private boolean speak = false;

    /* Phone menu items */

    private String[] primaryMenu = {"Favourites", "Emergency"};
    private String[] secondaryMenu = {"Missed", "Dialed", "Received"};
    private String[] favourites = {"Davide", "Alex", "Lisa", "John"};
    private String[] emergency = {"Davide", "Alex", "Lisa", "John"};
    private String[] missed = {"Davide", "Alex", "Lisa", "John"};
    private String[] dialed = {"Davide", "Alex", "Lisa", "John"};
    private String[] received = {"Davide", "Alex", "Lisa", "John"};
    private String[] dummy = {"a", "b"};
    private String[][] callLists = {dummy, favourites,  emergency, missed, dialed, received};
    private String[][] menus = { dummy, primaryMenu, secondaryMenu};
    private int directionIndicator = 0;
    private int menuCursorPosition = 0;
    private String currentMenuName = "todo";
    private int navLevel = 1;
    private String currentContact = "noOne";
    /**
     * The formatted location address.
     */
    protected String mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * Displays the location address.
     */
    protected TextView mLocationAddressTextView;


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyoRemoteService.MBinder mbinder = ((MyoRemoteService.MBinder) service);
            mService = mbinder.getService();

            if (mListener == null) {
                mListener = new MyoListener();
                Hub.getInstance().addListener(mListener);
            }

            mGlass = mService.getMyoRemote().getGlassDevice();
            mGlass.registerListener(ConfigActivity.this);
            updateGlassStatus(mGlass.getConnectionStatus());

            String glassAddress = mPrefs.getGlassAddress();
            if (!TextUtils.isEmpty(glassAddress)) {
                mService.getMyoRemote().connectGlassDevice(glassAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);
        mMyoStatusView = (TextView) findViewById(R.id.myo_status);
        mGlassStatusView = (TextView) findViewById(R.id.glass_status);
        mScreencastView = (ImageView) findViewById(R.id.screenshot);
        mScreencastButton = (Button) findViewById(R.id.stop_button);
        mPoseView = (TextView) findViewById(R.id.pose);
        mArmView = (TextView) findViewById(R.id.arm);

        updateScreencastState();

        mPrefs = new AppPrefs(this);

        Intent intent = new Intent(this, MyoRemoteService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        registerReceiver(mStopReceiver, new IntentFilter(MyoRemoteService.ACTION_STOP_MYO_GLASS));

        mResultReceiver = new AddressResultReceiver(new Handler());
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        //Text to speech initialization
        tts = new TextToSpeech(this, this);

        // Updating values from save instances
        updateValuesFromBundle(savedInstanceState);

        //Update Shared preferences
        mPrefs = new AppPrefs(this);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        if (sharedpreferences.contains(SpeakBoolean)) {
            speak = sharedpreferences.getBoolean(SpeakBoolean, false);

        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Hub.getInstance().removeListener(mListener);
        unregisterReceiver(mStopReceiver);
        unbindService(mServiceConnection);
        mGlass.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGlass.stopScreenshot();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.kill_myglass:
                killMyGlass();
                return true;
            case R.id.speak_text:
                SharedPreferences.Editor editor = sharedpreferences.edit();

                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    speak = true;
                } else {
                    speak = false;
                }
                editor.putBoolean(SpeakBoolean, speak);
                editor.commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void killMyGlass() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:com.google.glass.companion"));
        startActivity(intent);
    }

    public void onScreencastBtn(View view) {
        setScreencastEnabled(!mScreencastEnabled);
        if (mScreencastEnabled) {
            mGlass.requestScreenshot();
        } else {
            mGlass.stopScreenshot();
        }
        stopLocationUpdates();
        speakOut("Location Updates Stopped");
    }

    public void onGetLocation(View view) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mCurrentLocation != null) {
            mLatitudeText = (String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText = (String.valueOf(mLastLocation.getLongitude()));

        }
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setScreencastEnabled(boolean enable) {
        mScreencastEnabled = enable;
        updateScreencastState();
    }

    private void updateScreencastState() {
        //mScreencastButton.setText(mScreencastEnabled ? R.string.stop : R.string.start);
        //mScreencastView.setVisibility(mScreencastEnabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateGlassStatus(GlassDevice.ConnectionStatus connectionStatus) {
        mGlassStatusView.setText(connectionStatus.name());
    }

    public void onChooseMyoClicked(View view) {
        if (mService == null) {
            Log.w(TAG, "No MyoRemoveService. Can't choose Myo.");
            return;
        }

        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void onChooseGlassClicked(View view) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_devices_title)
                    .setMessage(R.string.no_devices_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] glassNames = new String[pairedDevices.size()];
        final String[] glassAddresses = new String[pairedDevices.size()];
        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            glassNames[i] = device.getName();
            glassAddresses[i] = device.getAddress();
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_glass);
        builder.setItems(glassNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mGlass != null && mGlass.getConnectionStatus() == GlassDevice.ConnectionStatus.CONNECTED) {
                    mService.getMyoRemote().closeGlassDevice();
                }
                mService.getMyoRemote().connectGlassDevice(glassAddresses[which]);

                // Remember MAC address for next time.
                mPrefs.setGlassAddress(glassAddresses[which]);
            }
        });
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(ConfigActivity.this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        return;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        return;
    }

    private class StopReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    private class MyoListener extends AbstractDeviceListener {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            mPrefs.setMyoAddress(myo.getMacAddress());
            mMyoStatusView.setText(R.string.connected);
            mPoseView.setText("LOCKED");
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            mMyoStatusView.setText(R.string.disconnected);
            mArmView.setText("");
            mPoseView.setText("");
        }

        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArmView.setText(arm == Arm.LEFT ? R.string.myo_arm_left : R.string.myo_arm_right);
        }

        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mArmView.setText(R.string.myo_arm_unknown);
        }

        @Override
        public void onPose(Myo myo, long timestamp, final Pose pose) {
            mPoseView.setText(pose.name());
            if (pose == pose.FIST) {
                //startLocationUpdates();
                //speakOut("Wave right for favourites");

            } else if (pose == pose.FINGERS_SPREAD) {
                menuCursorPosition = 0;
                directionIndicator = 0;
                if (navLevel > 1) {
                    navLevel--;
                }
                speakOut("Wave right for favourites");
                //stopLocationUpdates();
                //speakOut("Location Updates Stopped");
            } else if (pose == pose.WAVE_OUT) {
                if(navLevel == 1) {
                    if(directionIndicator == 0){
                        directionIndicator = 1;
                    }
                    incrementMenuCursorPosition(menus[directionIndicator]);
                    currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                    speakOut(currentMenuName);
                } else if(navLevel == 2){
                    incrementMenuCursorPosition(callLists[1]);
                    currentContact = callLists[1][menuCursorPosition-1];
                    speakOut(currentContact);
                }

            } else if(pose == pose.WAVE_IN) {
                if(navLevel == 1) {
                    if(directionIndicator == 0) {
                        directionIndicator = 2;
                    }
                    decrementMenuCursorPosition(menus[directionIndicator]);
                    currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                    speakOut(currentMenuName);
                } else if(navLevel == 2) {
                    decrementMenuCursorPosition(callLists[1]);
                    currentContact = callLists[1][menuCursorPosition-1];
                    speakOut(currentContact);
                }

            } else if(pose == pose.DOUBLE_TAP) {
                String s = currentMenuName.toLowerCase();
                if (s.equals("favourites")) {
                    navLevel = 2;
                    menuCursorPosition = 0;
                    speakOut("Wave right for contact names");

                } else if (s.equals("missed")) {
                } else if (s.equals("dialed")) {
                } else if (s.equals("received")) {
                } else if (s.equals("emergency")) {
                } else {
                }
            }
        }

        @Override
        public void onLock(Myo myo, long timestamp) {
            mPoseView.setText("LOCKED");
        }

        @Override
        public void onUnlock(Myo myo, long timestamp) {
            mPoseView.setText("UNLOCKED");
        }
    }

    public void openList(String listName){

    }

    public void incrementMenuCursorPosition(String[] array) {
        if (menuCursorPosition < (array.length)) {
            menuCursorPosition++;
        } else {
            menuCursorPosition = 1;
        }
    }

    public void decrementMenuCursorPosition(String[] array) {
        if (menuCursorPosition > 1) {
            menuCursorPosition--;
        } else {
            menuCursorPosition = array.length;
        }
    }

    @Override
    public void onConnectionStatusChanged(GlassDevice.ConnectionStatus status) {
        updateGlassStatus(status);
        if (status != GlassDevice.ConnectionStatus.DISCONNECTED) {
            setScreencastEnabled(false);
        }
    }

    // Called when a message from Glass is received
    public void onReceivedEnvelope(Proto.Envelope envelope) {
        if (envelope.screenshot != null) {
            if (envelope.screenshot.screenshotBytesG2C != null) {
                InputStream in = new ByteArrayInputStream(envelope.screenshot.screenshotBytesG2C);
                final Bitmap bp = BitmapFactory.decodeStream(in);

                // Update the UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mScreencastView.setImageBitmap(bp);
                    }
                });
            }
        }
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            String lines[] = mAddressOutput.split("\\r?\\n");
            //showToast(lines[1]);
            //speakOut(lines[1]);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //showToast(getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {

            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    private void speakOut(String text) {
        //String text = txtText.getText().toString();
        if (!text.equals("STOP") && speak) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        startIntentService();
        updateUI();
    }

    private void updateUI() {
        //mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
        //mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
        //mLastUpdateTimeTextView.setText(mLastUpdateTime);
    }

    protected void startLocationUpdates() {
        mRequestingLocationUpdates = true;
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        mRequestingLocationUpdates = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putBoolean(SPEAK_BOOLEAN, speak);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }

            // Update the value of speak boolean from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(SPEAK_BOOLEAN)) {
                speak = savedInstanceState.getBoolean(
                        SPEAK_BOOLEAN);
            }
            updateUI();
        }
    }
}