package com.ruoyu.secme.HelperFile;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class ChatDBHelper extends SQLiteOpenHelper {

    public ChatDBHelper(@Nullable Context context,String db_name){
        super(context,db_name,null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("");
        db.execSQL("");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
