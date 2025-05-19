package com.base;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author admin
 */
public class ThreadHelper {

    private ExecutorService cachedThreadPool;
    private static ThreadHelper sInstance;
    public Handler mainHandler;

    @SuppressLint("NewApi")
    public static final ExecutorService singleThreadExecutor = Executors.newWorkStealingPool();
    @SuppressLint("NewApi")
    public static final ExecutorService mediaSingleThreadExecutor = Executors.newSingleThreadExecutor();
    @SuppressLint("NewApi")
    public static final ExecutorService workThreadExecutor = Executors.newWorkStealingPool();

    private ThreadHelper() {
        init();
    }

    public static ThreadHelper getInstance() {
        if (sInstance == null) {
            sInstance = new ThreadHelper();
        }
        return sInstance;
    }

    public void init() {
        cachedThreadPool = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void post(Runnable r) {
        if (r == null) {
            return;
        }
        cachedThreadPool.execute(r);
    }

    public void postDelayed(final Runnable r, long delay) {
        if (r == null) {
            return;
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cachedThreadPool.execute(r);
            }
        }, delay);
    }

    public void postToWorkerDelayed(final Runnable r, long delay) {
        if (r == null) {
            return;
        }
        mainHandler.postDelayed(() -> workThreadExecutor.execute(r), delay);
    }

    public void postToWorker(final Runnable r) {
        if (r == null) {
            return;
        }
        workThreadExecutor.execute(r) ;
    }

    public void postToUIThread(final Runnable r, long delay) {
        if (r == null) {
            return;
        }
        mainHandler.postDelayed(r, delay);
    }

    public void postToUIThread(final Runnable r) {
        if (r == null) {
            return;
        }

        if (isOnMainThread()){
            r.run();
            return;
        }
        mainHandler.post(r);
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public void postSingle(Runnable task) {
        if (task == null) {
            return;
        }
        singleThreadExecutor.submit(task) ;
    }

    public void postMediaSingle(Runnable task) {
        if (task == null) {
            return;
        }
        mediaSingleThreadExecutor.submit(task) ;
    }
}
