# 极狐端/运管端
--------
## 集成使用文档

### 配置appkey
在项目根目录下的local.properties里配置appkey
EASEMOB_APPKEY={your appkey}

### 极狐APP
1.初始化
Application里调用EaseIMHelper初始化
EaseIMHelper.getInstance().init(this);

初始化IM
EaseIMHelper.getInstance().initChat(false);

2.登录
EaseIMHelper.getInstance().loginChat(username, password, new EMCallBack(){});

3.跳转聊天页面
//跳转专属服务群
EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_EXCLUSIVE);

//跳转我的聊天
EaseIMHelper.getInstance().startChat(MainActivity.this, EaseConstant.CON_TYPE_MY_CHAT);

4.获取未读数
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

5.退出登录
EaseIMHelper.getInstance().logoutChat(new EMCallBack(){});

### 运管端
1.初始化
Application里调用EaseIMHelper初始化
EaseIMHelper.getInstance().init(this);

初始化IM
EaseIMHelper.getInstance().initChat(true);

2.跳转登录界面
startActivity(new Intent(this, AdminLoginActivity.class));

3.退出登录
EaseIMHelper.getInstance().logoutChat(new EMCallBack(){});


### 离线推送集成
在项目根目录下的local.properties里配置vivo push的appid和appkey
VIVO_PUSH_APPID=xxx
VIVO_PUSH_APPKEY=xxx

在main目录下创建assets/config.properties文件，配置其他离线推送参数
MEIZU_PUSH_APPID=xxx
MEIZU_PUSH_APPKEY=xxx
MI_PUSH_APPID=xxx
MI_PUSH_APPKEY=xxx
OPPO_PUSH_APPKEY=xxx
OPPO_PUSH_APPSECRET=xxx















