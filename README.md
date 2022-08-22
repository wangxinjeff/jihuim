# 极狐端/运管端
--------
## 集成使用文档

### 配置appkey
在项目的AndroidManifest.xml中配置以下几项
环信的appkey和百度地图的appkey
```
<!-- 设置环信应用的AppKey -->
<meta-data android:name="EASEMOB_APPKEY"  android:value="${EASEMOB_APPKEY}" />

<!-- 设置百度地图的AppKey，用于发送位置消息 -->
<meta-data
   android:name="com.baidu.lbsapi.API_KEY"
   android:value="${BAIDU_LOCATION_APPKEY}" />
```   

 FileProvider
```
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:grantUriPermissions="true"
            android:exported="false"
            >
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"
                />
        </provider>
```
在res下创建xml目录，创建file_paths.xml文件,配置以下内容
```
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path path="Android/data/${applicationId}/" name="files_root" />
    <external-path path="." name="external_storage_root" />
</paths>
```

### 极狐APP
1.初始化
Application里调用EaseIMHelper初始化，传入server url
```
EaseIMHelper.getInstance().init(this, "http://182.92.236.214:12005/");
```

初始化IM
```
EaseIMHelper.getInstance().initChat(false);
```

2.登录
```
EaseIMHelper.getInstance().loginChat(username, password, new EMCallBack(){});
```

3.退出登录
```
EaseIMHelper.getInstance().logoutChat(new EMCallBack(){});
```

4.设置极狐aid和token
```
EaseIMHelper.getInstance().setAid(aid);
EaseIMHelper.getInstance().setAidToken(token);
```

5.跳转聊天页面
//跳转专属服务群
```
EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_EXCLUSIVE);
```

//跳转我的聊天
```
EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_MY_CHAT);
```

6.获取未读数
```
EaseIMHelper.getInstance().getChatUnread(new EMValueCallBack<Map<String, Integer>>() {
	        @Override
            public void onSuccess(Map<String, Integer> stringIntegerMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	//专属群未读数
                        stringIntegerMap.get(EaseConstant.UNREAD_EXCLUSIVE_GROUP);
                        //其他聊天未读数
                        stringIntegerMap.get(EaseConstant.UNREAD_MY_CHAT);
                    }
                });
            }

            @Override
            public void onError(int i, String s) {

            }
});
```

7.设置server地址
```
EaseIMHelper.getInstance().setServerHost("");
```

8.未读数变更通知
```
LiveDataBus.get().with(EaseConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                // 处理刷新UI
            }
        });
```

9.app内横幅通知
```
// 设置横幅的小图标和名称
InAppNotification.getInstance().setNotifyName("极狐App")
                .setNotifyIcon(R.drawable.em_chatfrom_voice_playing_f3);

// 在需要展示横幅的activity里添加代码
    @Override
    protected void onResume() {
        super.onResume();
        InAppNotification.getInstance().init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        InAppNotification.getInstance().hideNotification();
    }
```

### 运管端
1.初始化
Application里调用EaseIMHelper初始化
```
EaseIMHelper.getInstance().init(this);
```

初始化IM
```
EaseIMHelper.getInstance().initChat(true);
```

2.跳转登录界面
```
startActivity(new Intent(this, AdminLoginActivity.class));
```

3.退出登录
```
EaseIMHelper.getInstance().logoutChat(new EMCallBack(){});
```
4.获取未读数
```
EaseIMHelper.getInstance().getChatUnread(new EMValueCallBack<Map<String, Integer>>() {
	        @Override
            public void onSuccess(Map<String, Integer> stringIntegerMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	//未读总数
                        stringIntegerMap.get(EaseConstant.UNREAD_TOTAL);
                    }
                });
            }

            @Override
            public void onError(int i, String s) {

            }
});
```

### 离线推送集成
在项目的AndroidManifest.xml中配置vivo push的appid和appkey
```
<meta-data
   android:name="com.vivo.push.api_key"
   android:value="${VIVO_PUSH_APPKEY}" />
<meta-data
   android:name="com.vivo.push.app_id"
   android:value="${VIVO_PUSH_APPID}" />
```

在main目录下创建assets/config.properties文件，配置其他离线推送参数
```
MEIZU_PUSH_APPID=xxx
MEIZU_PUSH_APPKEY=xxx
MI_PUSH_APPID=xxx
MI_PUSH_APPKEY=xxx
```
华为推送需要在项目里导入agconnect-services.json

如果报错Manifest冲突在gradle.properties里添加
android.useNewApkCreator=false















