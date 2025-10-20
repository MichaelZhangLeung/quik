package dev.alejandrorosas.streamlib;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.widget.Toast;

import com.base.MyLog;


public class UsbUtils {

    private static final String TAG = "[UsbUtils]";
    private static Context sContext;

    public static String getDeviceName(UsbDevice device) {
        return device != null ? device.getDeviceName() : null;
    }

    public static void  toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show() ;
    }

   public static void setContext(Context context) {
       sContext = context;
   }

    public static Context getContext() {
        return sContext;
    }

    public static boolean isUsbMicrophone(UsbDevice device) {
        final int ifaceCount = device.getInterfaceCount();
        MyLog.e(TAG+"isUsbMicrophone ifaceCount:" + ifaceCount);
        for (int i = 0; i < ifaceCount; i++) {
            UsbInterface iface = device.getInterface(i);
//            MyLog.e(TAG+"isUsbMicrophone iface:" + iface);
            int ifClass = iface.getInterfaceClass();
            int ifSubClass = iface.getInterfaceSubclass();
            MyLog.e(TAG+"isUsbMicrophone ifClass:" + ifClass + ", ifSubClass:" + ifSubClass);
            // CDC/CDC_DATA + bulk endpoints 发音频
            if (ifClass == UsbConstants.USB_CLASS_CDC_DATA || ifClass == UsbConstants.USB_CLASS_COMM) {
//                MyLog.e(TAG+"isUsbMicrophone getEndpointCount:" + iface.getEndpointCount());
                for (int e = 0; e < iface.getEndpointCount(); e++) {
                    UsbEndpoint ep = iface.getEndpoint(e);
//                    MyLog.e(TAG+"isUsbMicrophone ep:" + ep);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                            && ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        // 进一步可用 vendorId/productId 白名单确认
                        return true;
                    }
                    // 如果是 isochronous（标准 UAC 会用 isoc）
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_ISOC
                            && ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        return true;
                    }
                }
            }

            // 厂商/产品名称或 VID/PID 过滤
            // int vid = device.getVendorId();
            // int pid = device.getProductId();
            // if (vid == MY_AUDIO_VID && pid == MY_AUDIO_PID) return true;
        }
        return false;
    }
    public static boolean isEarphone(UsbDevice device) {
        final int ifaceCount = device.getInterfaceCount();
        for (int i = 0; i < ifaceCount; i++) {
            UsbInterface iface = device.getInterface(i);
            int ifClass = iface.getInterfaceClass();
            int ifSubClass = iface.getInterfaceSubclass();
            MyLog.e(TAG+"#isEarphone ifClass:" + ifClass + ", ifSubClass:" + ifSubClass);

            if (ifClass == UsbConstants.USB_CLASS_VIDEO) {
              return false;
            }

            if (ifClass == UsbConstants.USB_CLASS_AUDIO) {
              return true;
            }
        }
        return false;
    }

    public static boolean isUsbCamera(UsbDevice device) {
        return false;
    }
}
