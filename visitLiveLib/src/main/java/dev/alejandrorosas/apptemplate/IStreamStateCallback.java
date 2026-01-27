package dev.alejandrorosas.apptemplate;


public interface IStreamStateCallback {

    void onStartStream();

    void onStopStream();
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onRtmpStreamConnected();
    void onRtmpStreamFailed();

    void onSafeInject();
}
