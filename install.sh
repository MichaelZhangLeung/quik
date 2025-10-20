device=$1
echo $device
adb connect $device
adb -s $device install /Users/ldd/zl/repos_local/android-uvc-rtmp-stream/app/build/intermediates/apk/release/anmi-poc-demo-115.apk
#adb -s $device install /Users/ldd/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/D573B57F81FAFDD75794C3E9A33635CD/Caches/Files/2025-06/564dac7d4d9bcb8ae6f0e5bcc3e3f93a/anmi-poc-demo-115.apk
