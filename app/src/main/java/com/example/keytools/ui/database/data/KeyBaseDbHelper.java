package com.example.keytools.ui.database.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.keytools.ui.database.data.KeyBaseContract.UidKey;
import com.example.keytools.ui.database.data.KeyBaseContract.Adresses;
import com.example.keytools.ui.database.data.KeyBaseContract.KeyAdress;
import com.example.keytools.ui.database.data.KeyBaseContract.Recovery;


public class KeyBaseDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "keytools.db";
    private static final int DATABASE_VERSION = 1;

    public KeyBaseDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String SQL_CREATE_UIDKEY_TABLE = "CREATE TABLE " + UidKey.TABLE_NAME + " ("
                + UidKey._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + UidKey.COLUMN_UID + " INTEGER NOT NULL UNIQUE DEFAULT -1);";
        // Запускаем создание таблицы
        db.execSQL(SQL_CREATE_UIDKEY_TABLE);

        String SQL_CREATE_ADRESSES_TABLE = "CREATE TABLE " + Adresses.TABLE_NAME + " ("
                + Adresses._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Adresses.COLUMN_ADRESS + " TEXT NOT NULL UNIQUE);";
        // Запускаем создание таблицы
        db.execSQL(SQL_CREATE_ADRESSES_TABLE);

        String SQL_CREATE_KEYADRESS_TABLE = "CREATE TABLE " + KeyAdress.TABLE_NAME + " ("
                + KeyAdress._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KeyAdress.COLUMN_KEYADRESS + " INTEGER,"
                + KeyAdress.COLUMN_UID + " INTEGER ,"
                + KeyAdress.COLUMN_CRYPTOKEY + " INTEGER);";
        // Запускаем создание таблицы
        db.execSQL(SQL_CREATE_KEYADRESS_TABLE);

        String SQL_CREATE_RECOVERY_TABLE = "CREATE TABLE " + Recovery.TABLE_NAME + " ("
                + Recovery._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Recovery.COLUMN_UID + " INTEGER UNIQUE ,"
                + Recovery.COLUMN_CRYPTOKEY + " INTEGER );";
        // Запускаем создание таблицы
        db.execSQL(SQL_CREATE_RECOVERY_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
