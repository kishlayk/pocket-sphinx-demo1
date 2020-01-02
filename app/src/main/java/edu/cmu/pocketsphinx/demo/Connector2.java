package edu.cmu.pocketsphinx.demo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.IBluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Set;


public class Connector2 extends Service {
    public static IBluetoothA2dp ibta2 = null;
    private static IBluetoothHeadset ibht;
    public static final String filter_1_string = "a2dp.connect2.Connector.INTERFACE";
    public static boolean mIsBound = false;

    public static Context application;
    private static String DeviceToConnect;
    boolean isConnected = false;
    private String PREFS = "bluetoothlauncher";
    private static String LOG_TAG = "A2DP_Connector";
    private static BluetoothDevice device = null;
    private String dname;
    private String bt_mac;
    boolean serviceRegistered = false;
    boolean receiverRegistered = false;
    int w_id;
    private String TOY_NAME = "Pet Singer";
    private static final String TOY_ADDRESS = "00:58:56:2E:05:40";


    public void onCreate() {
        // super.onCreate();
        application = getApplication();
        if (!receiverRegistered) {
            String filter_1_string = "a2dp.connect2.Connector.INTERFACE";
            IntentFilter filter1 = new IntentFilter(filter_1_string);
            application.registerReceiver(a2dpReceiver, filter1);
            application.registerReceiver(hspReceiver, new IntentFilter("HEADSET_INTERFACE_CONNECTED"));
            receiverRegistered = true;
        }
        getIBluetoothA2dp(application);
        getIBluetoothHeadset(application);
        serviceRegistered = true;
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        for (BluetoothDevice dev : pairedDevices) {
            if (dev.getAddress().equalsIgnoreCase(TOY_ADDRESS))
                device=dev;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "OnDestroy called");
        done();
        super.onDestroy();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();

        application = getApplicationContext();
        if (extras != null) {
            w_id = extras.getInt("ID", 0);

            bt_mac = extras.getString(TOY_ADDRESS);
            dname = extras.getString(TOY_NAME);
        } else {
            done();
        }

        DeviceToConnect = bt_mac;

        if (bt_mac != null)
            if (bt_mac.length() == 17) {

                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

                if (!bta.isEnabled()) {
                    Intent btIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    application.startActivity(btIntent);
                    Log.i(LOG_TAG, "Bluetooth was not enabled, starting...");
                    return START_REDELIVER_INTENT;
                }

                BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
                if (mBTA == null || !mBTA.isEnabled()) {
                    Log.i(LOG_TAG, "Bluetooth issue");
                    return START_REDELIVER_INTENT;
                }

                Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
                for (BluetoothDevice dev : pairedDevices) {
                    if (dev.getAddress().equalsIgnoreCase(bt_mac))
                        device = dev;
                }
                if (device == null) {
                    Log.i(LOG_TAG, "Device was NULL");

                    return START_REDELIVER_INTENT;
                }
                sendIntent();

            } else {
                Log.i(LOG_TAG, "Invalid device = " + bt_mac);
                done();
            }

        else {
            Log.e(LOG_TAG, "Device to connect was NULL");
            done();
        }
        return START_NOT_STICKY;
        // super.onStart(intent, startId);
    }

    private static void sendIntent() {
        Intent intent = new Intent();
        intent.setAction(filter_1_string);
        application.sendBroadcast(intent);

        Intent intent1 = new Intent();
        intent1.setAction("HEADSET_INTERFACE_CONNECTED");
        application.sendBroadcast(intent1);
    }

