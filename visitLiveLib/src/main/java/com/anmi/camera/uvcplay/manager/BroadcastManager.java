package com.anmi.camera.uvcplay.manager;

import android.content.Context;
import android.content.IntentFilter;

import com.anmi.camera.uvcplay.broadcast.UsbConnectionReceiver;
import com.anmi.camera.uvcplay.utils.Config;

public class BroadcastManager {
    private static BroadcastManager instance;

    private BroadcastManager() {
    }

    public static BroadcastManager getInstance() {
        if (instance == null) {
            instance = new BroadcastManager();
        }
        return instance;
    }

    public void init(Context context){
        registerUsbConnectionReceiver(context);
    }



    public void registerUsbConnectionReceiver(Context context){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Config.USB_ACTION_ATTACHED);
        intentFilter.addAction(Config.USB_ACTION_DETACHED);
        intentFilter.addAction(Config.USB_ACTION_PERMISSION);
        context.registerReceiver(new UsbConnectionReceiver(),intentFilter);
    }
}
