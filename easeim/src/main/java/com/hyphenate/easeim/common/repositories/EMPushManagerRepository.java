package com.hyphenate.easeim.common.repositories;

import android.support.annotation.NonNull;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMPushConfigs;
import com.hyphenate.chat.EMPushManager;
import com.hyphenate.easeim.common.interfaceOrImplement.ResultCallBack;
import com.hyphenate.easeim.common.net.Resource;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.exceptions.HyphenateException;

public class EMPushManagerRepository extends BaseEMRepository {

    private static EMPushManagerRepository instance;

    public static EMPushManagerRepository getInstance() {
        if(instance == null) {
            synchronized (EMPushManagerRepository.class) {
                if(instance == null) {
                    instance = new EMPushManagerRepository();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取推送配置
     * @return
     */
    public void getPushConfigsFromServer() {
                EaseThreadManager.getInstance().runOnIOThread(()-> {
                    EMPushConfigs configs = null;
                    try {
                        configs = getPushManager().getPushConfigsFromServer();
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * 获取推送配置
     * @return
     */
    public EMPushConfigs fetchPushConfigsFromServer() {
        try {
            return getPushManager().getPushConfigsFromServer();
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
        return null;
    }
}
