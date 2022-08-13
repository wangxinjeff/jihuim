package com.hyphenate.easeim.common.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.hyphenate.easeim.common.db.entity.AppKeyEntity;

import java.util.List;

@Dao
public interface AppKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insert(AppKeyEntity... keys);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insert(List<AppKeyEntity> keys);

    @Query("select * from app_key  order by timestamp asc")
    List<AppKeyEntity> loadAllAppKeys();

    @Query("delete from app_key where appKey = :arg0")
    void deleteAppKey(String arg0);

    @Query("select * from app_key where appKey = :arg0")
    List<AppKeyEntity> queryKey(String arg0);
}
