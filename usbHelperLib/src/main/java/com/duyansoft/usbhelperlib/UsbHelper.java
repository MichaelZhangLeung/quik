package com.duyansoft.usbhelperlib;

import android.text.TextUtils;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class UsbHelper {
    private static final String TAG = "UsbHelper";
    static {
        System.loadLibrary("usbhelperlib");
    }

    private final UsbSerialPort usbSerialPort;

    public UsbHelper(UsbSerialPort usbSerialPort) {
        this.usbSerialPort = usbSerialPort;
    }


    /**
     * 设备认证
     */
    public void authDevice(CallBack callBack) {
        try {
            getDeviceInfo();
            byte[] buffer = new byte[64];

            byte[] batchNum = new byte[4];
            int batchLen = usbSerialPort.read(buffer, 0);
            Log.e("UsbHelper","batchLen:"+batchLen);
            if (batchLen!=4){
                callBack.result(null,null);
                return;
            }
            System.arraycopy(buffer, 0, batchNum, 0, batchLen);

            auth();
            byte[] deviceId = new byte[12];
            int deviceLen = usbSerialPort.read(buffer,0);
            Log.e("UsbHelper","deviceLen:"+deviceLen);

            if (deviceLen!=12){
                callBack.result(null,null);
                return;
            }
            System.arraycopy(buffer, 0, deviceId, 0, deviceLen);
            Log.e(TAG,"auth callBack:"+ Arrays.toString(deviceId));

            byte[] vcode = combineAndSum(deviceId);
            authVcode(vcode);
//            open();
            callBack.result(batchNum,deviceId);

        } catch (Exception e) {
            callBack.result(null,null);
        }
    }

    public void auth(String deviceId){
        try{
            byte[] devicesIdBytes = convertStringToBytes(deviceId);
            byte[] vCode = combineAndSum(devicesIdBytes);
            authVcode(vCode);
        }catch (Exception ignore){}
    }

    public static byte[] combineAndSum(byte[] data) {
        // 确保输入数据长度是 12
        if (data.length != 12) {
            throw new IllegalArgumentException("输入数据长度必须是 12");
        }

        // 将每两个字节组合成一个 16 位无符号整数
        int[] numbers = new int[6];
        for (int i = 0; i < 6; i++) {
            int highByte = data[11 - 2 * i] & 0xFF; // 高 8 位
            int lowByte = data[10 - 2 * i] & 0xFF;   // 低 8 位
            numbers[i] = (highByte << 8) | lowByte;  // 组合成 16 位数
        }

        // 对 6 个 16 位数进行无符号加法运算
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }

        // 处理溢出，只保留低 16 位
        sum &= 0xFFFF;

        // 将结果拆分为 byte[2]
        byte[] result = new byte[2];
        result[0] = (byte) ((sum >> 8) & 0xFF); // 高 8 位
        result[1] = (byte) (sum & 0xFF);       // 低 8 位
        return result;
    }

    public byte[] convertStringToBytes(String str) {
        if (TextUtils.isEmpty(str) || str.length() % 2 != 0) {
            return null;
        }

        try{
            byte[] deviceId = new byte[str.length()/2];
            for (int i=0;i<deviceId.length;i++){
                deviceId[i] = (byte) Integer.parseInt(str.substring(i*2,i*2+2), 16);}
            return deviceId;
        }catch (Exception ignored){
        }

        return null;
    }

    public interface CallBack{
        void result(byte[] batchNum,byte[] deviceId);
    }

    /**
     * A native method that is implemented by the 'usbhelperlib' native library,
     * which is packaged with this application.
     */
    public native void auth();

    private native void authVcode(byte[] code);

    public native void open();

    public native void close();

    public native void startRecharge();

    public native void closeRecharge();

    public native void getDeviceInfo();

    // 将 byte[] 传递给 Native 层
    public native void pushData(byte[] data);

    // 启动 Native 层的 10ms 定时消费任务
    public native void startConsumer();
    public native void stopConsumer();

    // Java 方法：提供给 Native 层回调，用于写串口
    public void writeToSerialPort(byte[] data) {
        Log.e("writeToSerialPort","time:"+System.currentTimeMillis());
        // 这里调用你的串口写方法，例如：
        // serialPort.write(data);
        try {
            usbSerialPort.write(data,100);
        } catch (IOException ignore) {
        }
    }
}