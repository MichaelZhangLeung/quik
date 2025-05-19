package com.base;

import android.util.Log;

import com.base.log.Dlog;

public class MyLog {

    public static void d(String msg) {
        Dlog.d(msg);
    }

    public static void e(String msg) {
        Dlog.e( msg) ;
    }

    public static void e(String msg, Throwable e) {
        Dlog.e(msg, e);
    }
}
