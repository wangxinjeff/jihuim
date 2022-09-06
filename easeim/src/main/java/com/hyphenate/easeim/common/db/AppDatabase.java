package com.hyphenate.easeim.common.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.hyphenate.easeim.common.db.converter.DateConverter;
import com.hyphenate.easeim.common.db.dao.EmUserDao;
import com.hyphenate.easeim.common.db.entity.EmUserEntity;

@Database(entities = {EmUserEntity.class},
        version = 17)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract EmUserDao userDao();
}
