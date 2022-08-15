package com.hyphenate.easeim.common.db;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.hyphenate.easeim.common.greendao.db.DaoMaster;
import com.hyphenate.easeim.common.greendao.db.DaoSession;
import com.hyphenate.easeim.common.greendao.db.EaseUserDao;
import com.hyphenate.easeim.common.utils.MD5;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.List;

public class EaseDbHelper {
    private static final String TAG = "DemoDbHelper";
    private static EaseDbHelper instance;
    private Context mContext;
    private String currentUser;
    private DaoMaster.DevOpenHelper helper;
    private DaoSession daoSession;
    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    private EaseDbHelper(Context context){
        this.mContext = context.getApplicationContext();
    }

    public static EaseDbHelper getInstance(Context context) {
        if(instance == null) {
            synchronized (EaseDbHelper.class) {
                if(instance == null) {
                    instance = new EaseDbHelper(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化数据库
     * @param user
     */
    public void initDb(String user) {
        if(currentUser != null) {
            if(TextUtils.equals(currentUser, user)) {
                EMLog.i(TAG, "you have opened the db");
                return;
            }
            closeDb();
        }
        this.currentUser = user;
        String userMd5 = MD5.encrypt2MD5(user);
        // 以下数据库升级设置，为升级数据库将清掉之前的数据，如果要保留数据，慎重采用此种方式
        // 可以采用addMigrations()的方式，进行数据库的升级
        String dbName = String.format("em_%1$s.db", userMd5);
        EMLog.i(TAG, "db name = "+dbName);

        helper = new DaoMaster.DevOpenHelper(mContext, dbName, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    public LiveData<Boolean> getDatabaseCreatedObservable() {
        return mIsDatabaseCreated;
    }

    /**
     * 关闭数据库
     */
    public void closeDb() {
        currentUser = null;

        if(helper != null){
            helper.close();
            helper = null;
        }

        if(daoSession != null){
            daoSession.clear();
            daoSession = null;
        }
    }

    public boolean insertUsers(List<EaseUser> users){
        try {
            daoSession.runInTx(() -> {
                for (EaseUser entity : users) {
                    daoSession.insertOrReplace(entity);
                }
            });
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public List<EaseUser> loadAllEaseUsers(){
        return daoSession.loadAll(EaseUser.class);
    }

    public List<String> loadTimeOutEaseUsers(long arg0,long arg1){
        List<EaseUser> list = daoSession.queryBuilder(EaseUser.class).where(EaseUserDao.Properties.LastModifyTimestamp.le(arg1 - arg0)).list();
        List<String> names = new ArrayList<>();
        for(EaseUser entity : list){
            names.add(entity.getUsername());
        }
        return names;
    }

    public DaoSession getDaoSession(){
        return daoSession;
    }
}
