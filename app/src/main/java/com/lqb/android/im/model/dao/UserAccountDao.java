package com.lqb.android.im.model.dao;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lqb.android.im.model.bean.UserInfo;
import com.lqb.android.im.model.db.UserAccountDB;

// 用户账户数据库的操作类
public class UserAccountDao {
    private final UserAccountDB mHelper;

    public UserAccountDao(Context context) {
        mHelper = new UserAccountDB(context);
    }

    // 添加用户到数据库
    public void addAccount(UserInfo user) {
        // 获取数据库对象
        SQLiteDatabase db = mHelper.getWritableDatabase();

        // 执行添加操作
        ContentValues values = new ContentValues();
        values.put(UserAccountTable.COL_NAME, user.getName());
        values.put(UserAccountTable.COL_HXID, user.getHxId());
        values.put(UserAccountTable.COL_NICK, user.getNick());
        values.put(UserAccountTable.COL_PHOTO, user.getPhoto());
        db.replace(UserAccountTable.TAB_NAME, null, values);
    }

    // 根据环信id获取所有用户信息
    public UserInfo getAccountByHxId(String hxId) {
        // 1.获取数据库对象
        SQLiteDatabase db = mHelper.getReadableDatabase();

        // 2.执行查询语句
        String sql = "select * from " + UserAccountTable.TAB_NAME + " where " +
                UserAccountTable.COL_NAME + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{hxId});

        UserInfo userInfo = null;
        if (cursor.moveToNext()) {
            userInfo = new UserInfo();

            //封装对象
            userInfo.setName(cursor.getString(cursor.getColumnIndex(UserAccountTable.COL_NAME)));
            userInfo.setHxId(cursor.getString(cursor.getColumnIndex(UserAccountTable.COL_HXID)));
            userInfo.setNick(cursor.getString(cursor.getColumnIndex(UserAccountTable.COL_NICK)));
            userInfo.setPhoto(cursor.getString(cursor.getColumnIndex(UserAccountTable.COL_PHOTO)));
        }

        // 3.关闭资源
        cursor.close();

        // 4.返回数据
        return userInfo;
    }

}
