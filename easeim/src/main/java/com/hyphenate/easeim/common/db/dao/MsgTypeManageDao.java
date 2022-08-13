package com.hyphenate.easeim.common.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.hyphenate.easeim.common.db.entity.MsgTypeManageEntity;

import java.util.List;

@Dao
public interface MsgTypeManageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insert(MsgTypeManageEntity... entities);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(MsgTypeManageEntity... entities);

    @Query("select * from em_msg_type")
    List<MsgTypeManageEntity> loadAllMsgTypeManage();

    @Query("select * from em_msg_type where type = :type")
    MsgTypeManageEntity loadMsgTypeManage(String type);

    @Delete
    void delete(MsgTypeManageEntity... entities);
}
