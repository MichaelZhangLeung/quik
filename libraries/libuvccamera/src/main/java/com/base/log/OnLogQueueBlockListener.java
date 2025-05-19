package com.base.log;

/**
 * 日志队列中积压日志过多的监听器
 */

public interface OnLogQueueBlockListener {
    /**
     * 只是用来通知使用者，日志队列已经发生了阻塞，工具会自动清空队列，并重置输出流
     */
    void onLogQueueBlock();
}