    private final BroadcastReceiver a2dpReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            IBluetoothA2dp ibta = ibta2;
            connectBluetoothA2dp(bt_mac);
        }

    };

    private void connectBluetoothA2dp(String device) {
        new ConnectBt().execute(device);
    }


    public static ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mIsBound = true;
            ibta2 = IBluetoothA2dp.Stub.asInterface(service);
            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> pairedDevices = mBTA.getBondedDevices();
            BluetoothDevice device = null;
            for (BluetoothDevice dev : pairedDevices) {
                if (dev.getAddress().equalsIgnoreCase(DeviceToConnect))
                    device = dev;
            }
            if (device != null)
                try {
                    Log.i(LOG_TAG, "Service connecting " + device);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error connecting Bluetooth device " + e.getLocalizedMessage(), e);
                }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            doUnbind();
        }
    };

    static void doUnbind() {
        if (mIsBound) {
            try {
                application.unbindService(mConnection);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getLocalizedMessage(), e);
            }
        }
    }

    public void getIBluetoothHeadset(Context context) {
        Intent i = new Intent(IBluetoothHeadset.class.getName());
        i.setPackage(getPackageManager().resolveService(i, PackageManager.GET_RESOLVED_FILTER).serviceInfo.packageName);
        if (bindService(i, HSPConnection, Context.BIND_AUTO_CREATE)) {
            Log.i("HSP SUCCEEDED", "HSP connection bound");
        } else {
            Log.e("HSP FAILED", "Could not bind to Bluetooth HFP Service");
        }
    }

    //Method for bind
    public static ServiceConnection HSPConnection= new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
                    ibht = IBluetoothHeadset.Stub.asInterface(service);
                    try {
                        ibht.connect(device);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ibht=null;
        }

    };

    private final BroadcastReceiver hspReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
//            Log.d(LOG_TAG,"hsp||shouldConnect:" + shouldConnect + ", ibht:" + ibht);
            try {
//                if (ibht != null && ibht.getConnectionState(device) == 0) {
                if(ibht != null){
                    ibht.connect(device);
                }else{
                    ibht.disconnect(device);
                }
            } catch (RemoteException | SecurityException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage(), e);

            }
        }
    };

    public void getIBluetoothA2dp(Context context) {

        Intent i = new Intent(IBluetoothA2dp.class.getName());

        String filter;
        filter = getPackageManager().resolveService(i, PackageManager.GET_RESOLVED_FILTER).serviceInfo.packageName;
        i.setPackage(filter);

        if (context.bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.i(LOG_TAG, "mConnection service bound " + context.getPackageCodePath());
        } else {
            Log.e(LOG_TAG, "Could not bind to Bluetooth A2DP Service");
        }

    }

    private class ConnectBt extends AsyncTask<String, Void, Boolean> {

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */

        String btd;

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
        }

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        protected void onPreExecute() {
            //Log.i(LOG_TAG, "Running background task with ");
        }

        @Override
        protected Boolean doInBackground(String... arg0) {

            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
            if (mBTA == null || !mBTA.isEnabled())
                return false;

            Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
            BluetoothDevice device = null;
            for (BluetoothDevice dev : pairedDevices) {
                if (dev.getAddress().equalsIgnoreCase(arg0[0])) device = dev;
            }
            if (device == null)
                return false;
            btd = device.getAddress();
            /*
             * mBTA.cancelDiscovery(); mBTA.startDiscovery();
             */
            IBluetoothA2dp ibta = ibta2;
//            Log.d(LOG_TAG,"doInBg()||shouldConnect:" + shouldConnect + ", ibht:" + ibht);
            try {

                if(ibta != null){
                    ibta.connect(device);
                    isConnected = true;
                    Log.v(LOG_TAG, "Connecting...: " + device.getName() + " PREF_CONNECTED_DEVICE_MAC " + device.getAddress() + " PREF_CONNECTED_DEVICE_NAME " + device.getName());
                } else {
                    ibta.disconnect(device);
                    isConnected = false;
                    Log.v(LOG_TAG, "Disconnecting...: " + device.getName());
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
            }
            return true;
        }
    }

    private void done() {
        Log.i(LOG_TAG, "Connector Service stopping");
        if (receiverRegistered) {
            try {
                application.unregisterReceiver(a2dpReceiver);
                application.unregisterReceiver(hspReceiver);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getLocalizedMessage(), e);
            }
        }
        if (serviceRegistered) {
            try {
                //doUnbindService(application);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.stopSelf();
    }
}
