package com.base.log;


import android.annotation.SuppressLint;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.base.ThreadHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LogControl {
    public final static Boolean LOG_TO_FILE = true; // 日志写入文件开关
    public static final boolean LOG_SWITCH = true;

    private static final String TAG = "LogControl";
    private static final boolean DEBUG = false;
    public static final boolean ShowLog = LOG_SWITCH;
    public static final boolean printLog = LOG_TO_FILE;
    private static final int LOG_QUEUE_MAX_CAPACITY = 1024;
    private File logFile;
    private RandomAccessFile randomAccessFile;
    private BlockingQueue<String> logBlockingQueue;
    private String dirPath;
    private OnLogQueueBlockListener onLogQueueBlockListener;
    private String tagId;
    private int tempCount;
    private boolean isInitSuccess;

    /**
     * yyyy-MM-dd 格式
     */
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat DATE_FORMAT_YMD = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * HH-mm-ss 格式
     */
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat TIME_FORMAT_HMS = new SimpleDateFormat("HH-mm-ss");
    /**
     * HH-mm-ss:SSS 格式
     */
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat TIME_FORMAT_HMS_S = new SimpleDateFormat("HH-mm-ss:SSS");

    private LogControl() {
        init(getLogPath()
                , ""
                , 30
                , (OnLogQueueBlockListener) () -> {
                });
    }

    /**
     * 使用内部静态类实现单例模式
     *
     * @return 实例
     */
    public synchronized static LogControl getInstance() {
        return LogFileToolsHolder.instance;
    }

    private static class LogFileToolsHolder {
        private volatile static LogControl instance = new LogControl();
    }

    public void init(String dirPath, String id, int overTimeDays, OnLogQueueBlockListener onLogQueueBlockListener) {
        this.dirPath = dirPath;
        this.tagId = id;
        isInitSuccess = true;
        this.onLogQueueBlockListener = onLogQueueBlockListener;
        // 初始化阻塞队列
        logBlockingQueue = new LinkedBlockingDeque<>(LOG_QUEUE_MAX_CAPACITY);
        // 进行日志文件的删除
        try {
            deleteOldLogFiles(overTimeDays);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        ThreadHelper.getInstance().post(new LogRecordRunnable()) ;
    }

    /**
     * 删除旧的文件
     *
     * @param overTimeDays 时间天数
     */
    public void deleteOldLogFiles(int overTimeDays) {
        if (overTimeDays == -1) {
            return;
        }
        File dir = new File(dirPath);
        if (!dir.exists() || dir.isFile()) {
            return;
        }
        for (File file : dir.listFiles()) {
            String currentFileName = file.getName();
            if (!currentFileName.contains("command-")) {
                continue;
            }

            long lastModified = file.lastModified();
            Date fileDate = new Date(lastModified);
            if (analysisDateDiffer(fileDate, new Date()) > overTimeDays) {
                file.delete();
            }
        }
    }

    /**
     * 将日志数据加入阻塞队列，等待被写入日志
     *
     * @param command 要写入到日志的数据
     */
    public void putCommandLogIntoQueue(String command) {
        if (!isInitSuccess)
            return;
        try {
            command = String.format("%s %s-%s %s", TIME_FORMAT_HMS_S.format(new Date()), android.os.Process.myPid(),  android.os.Process.myTid(), command) ;

            if (logBlockingQueue.remainingCapacity() <= 50) {
                // 说明队列的消费被阻塞，清空队列，并重新打开输出流
                logBlockingQueue.clear();
                resetOutputStream();
                if (onLogQueueBlockListener != null) {
                    onLogQueueBlockListener.onLogQueueBlock();
                }
            }
            int size = logBlockingQueue.size();
            if (size > tempCount) {
                tempCount = size;
            }
            if (DEBUG) Log.i(TAG, "log queue info"
                    + "\ntemp queue count= " + tempCount
                    + "\nlog content:" + command);
            logBlockingQueue.put(command);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 将数据写入到txt日志文件中
     */
    private class LogRecordRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String command = logBlockingQueue.take();
                    if (!TextUtils.isEmpty(command))
                        writeCommand2FileTxt(command);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                // 线程被打断，日志停止输出，清空阻塞队列
                logBlockingQueue.clear();
                //重启写日志任务
                ThreadHelper.getInstance().postToWorkerDelayed(this, 1000L) ;
            }
        }
    }

    /**
     * 创建日志文件目录
     *
     * @param logDirPath  日志存放路径
     * @param logFileName 日志文件名称
     * @return 返回已经创建的日志文件
     */
    private File createLogFile(String logDirPath, String logFileName) {
        File dir = new File(logDirPath);
        if (!dir.exists() || dir.isFile()) {
            dir.mkdirs();
        }
        return new File(dir, logFileName);
    }

    /**
     * 日志记录是很频繁的操作，不应该每次都打开和关闭流，每次写文件时检查该文件是否已经被删除，应该重建并写入重建标志,检查时间，是否需要生成另一个文件
     *
     * @param strContent 写入的内容
     */
    private void writeCommand2FileTxt(String strContent) {
        // 每次写入时，检测写入文件是否满足条件
        checkLogFile();
        // 每次写入时，都进行换行
        String str = strContent + "\r\n";
        FileLock fileLock = null ;
        try {
            if (null != randomAccessFile) {
                FileChannel channel = randomAccessFile.getChannel();
                fileLock = channel.lock() ;
                randomAccessFile.seek(logFile.length());
                randomAccessFile.write(str.getBytes());
            }
        } catch (Throwable e) {
            Log.e("log", "write exception:" + e);
            e.printStackTrace();
        } finally {
            try {
                if (fileLock != null && fileLock.isValid()) {
                    fileLock.release();
                }
            } catch (Throwable e){
                Log.e("log", "lock release exception:" + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查Log文件是否存在，是否过时
     */
    private void checkLogFile() {
        try {
            // 检查文件是否被删除
            if (null == logFile || !logFile.exists()) {
                // 文件已经不存在，创建新的文件
                closeLogFileStream();
                logFile = createLogFile(dirPath, getNowLogFileName());
                randomAccessFile = new RandomAccessFile(logFile, "rwd");
                printTagId();
            }
            // 检查是否应该写入到新的日志文件
            String currentFileName = getNowLogFileName();
            if (!logFile.getName().equals(currentFileName)) {
                closeLogFileStream();
                logFile = createLogFile(dirPath, currentFileName);
                randomAccessFile = new RandomAccessFile(logFile, "rwd");
                printTagId();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void printTagId() throws IOException {
        if (TextUtils.isEmpty(tagId)) {
            tagId = "unknown";
        }
        String str = "account = " + tagId + "\r\n";
        randomAccessFile.write(str.getBytes());
    }

    /**
     * 重置输出流
     */
    private void resetOutputStream() {
        try {
            closeLogFileStream();
            String currentFileName = getNowLogFileName();
            logFile = createLogFile(dirPath, currentFileName);
            randomAccessFile = new RandomAccessFile(logFile, "rwd");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭日志输出流
     */
    private void closeLogFileStream() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
                randomAccessFile = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取现在应该生成的日志文件名
     *
     * @return 日志文件名
     */
    private String getNowLogFileName() {
        return "info-" + DATE_FORMAT_YMD.format(new Date()) + ".log";
    }

    /**
     * 判断两个日期相差几天
     *
     * @param date1 文件最后修改时间
     * @param date2 当前机器时间
     * @return 两个日志相差的天数
     */
    private int analysisDateDiffer(Date date1, Date date2) {
        Calendar d1 = new GregorianCalendar();
        d1.setTime(date1);
        Calendar d2 = new GregorianCalendar();
        d2.setTime(date2);
        int days = d2.get(Calendar.DAY_OF_YEAR) - d1.get(Calendar.DAY_OF_YEAR);
        int y2 = d2.get(Calendar.YEAR);

        if (d1.get(Calendar.YEAR) != y2) {
//            do {
//                days += d1.getActualMaximum(Calendar.DAY_OF_YEAR);
//                d1.add(Calendar.YEAR, 1);
//            } while (d1.get(Calendar.YEAR) != y2);
            return 11;
        }
        return days;
    }



    public static String getLogPath() {
        try {
            if (!isSdcardExit()) {
                throw new IllegalStateException("sd card no found");
            }

            String fileBasePath = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()
                    + "/" + "ur_play" + "/log/";

            File file = new File(fileBasePath);
            //创建目录
            if (!file.exists()) {
                file.mkdirs();
            }

            return fileBasePath;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }




    /**
     * 判断是否有外部存储设备sdcard
     *
     * @return true | false
     */
    public static boolean isSdcardExit() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }
}
