package edu.cmu.pocketsphinx.demo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothHeadset;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Set;


public class Connector extends Service {
    private static IBluetoothHeadset ibht;
    public static Context application;
    private static String LOG_TAG = "HSP_Connector";
    private static BluetoothDevice device = null;
    private String TOY_NAME = "Pet Singer";
    private static final String TOY_ADDRESS = "00:58:56:7B:D7:17";


    public void onCreate() {
         super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "OnDestroy called");
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
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        for (BluetoothDevice dev : pairedDevices) {
            if (dev.getAddress().equalsIgnoreCase(TOY_ADDRESS))
                device=dev;
        }
        if (ibht!=null){
            setHSPConnection();
            return START_NOT_STICKY;
        }
        getIBluetoothHeadset(getApplication());
        return START_NOT_STICKY;
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
                    setHSPConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ibht=null;
        }

    };

    private static void setHSPConnection(){
            try {
                ibht.connect(device);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }

}
