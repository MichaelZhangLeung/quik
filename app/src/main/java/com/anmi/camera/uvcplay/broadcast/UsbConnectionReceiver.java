package com.anmi.camera.uvcplay.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import dev.alejandrorosas.apptemplate.R;
import dev.alejandrorosas.streamlib.UsbDeviceManager;
import com.anmi.camera.uvcplay.utils.Config;
import dev.alejandrorosas.streamlib.UsbUtils;
import com.anmi.camera.uvcplay.utils.Utils;
import com.base.MyLog;

public class UsbConnectionReceiver extends BroadcastReceiver {
    private static final String TAG = "[UsbConnectionReceiver]";
    private static final int WHAT_USB_ATTACHED = 1;
    private static final int WHAT_USB_DETACHED = 2;
    private static final int WHAT_USB_PERMISSION = 3;
    private static Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case WHAT_USB_ATTACHED:
                    UsbDeviceManager.getInstance().initDevice(Utils.INSTANCE.getContext());
                    break;
                case WHAT_USB_DETACHED:
                    UsbDeviceManager.getInstance().clearPorts();
                    Utils.INSTANCE.toast(Utils.INSTANCE.getContext().getString(R.string.toast_device_connect_error));

                    break;
                case WHAT_USB_PERMISSION:
                    if (!mIntent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        UsbDeviceManager.getInstance().openConnect();
//                        PhoneControlManager.getInstance().notifyDeviceStatus(PhoneControlManager.DEVICE_STATUS_FREE);
                    }
                    break;
            }
        }
    };
    private static Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.e(TAG+"action:" + intent.getAction());
        String action = intent.getAction();
        int what=0;
        switch (action) {
            case Config.USB_ACTION_ATTACHED:
                what = WHAT_USB_ATTACHED;
                break;
            case Config.USB_ACTION_DETACHED:
                what = WHAT_USB_DETACHED;
                break;
            case Config.USB_ACTION_PERMISSION:
                mIntent = intent;
                what = WHAT_USB_PERMISSION;
                break;
        }
        boolean usbMicrophone = false;
        try {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            MyLog.e(TAG+"EXTRA_DEVICE device:" + Utils.INSTANCE.getDeviceName(device));
            usbMicrophone = UsbUtils.isUsbMicrophone(device);
        } catch (Throwable e) {
            MyLog.e(TAG+"EXTRA_DEVICE exception:" + e, e);
        }

        MyLog.e(TAG+"EXTRA_DEVICE final usbMicrophone:" + usbMicrophone);

        if (usbMicrophone){
            mHandler.removeMessages(what);
            mHandler.sendEmptyMessageDelayed(what,2000);
        }

    }

}
