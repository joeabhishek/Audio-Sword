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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.glass.companion.Proto;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class YelpActivity extends Activity implements GlassDevice.GlassConnectionListener,
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
    public static TextToSpeech tts;
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    protected boolean mAddressRequested;

    public static final String MyPREFERENCES = "MyPrefs";
    SharedPreferences sharedpreferences;
    public static final String SpeakBoolean = "speakBoolean";
    public static boolean speak = false;
    Handler handler = new Handler();

    /* Yelp App */
    public static String[] primaryMenu = {"Favourites", "Restaurants", "Coffee Shops", "Bars"};
    public static String[] secondaryMenu = {"Delivery", "Reservations", "More"};
    public static String[] favourites = {"Bucca Di Beppo", "olive Garden", "Osterio Pronto", "Qdoba", "Wasabi"};
    public static String[] favourites_details = {"Fresh Authentic Italian Food served family style.", "American " +
            "casual dining restaurant chain specializing in Italian American cuisine. ",
            "Italian cuisine plus full bar and breakfast", "fast casual restaurant serving mexican style cuisine", "This is a Sushi place"};
    public static String[] restaurants = {"Blaze Pizza", "Sushi Boss", "Delhi Palace", "Yats", "Which Wich"};
    public static String[] restaurants_details = {"Hip counter-serve pizzeria dishing up crispy, thin-crust pies made with creative toppings & sauces.",
            "Sushi Boss is an innovative sushi concept that lets you create your own sushi rolls in any combination you desire.",
            "Low-key Indian eatery whipping up traditional fare, including an array of vegetarian entrees.",
            "Cajun Creole Restaurant", "Modern, casual chain known for design-your-own sandwiches & varied bread options."};
    public static String[] coffee = {"Quills Coffee", "Mo Joe Coffeehouse", "Starbucks", "Caribou"};
    public static String[] coffee_details = {"The latte art at this place is real", "Low-key coffee shop draws a student crowd for light lunches &" +
            " snacks with diverse seating spaces.", "Starbucks uses the highest quality arabica coffee as the base for its beloved drinks.",
            "Eco-conscious coffee chain offering java drinks, smoothies & teas alongside light fare & pastries."};
    public static String[] bars = {"Stacked Pickle", "Charlie & barney's", "Downtown Olly's", "Bourbon Street"};
    public static String[] bars_details = {"Sports bar with TVs, offering basic eats such as sandwiches, wings & burgers, plus beer on tap.",
            "Charlie and Barney's Restaurant is known for its signature chili.", "Known as the sports bar with a twist",
            "Laid-back joint with a New Orleans feel doling out Cajun-style eats & daily drink specials."};
    public static String[] delivery = {"Bucca Di Beppo", "Bento Asian Bistro", "Sushi Boss", "Topper's Pizza"};
    public static String[] reservations = {"Punch burger", "Delhi Palace", "Plow And Anchor", "Al Basha"};
    public static String[] restOptions = {"Navigate", "Call", "Bookmark"};
    public static String[] dummy = {"a", "b"};
    public static String[][] optionLists = {dummy, favourites,  restaurants, coffee, bars, delivery, reservations, favourites};
    public static String[][] menus = { dummy, primaryMenu, secondaryMenu};
    public static int directionIndicator = 0;
    public static int menuCursorPosition = 0;
    public static String currentMenuName = "todo";
    public static String currentRestOption = "todo";
    public static String currentRestaurant = "todo";
    public static int navLevel = 1;
    public static String currentContact = "noOne";
    public static int secondNavSelection = 0;
    public static int secondNavSelectionForFistRotate = 0;
    public static int thirdNavSelection = 0;
    public static String helpMenuName = "noName";
    public static String helpRestName = "noName";
    public static String helpRestOption = "noName";
    public static Boolean freeFlow = Boolean.TRUE;
    public static Boolean callConfirmation = Boolean.FALSE;
    public static Boolean lockConfirmation = Boolean.FALSE;
    public Intent freeFlowIntent;
    public EarconManager earconManager;
    public static float roll;
    public static float startRoll;
    public static float endRoll;
    public static float rollDiff;
    public static Boolean fistBoolean = false;
    public static Boolean fingerSpreadBoolean = false;
    public static Boolean rollStartBoolean = false;

    //public static AsyncTask freeFlowTask = new freeFlowTask();
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
            mGlass.registerListener(YelpActivity.this);
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

        freeFlowIntent = new Intent(this, YelpFreeFlowService.class);

        registerReceiver(mStopReceiver, new IntentFilter(MyoRemoteService.ACTION_STOP_MYO_GLASS));

        mResultReceiver = new AddressResultReceiver(new Handler());
        //buildGoogleApiClient();
        //mGoogleApiClient.connect();

        //Text to speech initialization
        tts = new TextToSpeech(this, this);
        earconManager = new EarconManager();
        earconManager.setupEarcons(tts, getApplicationContext().getPackageName());

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
        tts.shutdown();
        Hub.getInstance().removeListener(mListener);
        unregisterReceiver(mStopReceiver);
        unbindService(mServiceConnection);
        mGlass.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGlass.stopScreenshot();
