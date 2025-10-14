package com.anmi.camera.uvcplay.utils;



import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.base.log.Dlog;


public class AppUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{
    private static final String TAG = "[AppUncaughtExceptionHandler]";

    private static AppUncaughtExceptionHandler instance;
    private AppUncaughtExceptionHandler() {}
    public static AppUncaughtExceptionHandler getInstance(){
        if (instance == null) {
            instance = new AppUncaughtExceptionHandler();
        }
        return instance;
    }

    public void init(){
        Thread.setDefaultUncaughtExceptionHandler(this);
        new Handler(Looper.getMainLooper()).post(() -> {
            for (;;) {
                try {
                    Dlog.e("#looper in:" + Thread.currentThread().getId());
                    Looper.loop();
                } catch (Throwable e) {
                    Dlog.e("#looper exception:" + e);
                    // 捕获住主线程异常并处理
                    uncaughtException(Looper.getMainLooper().getThread(), e);
                }
            }
        });
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            Dlog.e(TAG + "uncaughtException Throwable:"+ e, e);
            StringBuilder exceptionContent = new StringBuilder() ;
            exceptionContent.append(e);
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length>0){
                for (StackTraceElement stackTraceElement : stackTrace) {
                    exceptionContent
                            .append("\n\t")
                            .append(stackTraceElement);
                }
            }
            Dlog.e(TAG + "uncaughtException full exceptionContent:"+ exceptionContent);
        } catch (Throwable exception) {
            Dlog.e(TAG + "uncaughtException handle exception:"+ e);
        }
    }
}
