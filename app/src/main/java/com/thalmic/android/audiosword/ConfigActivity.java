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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
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
import java.util.Set;

public class ConfigActivity extends Activity implements GlassDevice.GlassConnectionListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "ConfigActivity";

    private static final int REQUEST_ENABLE_BT = 1;

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

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mLatitudeText;
    private String mLongitudeText;
    private Pose pose;

    protected boolean mAddressRequested;

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
            MyoRemoteService.MBinder mbinder = ((MyoRemoteService.MBinder)service);
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
        mScreencastButton = (Button) findViewById(R.id.btnStartScreencast);
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
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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
        if (mScreencastEnabled){
            mGlass.requestScreenshot();
        } else{
            mGlass.stopScreenshot();
        }
    }

    public void onGetLocation(View view) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText = (String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText = (String.valueOf(mLastLocation.getLongitude()));
            startIntentService();
        }
    }

    private void setScreencastEnabled(boolean enable) {
        mScreencastEnabled = enable;
        updateScreencastState();
    }

    private void updateScreencastState(){
        mScreencastButton.setText(mScreencastEnabled ? R.string.stop : R.string.start);
        mScreencastView.setVisibility(mScreencastEnabled ? View.VISIBLE : View.INVISIBLE);
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
        if (mLastLocation != null) {
            mLatitudeText = (String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText = (String.valueOf(mLastLocation.getLongitude()));
            startIntentService();
        }
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
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

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
            if(pose == pose.FIST) {
                startIntentService();
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

    @Override
    public void onConnectionStatusChanged(GlassDevice.ConnectionStatus status) {
        updateGlassStatus(status);
        if (status != GlassDevice.ConnectionStatus.DISCONNECTED) {
            setScreencastEnabled(false);
        }
    }

    // Called when a message from Glass is received
    public void onReceivedEnvelope(Proto.Envelope envelope){
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
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            String lines[] = mAddressOutput.split("\\r?\\n");
            showToast(lines[1]);

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

}
