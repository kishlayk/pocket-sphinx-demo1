package edu.cmu.pocketsphinx.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import static android.media.AudioManager.SCO_AUDIO_STATE_CONNECTED;
import static android.media.AudioManager.SCO_AUDIO_STATE_CONNECTING;
import static android.media.AudioManager.SCO_AUDIO_STATE_DISCONNECTED;
import static android.media.AudioManager.SCO_AUDIO_STATE_ERROR;

public class BluetoothReceiver extends BroadcastReceiver {
    private static String TAG="BluetoothReceiver";
    private long HEADSET_ENABLE_TIMEOUT = 0;
    private Context activitycontext;
    private BluetoothListener mListener;
    private AudioManager am;

    public interface BluetoothListener{
        void onBluetoothConnect();
        void onBluetoothDisconnect();
    }

    public BluetoothReceiver(Context context, BluetoothListener listener){
        if (context==null){
            Log.e(TAG, "Context passed to BluetoothReceiver is null");
            return;
        }
        activitycontext = context;
        mListener = listener;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        activitycontext.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()==BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
            Log.d(TAG, "Bluetooth connection state " + state);
            if(state==BluetoothAdapter.STATE_CONNECTED)
                checkHeadset(context);
        }

        if (intent.getAction()==AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED){
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            switch (state){
                case SCO_AUDIO_STATE_CONNECTED:
                    mListener.onBluetoothConnect();
                    break;
                case SCO_AUDIO_STATE_DISCONNECTED:
                    mListener.onBluetoothDisconnect();
                    break;
                case SCO_AUDIO_STATE_CONNECTING:
                    break;
                case SCO_AUDIO_STATE_ERROR:
                    break;
            }
        }

        if (intent.getAction()==BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT){
            Log.i(TAG, "Event Received from Pet Signer.");
        }
    }

    @SuppressWarnings("deprecation")
    private void checkHeadset(final Context context) {
        if (context==null){
            Log.e(TAG, "Context passed to BluetoothReceiver is null");
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() {
                    am.setBluetoothScoOn(true);
                    am.startBluetoothSco();
//                    am.setMicrophoneMute(true);
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
            }, HEADSET_ENABLE_TIMEOUT);
        } else if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_DISCONNECTED){
                am.stopBluetoothSco();
        }
    }

    public void closeReceiver(){
        if (activitycontext==null){
            Log.e(TAG, "Context is null when closing Bluetooth Receiver");
            return;
        }
        activitycontext.unregisterReceiver(this);

        if (am!=null){
            am.stopBluetoothSco();
        }
    }
}