//        if (mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.disconnect();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
//        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
//            startLocationUpdates();
//        }

        /* Resetting variables for phone application */
        directionIndicator = 0;
        menuCursorPosition = 0;
        navLevel = 1;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                help();
            }
        }, 1000);
        //speakOut("Wave right for favourites and emergency contacts. Wave left for missed, dialed and received");
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
        //stopLocationUpdates();
        speakOut("Location Updates Stopped");
    }

    public void openDialerApplication(View view) {
        Intent intent = new Intent(this, ConfigActivity.class);
        startActivity(intent);
        finish();
    }

    public void openTrainingApplication(View view) {
        Intent intent = new Intent(this, TrainingActivity.class);
        startActivity(intent);
        finish();
    }

    public void openYelpApplication(View view) {
//        Intent intent = new Intent(this, YelpActivity.class);
//        startActivity(intent);
//        finish();
        showToast("Already in Yelp");
    }

    public void openAppDrawer(View view) {
        Intent intent = new Intent(this, DrawerActivity.class);
        startActivity(intent);
        finish();
    }

    public void onGetLocation(View view) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mCurrentLocation != null) {
            mLatitudeText = (String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText = (String.valueOf(mLastLocation.getLongitude()));

        }
        //startLocationUpdates();
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
        //mGoogleApiClient.connect();
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
        Intent intent = new Intent(YelpActivity.this, FetchAddressIntentService.class);

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
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            if(fingerSpreadBoolean) {
                if(rollStartBoolean) {
                    startRoll = roll;
                    rollStartBoolean = false;
                }
                endRoll = roll;
                rollDiff = (startRoll - endRoll);
               if (rollDiff <= -10.00){
                    Log.i("Roll Diff Finger Spread", String.valueOf(rollDiff));
                    Log.i("Direction", "Anti-clockwise");
                    Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.STANDARD);
                    tts.speak("locking", TextToSpeech.QUEUE_FLUSH, null);
                    tts.playEarcon(earconManager.lockEarcon, TextToSpeech.QUEUE_ADD, null);
                    myo.lock();
                    fingerSpreadBoolean = false;
                }
            }

            if(fistBoolean){
                if(rollStartBoolean) {
                    startRoll = roll;
                    rollStartBoolean = Boolean.FALSE;
                }
                endRoll = roll;
                rollDiff = (startRoll - endRoll);
                menuCursorPosition = secondNavSelectionForFistRotate;
                if(rollDiff >= 10.00) {
                    Log.i("Roll Difference", String.valueOf(rollDiff));
                    Log.i("Direction", "Clockwise");
                    fistBoolean = false;
                    navLevel = 1;

                    if(navLevel == 1) {
                        if(directionIndicator == 0){
                            directionIndicator = 1;
                        }
                        if(directionIndicator == 1) {
                            menuCursorPosition = secondNavSelectionForFistRotate;
                            incrementMenuCursorPosition(menus[directionIndicator]);
                            currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                            secondNavSelectionForFistRotate = menuCursorPosition;
                            autoSelectItem();
                        } else {
                            menuCursorPosition = secondNavSelectionForFistRotate;
                            decrementMenuCursorPosition(menus[directionIndicator]);
                            currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                            secondNavSelectionForFistRotate = menuCursorPosition;
                            autoSelectItem();
                        }

                    }

                } else if (rollDiff <= -10.00) {
                    Log.i("Roll Difference", String.valueOf(rollDiff));
                    Log.i("Direction", "Anti-clockwise");
                    fistBoolean = false;
                    navLevel = 1;
                    if(navLevel == 1) {
                        if(directionIndicator == 0) {
                            directionIndicator = 2;
                        }
                        if(directionIndicator == 1){
                            menuCursorPosition = secondNavSelectionForFistRotate;
                            decrementMenuCursorPosition(menus[directionIndicator]);
                            currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                            secondNavSelectionForFistRotate = menuCursorPosition;
                            autoSelectItem();
                        } else if (directionIndicator == 2) {
                            menuCursorPosition = secondNavSelectionForFistRotate;
                            Log.i("Cursor position", String.valueOf(menuCursorPosition));
                            incrementMenuCursorPosition(menus[directionIndicator]);
                            Log.i("Cursor position", String.valueOf(menuCursorPosition));
                            currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                            secondNavSelectionForFistRotate = menuCursorPosition;
                            autoSelectItem();
                        }
                    }
                }
            }
        }


        @Override
        public void onPose(Myo myo, long timestamp, final Pose pose) {
            mPoseView.setText(pose.name());
            fingerSpreadBoolean = Boolean.FALSE;
            fistBoolean = Boolean.FALSE;
            if (pose == pose.FIST) {
                //startLocationUpdates();
                //speakOut("Wave right for favourites");
                YelpActivity.tts.playEarcon(earconManager.helpEarcon, TextToSpeech.QUEUE_FLUSH, null);
                //help();
                fistBoolean = Boolean.TRUE;
                rollStartBoolean = Boolean.TRUE;
            } else if (pose == pose.FINGERS_SPREAD) {
                YelpActivity.tts.playEarcon(earconManager.unlockEarcon, TextToSpeech.QUEUE_FLUSH, null);
                fingerSpreadBoolean = Boolean.TRUE;
                rollStartBoolean = Boolean.TRUE;
                stopFreeFlow();
                menuCursorPosition = 0;
                directionIndicator = 0;
                YelpActivity.tts.playEarcon(earconManager.unlockEarcon, TextToSpeech.QUEUE_FLUSH, null);
                if (navLevel > 1) {
                    navLevel--;
                    help();
                    lockConfirmation = Boolean.FALSE;
                } else if(navLevel == 1){
                    if(lockConfirmation == Boolean.TRUE) {
                        YelpActivity.tts.playEarcon(earconManager.unlockEarcon, TextToSpeech.QUEUE_FLUSH, null);
                        Intent intent = new Intent(getApplicationContext(), DrawerActivity.class);
                        startActivity(intent);
                        Activity activity = YelpActivity.this;
                        activity.finish();
                    } else {
                        addSpeechtoQueue("You are back to the start. Repeat the same action to go to app drawer.");
                        lockConfirmation = Boolean.TRUE;
                    }
                }
                callConfirmation = Boolean.FALSE;
                //stopLocationUpdates();
                //speakOut("Location Updates Stopped");
            } else if (pose == pose.WAVE_OUT) {
                Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
                YelpActivity.tts.playEarcon(EarconManager.swooshEarcon, TextToSpeech.QUEUE_FLUSH, null);

                if(navLevel == 1) {
                    if(directionIndicator == 0){
                        directionIndicator = 1;
                    }
                    if(directionIndicator == 1) {
                        incrementMenuCursorPosition(menus[directionIndicator]);
                        currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                        addSpeechtoQueue(currentMenuName);
                    } else {
                        decrementMenuCursorPosition(menus[directionIndicator]);
                        currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                        addSpeechtoQueue(currentMenuName);
                    }

                } else if(navLevel == 2){
                    stopFreeFlow();
                    incrementMenuCursorPosition(optionLists[secondNavSelection]);
                    currentContact = optionLists[secondNavSelection][menuCursorPosition-1];
                    //helpMenuName = currentContact;
                    addSpeechtoQueue(currentContact);
                } else if(navLevel == 3){
                    stopFreeFlow();
                    incrementMenuCursorPosition(restOptions);
                    currentRestOption = restOptions[menuCursorPosition-1];
                    helpRestOption = currentRestOption;
                    addSpeechtoQueue(currentRestOption);
                }

            } else if(pose == pose.WAVE_IN) {
                Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
                YelpActivity.tts.playEarcon(earconManager.swooshEarcon, TextToSpeech.QUEUE_FLUSH, null);
                if(navLevel == 1) {
                    if(directionIndicator == 0) {
                        directionIndicator = 2;
                    }
                    if(directionIndicator == 1){
                        decrementMenuCursorPosition(menus[directionIndicator]);
                        currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                        addSpeechtoQueue(currentMenuName);
                    } else if (directionIndicator == 2) {
                        incrementMenuCursorPosition(menus[directionIndicator]);
                        currentMenuName = menus[directionIndicator][menuCursorPosition-1];
                        addSpeechtoQueue(currentMenuName);
                    }
                } else if(navLevel == 2) {
                    stopFreeFlow();
                    decrementMenuCursorPosition(optionLists[secondNavSelection]);
                    currentContact = optionLists[secondNavSelection][menuCursorPosition-1];
                    //helpMenuName = currentContact;
                    addSpeechtoQueue(currentContact);
                } else if(navLevel == 3) {
                    stopFreeFlow();
                    decrementMenuCursorPosition(restOptions);
                    currentRestOption = restOptions[menuCursorPosition-1];
                    helpRestOption = currentRestOption;
                    addSpeechtoQueue(helpRestOption);
                }

            } else if(pose == pose.DOUBLE_TAP) {
                YelpActivity.tts.playEarcon(earconManager.selectEarcon, TextToSpeech.QUEUE_FLUSH, null);
                if(navLevel == 1) {
                    final String s = currentMenuName.toLowerCase();
                    if (s.equals("favourites")) {
                        secondNavSelection = 1;
                        secondNavSelectionForFistRotate = 1;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("restaurants")) {
                        secondNavSelection = 2;
                        secondNavSelectionForFistRotate = 2;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("coffee shops")) {
                        secondNavSelection = 3;
                        secondNavSelectionForFistRotate = 3;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("bars")) {
                        secondNavSelection = 4;
                        secondNavSelectionForFistRotate = 4;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("delivery")) {
                        secondNavSelection = 5;
                        // This is the index for the left direction.
                        // When the user chooses to wave left at navlevel 1, we select another array of choices.
                        // For this array, we need to start from 1 again.
                        // Make sure this is correct in other applications too.
                        secondNavSelectionForFistRotate = 1;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("reservations")) {
                        secondNavSelection = 6;
                        secondNavSelectionForFistRotate = 2;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else if (s.equals("more")) {
                        secondNavSelection = 7;
                        secondNavSelectionForFistRotate = 3;
                        addSpeechtoQueue(s);
                        callFunctionWithDelay(1000, s);
                    } else {
                        callFunctionWithDelay(1000, s);
                    }
                } else if(navLevel == 2) {
                    stopFreeFlow();
                    addSpeechtoQueue(currentContact);
                    final String s = currentContact;
                    if(callConfirmation) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                thirdNavLevelAction(s);
                            }
                        }, 1500);
                    } else {
                        callConfirmation = Boolean.TRUE;
                    }
                } else if(navLevel == 3) {
                    currentRestOption = restOptions[menuCursorPosition-1];
                    final String s = currentRestOption.toLowerCase();
                    if (s.equals("navigate")) {
                        secondNavSelection = 1;
                        addSpeechtoQueue(s);
                        helpRestOption = s;
                        if(callConfirmation) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    addSpeechtoQueue("In 100 feet take a right");
                                    //Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.STANDARD);
                                }
                            }, 1000);
                        } else {
                            callConfirmation = Boolean.TRUE;
                        }

                    } else if (s.equals("call")) {
                        secondNavSelection = 2;
                        addSpeechtoQueue(s);
                        helpRestOption = s;
                        if(callConfirmation) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //callContact();
                                    addSpeechtoQueue("calling restaurant");
                                    //Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.STANDARD);
                                }
                            }, 1000);
                        } else {
                            callConfirmation = Boolean.TRUE;
                        }

                    } else if (s.equals("bookmark")) {
                        secondNavSelection = 3;
                        addSpeechtoQueue(s);
                        helpRestOption = s;
                        if(callConfirmation) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    addSpeechtoQueue("Added to favourites");
                                    //Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.STANDARD);
                                }
                            }, 1000);
                        } else {
                            callConfirmation = Boolean.TRUE;
                        }

                    }
                }
            }
        }

        public void autoSelectItem() {
            if(navLevel == 1) {
                final String s = currentMenuName.toLowerCase();
                if (s.equals("favourites")) {
                    secondNavSelection = 1;
                    secondNavSelectionForFistRotate = 1;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("restaurants")) {
                    secondNavSelection = 2;
                    secondNavSelectionForFistRotate = 2;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("coffee shops")) {
                    secondNavSelection = 3;
                    secondNavSelectionForFistRotate = 3;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("bars")) {
                    secondNavSelection = 4;
                    secondNavSelectionForFistRotate = 4;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("delivery")) {
                    secondNavSelection = 5;
                    secondNavSelectionForFistRotate = 1;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("reservations")) {
                    secondNavSelection = 6;
                    secondNavSelectionForFistRotate = 2;
                    callFunctionWithDelay(1000, s);
                } else if (s.equals("more")) {
                    secondNavSelection = 7;
                    secondNavSelectionForFistRotate = 3;
                    callFunctionWithDelay(1000, s);
                } else {
                    callFunctionWithDelay(1000, s);
                }
                addSpeechtoQueue(s);
            }
            YelpActivity.tts.playEarcon(earconManager.selectEarcon, TextToSpeech.QUEUE_ADD, null);
        }


        public void callFunctionWithDelay(int delay, final String s) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    secondNavLevelAction(s);
                }
            }, delay);
        }

        @Override
        public void onLock(Myo myo, long timestamp) {
            //myo.vibrate(Myo.VibrationType.SHORT);
            mPoseView.setText("LOCKED");
        }

        @Override
        public void onUnlock(Myo myo, long timestamp) {
            mPoseView.setText("UNLOCKED");
        }
    }

    public void stopSpeech(View view){
        tts.stop();
    }

    public void callHelp(View view){
        help();
    }

    public void unlockMyo(View view){
        Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
    }

    public void lockMyo(View view){
        Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.STANDARD);
    }

    public void help(){
        if(navLevel == 1){
            addSpeechtoQueue("Wave right for Favourites, Restaurants, Coffee shops and Bars. Wave left for Delivery Reservations and More.");
        } else if(navLevel == 2) {
            addSpeechtoQueue("Wave right to browse " + helpMenuName);
        } else if(navLevel == 3) {
            addSpeechtoQueue("Wave Right to browse options for" + helpRestName);
        }
    }


    public static void incrementMenuCursorPosition(String[] array) {
        if(freeFlow == Boolean.FALSE) {
            callConfirmation = Boolean.TRUE;
        } else {
            callConfirmation = Boolean.FALSE;
        }

        if (menuCursorPosition < (array.length)) {
            menuCursorPosition++;
        } else {
            menuCursorPosition = 1;
        }
    }

    public static void decrementMenuCursorPosition(String[] array) {
        if(freeFlow == Boolean.FALSE) {
            callConfirmation = Boolean.TRUE;
        } else {
            callConfirmation = Boolean.FALSE;
        }

        if (menuCursorPosition > 1) {
            menuCursorPosition--;
        } else {
            menuCursorPosition = array.length;
        }
    }

    public void secondNavLevelAction(String s){
        navLevel = 2;
        menuCursorPosition = 0;
        helpMenuName = s;
        tts.stop();
        startFreeFlow(optionLists[secondNavSelection]);
    }

    public void thirdNavLevelAction(String s){
        navLevel = 3;
        if(secondNavSelection == 1) {
            tts.speak(favourites_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else if(secondNavSelection == 2) {
            tts.speak(restaurants_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else if(secondNavSelection == 3) {
            tts.speak(coffee_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else if(secondNavSelection == 4) {
            tts.speak(bars_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else if(secondNavSelection == 5) {
            tts.speak(favourites_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else if(secondNavSelection == 6) {
            tts.speak(favourites_details[menuCursorPosition-1], TextToSpeech.QUEUE_ADD, null);
        } else {

        }

        tts.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        menuCursorPosition = 0;
        helpRestName = s;
        //tts.stop();
        startFreeFlow(restOptions);
    }

    public void startFreeFlow(String[] array) {
        freeFlow = Boolean.TRUE;
        startService(freeFlowIntent);
    }

    public void stopFreeFlow() {
        freeFlow = Boolean.FALSE;
        //tts.stop();
        stopService(freeFlowIntent);
    }

    public void callContact(){
        tts.stop();
        Uri number = Uri.parse("tel:3176409616");
        Intent callIntent = new Intent(Intent.ACTION_CALL, number);
        startActivity(callIntent);
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

    public static void speakOut(String text) {
        //String text = txtText.getText().toString();
        if (!text.equals("STOP") && speak) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public static void addSpeechtoQueue(String text) {
        //String text = txtText.getText().toString();
        if (!text.equals("STOP") && speak) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
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

