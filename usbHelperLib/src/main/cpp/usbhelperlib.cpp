#include <jni.h>
#include <android/log.h>
#include <iostream>
#include <vector>
#include <queue>
#include <mutex>
#include <time.h>
#include <unistd.h>

#define LOG_TAG "UsbSerialJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


// 通用的 writeData 方法
void writeData(JNIEnv *env, jobject thiz, jbyteArray data) {
    // 获取 UsbSerialHelper 类
    jclass helperClass = env->GetObjectClass(thiz);
    if (helperClass == nullptr) {
        LOGE("Failed to get UsbSerialHelper class");
        return;
    }

    // 获取 UsbSerialPort 字段
    jfieldID usbSerialPortFieldId = env->GetFieldID(helperClass, "usbSerialPort", "Lcom/hoho/android/usbserial/driver/UsbSerialPort;");
    if (usbSerialPortFieldId == nullptr) {
        LOGE("Failed to get field ID for usbSerialPort");
        return;
    }

    // 获取 UsbSerialPort 对象
    jobject usbSerialPort = env->GetObjectField(thiz, usbSerialPortFieldId);
    if (usbSerialPort == nullptr) {
        LOGE("Failed to get usbSerialPort object");
        return;
    }

    // 获取 UsbSerialPort 类
    jclass usbSerialPortClass = env->GetObjectClass(usbSerialPort);
    if (usbSerialPortClass == nullptr) {
        LOGE("Failed to get usbSerialPort class");
        return;
    }

    // 获取 write 方法 ID
    jmethodID writeMethodId = env->GetMethodID(usbSerialPortClass, "write", "([BI)V");
    if (writeMethodId == nullptr) {
        LOGE("Failed to get method ID for write");
        return;
    }

    // 调用 write 方法
    jint timeout = 1000;  // 超时时间（毫秒）
    env->CallVoidMethod(usbSerialPort, writeMethodId, data, timeout);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_auth(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x03, (jbyte) 0x30, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_getDeviceInfo(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x03, (jbyte) 0x20, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_open(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0xC3, (jbyte) 0xA5, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_close(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x81, (jbyte) 0x0A, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_startRecharge(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x00, (jbyte) 0x01, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_closeRecharge(
        JNIEnv *env,
        jobject thiz) {

    // 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x00, (jbyte) 0x08, 13};
    jsize length = sizeof(nativeData) / sizeof(nativeData[0]);

    // 创建 jbyteArray
    jbyteArray data = env->NewByteArray(length);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 将 Native 数据复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, length, nativeData);
    // 调用 write 方法
    LOGE("CallVoidMethod");
    writeData(env, thiz, data);
    // 释放 jbyteArray
    env->DeleteLocalRef(data);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_authVcode(JNIEnv *env, jobject thiz, jbyteArray javaArray) {
    // 1. 获取 Java 数组的长度
    jsize javaArrayLength = env->GetArrayLength(javaArray);

    // 2. 创建 Native 数据
    jbyte nativeData[] = {(jbyte) 0xF5, (jbyte) 0x5F, (jbyte) 0x03, (jbyte) 0x01, 13};
    jsize nativeDataLength = sizeof(nativeData) / sizeof(nativeData[0]);
    // 3. 创建一个新的数组，用于存储合并后的数据
    jsize newArrayLength = nativeDataLength + javaArrayLength;
    jbyte newArray[newArrayLength];
    // 4. 将 nativeData 复制到 newArray 中，并在倒数第二位插入 Java 数组
    int insertPosition = nativeDataLength - 1; // 倒数第二位
    for (int i = 0; i < insertPosition; i++) {
        newArray[i] = nativeData[i];
    }
    for (int i = 0; i < javaArrayLength; i++) {
        jbyte element; // 本地缓冲区，用于存储单个字节
        env->GetByteArrayRegion(javaArray, i, 1, &element); // 读取一个字节到 element
        newArray[insertPosition + i] = element; // 将读取的字节存储到 newArray
    }

    newArray[insertPosition + javaArrayLength] = nativeData[insertPosition];

    // 5. 创建 jbyteArray
    jbyteArray data = env->NewByteArray(newArrayLength);
    if (data == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    // 6. 将 newArray 复制到 jbyteArray
    env->SetByteArrayRegion(data, 0, newArrayLength, newArray);

    writeData(env, thiz, data);

    // 8. 释放 jbyteArray
    env->DeleteLocalRef(data);
}

std::queue<std::vector<uint8_t>> dataQueue; // 存储 byte[] 的队列
std::mutex queueMutex; // 保护队列的互斥锁
volatile bool isRunning = false; // 控制消费线程运行状态
// Java 对象和方法的全局引用
JavaVM *jvm = nullptr;
jobject javaSerialPortObj = nullptr;
jmethodID javaWriteMethod = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_pushData(JNIEnv *env, jobject thiz, jbyteArray jData) {
    jbyte *data = env->GetByteArrayElements(jData, nullptr);
    jsize length = env->GetArrayLength(jData);

    // 将 jbyteArray 转为 std::vector<uint8_t>
    std::vector<uint8_t> buffer(data, data + length);

    // 加锁并推入队列
    std::lock_guard<std::mutex> lock(queueMutex);
    dataQueue.push(buffer);

    env->ReleaseByteArrayElements(jData, data, JNI_ABORT); // 释放 Java 数组
}

void* consumerThread(void* arg) {
    JNIEnv *env;
    jvm->AttachCurrentThread(&env, nullptr); // 绑定当前线程到 JVM

    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);

    while (isRunning) {
        // 增加 10ms
        ts.tv_nsec += 10 * 1000000; // 10ms = 10,000,000 ns
        if (ts.tv_nsec >= 1000000000) {
            ts.tv_sec++;
            ts.tv_nsec -= 1000000000;
        }

        // 高精度休眠
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &ts, nullptr);

        // 检查队列是否有数据
        std::lock_guard<std::mutex> lock(queueMutex);
        if (!dataQueue.empty()) {
            std::vector<uint8_t> data = dataQueue.front();
            dataQueue.pop();

            // 处理数据（示例：打印大小）
//            LOGE("Consumed data size: %zu", data.size());
            // 3. 回调 Java 方法写串口
            jbyteArray jData = env->NewByteArray(data.size());
            env->SetByteArrayRegion(jData, 0, data.size(), (jbyte *) data.data());
            writeData(env,javaSerialPortObj,jData);
//            env->CallVoidMethod(javaSerialPortObj, javaWriteMethod, jData);
            env->DeleteLocalRef(jData);
        }
    }

    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_startConsumer(JNIEnv *env, jobject obj) {
    if (isRunning) {
        LOGE("Consumer already running!");
        return;
    }

    isRunning = true;
    // 1. 保存 Java 对象和方法 ID（供后续回调）
    env->GetJavaVM(&jvm); // 获取 JavaVM
    javaSerialPortObj = env->NewGlobalRef(obj); // 全局引用
    jclass clazz = env->GetObjectClass(obj);
    javaWriteMethod = env->GetMethodID(clazz, "writeToSerialPort", "([B)V");

    // 2. 启动消费线程
    pthread_t thread;
    pthread_create(&thread, nullptr, consumerThread, nullptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_duyansoft_usbhelperlib_UsbHelper_stopConsumer(JNIEnv *env, jobject thiz) {
    isRunning = false;
}