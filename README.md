# 运管端
--------
## 集成使用文档

### 配置
在项目的AndroidManifest.xml配置以下几项
1.环信的appkey和百度地图的appkey
```
<!-- 设置环信应用的AppKey -->
<meta-data android:name="EASEMOB_APPKEY"  android:value="${EASEMOB_APPKEY}" />

<!-- 设置百度地图的AppKey，用于发送位置消息 -->
<meta-data
   android:name="com.baidu.lbsapi.API_KEY"
   android:value="${BAIDU_LOCATION_APPKEY}" />
```  
2.FileProvider
 ```
          <provider
              android:name="androidx.core.content.FileProvider"
              android:authorities="${applicationId}.fileProvider"
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

在project build.gradle配置
```
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.greenrobot:greendao-gradle-plugin:3.3.0' // add plugin
    }
}
```  

### api
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















