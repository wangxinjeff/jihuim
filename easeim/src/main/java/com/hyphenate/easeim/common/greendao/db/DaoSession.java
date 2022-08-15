package com.hyphenate.easeim.common.greendao.db;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.hyphenate.easeui.domain.EaseUser;

import com.hyphenate.easeim.common.greendao.db.EaseUserDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig easeUserDaoConfig;

    private final EaseUserDao easeUserDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        easeUserDaoConfig = daoConfigMap.get(EaseUserDao.class).clone();
        easeUserDaoConfig.initIdentityScope(type);

        easeUserDao = new EaseUserDao(easeUserDaoConfig, this);

        registerDao(EaseUser.class, easeUserDao);
    }
    
    public void clear() {
        easeUserDaoConfig.clearIdentityScope();
    }

    public EaseUserDao getEaseUserDao() {
        return easeUserDao;
    }

}
