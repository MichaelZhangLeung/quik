package com.anmi.camera.uvcplay.utils;

public class Config {

    public  static final String HEADER_CALLEE = "X-Wpcallee";//被叫
    public  static final String HEADER_CALLERID= "X-Wpcallerid";
    public  static final String HEADER_CALLERER= "X-Wpcaller";//主叫
    public  static final String HEADER_CALL_ICCID= "X-Wpiccid";

    public static final String ANMI_ACTION_LOGIN = "ANMI_ACTION_LOGIN";
    public static final String ANMI_ACTION_CALL_IN = "ANMI_ACTION_CALL_IN";//呼入
    public static final String ANMI_ACTION_CALL_OUT = "ANMI_ACTION_CALL_OUT";//呼出
    public static final String ANMI_ACTION_CALL_ACTIVE = "ANMI_ACTION_CALL_ACTIVE";//接通
    public static final String ANMI_ACTION_CALL_DISCONNECTED = "ANMI_ACTION_CALL_DISCONNECTED";//挂断

    public static final String ANMI_PHONE_VALUE_TARGET_NUMBER = "ANMI_PHONE_VALUE_TARGET_NUMBER";//对方手机号
    public static final String ANMI_PHONE_VALUE_ICCID = "ANMI_PHONE_VALUE_ICCID";//当前通话的iccId

    public static final String BUNDLE_WECHAT_ID = "BUNDLE_WECHAT_ID";//微信号id


    public static final String USB_ACTION_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String USB_ACTION_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String USB_ACTION_PERMISSION = "com.duyansoft.cti.plus.USB_PERMISSION";

    /**
     * 安米微信音视频事件
     */
    public static final String WECHAT_ACTION_OUTCOME_AUDIO_CALL = "anmi.wechat.outcome.audio.call";
    public static final String WECHAT_ACTION_INCOMING_AUDIO_CALL = "anmi.wechat.incoming.audio.call";
    public static final String WECHAT_ACTION_CONNECTED_AUDIO_CALL = "anmi.wechat.connected.audio.call";
    public static final String WECHAT_ACTION_DISCONNECTED_AUDIO_CALL = "anmi.wechat.disconnected.audio.call";


    /**
     * 官方微信音视频事件
     */
    public static final String OFFICIAL_WECHAT_ACTION_OUTCOME_AUDIO_CALL = "official.wechat.outcome.audio.call";
    public static final String OFFICIAL_WECHAT_ACTION_OUTCOME_SEND_AUDIO_CALL = "official.wechat.outcome.send.audio.call";
    public static final String OFFICIAL_WECHAT_ACTION_INCOMING_AUDIO_CALL = "official.wechat.incoming.audio.call";
    public static final String OFFICIAL_WECHAT_ACTION_CONNECTED_AUDIO_CALL = "official.wechat.connected.audio.call";
    public static final String OFFICIAL_WECHAT_ACTION_DISCONNECTED_AUDIO_CALL = "official.wechat.disconnected.audio.call";
    public static final String OFFICIAL_WECHAT_ACTION_TIMEOUT_AUDIO_CALL = "official.wechat.timeout.audio.call";//节点超时
    //2min 定时广播事件
    public static final String ALARM_2MINS_ACTION = "ANMI_ACTION_UPLOAD";

    public static final String KEY_USER_ID = "userId";

    public static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";//QQ的包名
    public static final String PACKAGE_NAME_MM = "com.tencent.mm";//微信的包名

    public static final String KEY_BOX_IMEI = "key_box_imei";
    public static final String KEY_BOX_BATCH= "key_box_batch";
}

