package com.base.log;


import static com.base.log.LogControl.ShowLog;
import static com.base.log.LogControl.printLog;

import android.util.Log;


public class Dlog {

    private static final String TAG = "[ur_play]" ;
    
    private static String getTag(){
       return TAG;
    }

    public static void debug(String msg){
        Log.d(TAG, getTag()+msg);
    }

    private static String wrapVersion(String msg){
        return msg ;
    }

    public static void d(String msg){
        msg = wrapVersion(msg);
        if (ShowLog){
            Log.d(TAG, getTag() + msg);
        }
        if (printLog){
            // 之前的日志无法写入
            msg = encryptMsg('d', msg) ;
            LogControl.getInstance().putCommandLogIntoQueue(getTag() + msg);
//            LogUtils.d(getTag() + msg);
        }
    }

    public static void e(String msg){
        msg = wrapVersion(msg);
        if (ShowLog){
            Log.e(TAG, getTag() + msg);
        }

        if (printLog){
            msg = encryptMsg('e', msg) ;
            LogControl.getInstance().putCommandLogIntoQueue(getTag() + msg);
//            LogUtils.e(getTag() + msg);
        }

    }

    public static void e(String msg, Throwable t){
        msg = wrapVersion(msg);
        if (ShowLog){
            Log.e(TAG, getTag() + msg, t);
        }

        if (printLog){
            try{
                if (t != null){
                    StringBuilder sb = new StringBuilder(" stacks:") ;
                    StackTraceElement[] stackTrace = t.getStackTrace();
                    int lines = Math.min(20, stackTrace.length) ;
                    for (int i = 0; i < lines; i++) {
                        sb.append(stackTrace[i]).append("\n\t");
                    }
                    msg = msg + sb.toString() ;
                }
                msg = encryptMsg('e', msg) ;
                LogControl.getInstance().putCommandLogIntoQueue("E/" + getTag() + msg);
            } catch (Throwable e){
                Log.e(TAG,  getTag() + "to log control exception:" + e) ;
            }
//            LogUtils.e(getTag() + msg, t);
        }
    }

    private static String encryptMsg(char level,String msg){
//        if (isSass()){
//            return msg;
//        }
//        return 'e'==level?"-e:-"+ AESUtils.encrypt(msg) :msg;
        return msg;
    }

    public static void divide(String msg){
        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize ) {// 长度小于等于限制直接打印
            d(msg);
        }else {
            while (msg.length() > segmentSize ) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize );
                msg = msg.replace(logContent, "");
                d(logContent);
            }
            d(msg);// 打印剩余日志
        }

    }
}

