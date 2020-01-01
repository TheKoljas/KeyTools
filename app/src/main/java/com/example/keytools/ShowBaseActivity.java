package com.example.keytools;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.keytools.ui.database.data.KeyBaseContract;
import com.example.keytools.ui.database.data.KeyBaseDbHelper;

public class ShowBaseActivity extends AppCompatActivity {

    private static KeyBaseDbHelper mDbHelper ;
    private TextView TextWin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_base);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextWin = findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);

        mDbHelper = new KeyBaseDbHelper(this);
        ShowBase();
    }


    public void ShowBase(){
        String s;

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Зададим условие для выборки - список столбцов
        String[] projection = {
                KeyBaseContract.UidKey._ID,
                KeyBaseContract.UidKey.COLUMN_UID };

        // Делаем запрос
        Cursor cursor = db.query(
                KeyBaseContract.UidKey.TABLE_NAME,   // таблица
                projection,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            s = "Таблица содержит " + cursor.getCount() + " меток.\n\n";
            TextWin.setText(s);
            TextWin.append(KeyBaseContract.UidKey._ID + " - " +
                    KeyBaseContract.UidKey.COLUMN_UID + "\n");

            // Узнаем индекс каждого столбца
            int idColumnIndex = cursor.getColumnIndex(KeyBaseContract.UidKey._ID);
            int uidColumnIndex = cursor.getColumnIndex(KeyBaseContract.UidKey.COLUMN_UID);

            // Проходим через все ряды
            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = cursor.getInt(idColumnIndex);
                int currentUID = cursor.getInt(uidColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - " + String.format("%08X", currentUID)
                ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            cursor.close();
        }

        String[] projection1 = {
                KeyBaseContract.Adresses._ID,
                KeyBaseContract.Adresses.COLUMN_ADRESS };

        // Делаем запрос
        cursor = db.query(
                KeyBaseContract.Adresses.TABLE_NAME,   // таблица
                projection1,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            TextWin.append("\n\nТаблица содержит " + cursor.getCount() + " Адресов.\n\n");
            TextWin.append(KeyBaseContract.Adresses._ID + " - " +
                    KeyBaseContract.Adresses.COLUMN_ADRESS + "\n");

            // Узнаем индекс каждого столбца
            int idColumnIndex = cursor.getColumnIndex(KeyBaseContract.Adresses._ID);
            int uidColumnIndex = cursor.getColumnIndex(KeyBaseContract.Adresses.COLUMN_ADRESS);

            // Проходим через все ряды
            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = cursor.getInt(idColumnIndex);
                s = cursor.getString(uidColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - " + s
                ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            cursor.close();
        }



        // Делаем запрос
        cursor = db.query(
                KeyBaseContract.KeyAdress.TABLE_NAME,   // таблица
                null,            // столбцы
//                KeyAdress.COLUMN_KEYADRESS + "=" + AdressIndex,                  // столбцы для условия WHERE
                null,
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            TextWin.append("\n\nТаблица содержит " + cursor.getCount() + " ключей.\n\n");
            TextWin.append(KeyBaseContract.KeyAdress._ID + " - "
                    + KeyBaseContract.KeyAdress.COLUMN_KEYADRESS + " - "
                    +  KeyBaseContract.KeyAdress.COLUMN_UID + " - "
                    + KeyBaseContract.KeyAdress.COLUMN_CRYPTOKEY);

            // Узнаем индекс каждого столбца
            int idColumnIndex = cursor.getColumnIndex(KeyBaseContract.KeyAdress._ID);
            int keyadressColumnIndex = cursor.getColumnIndex(KeyBaseContract.KeyAdress.COLUMN_KEYADRESS);
            int uidkeyColumnIndex = cursor.getColumnIndex(KeyBaseContract.KeyAdress.COLUMN_UID);
            int cryptokeyColumnIndex = cursor.getColumnIndex(KeyBaseContract.KeyAdress.COLUMN_CRYPTOKEY);

            // Проходим через все ряды
            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = cursor.getInt(idColumnIndex);
                int currentKEYADRESS = cursor.getInt(keyadressColumnIndex);
                int currentUIDKEY = cursor.getInt(uidkeyColumnIndex);
                long currentCRYPTOKEY = cursor.getLong(cryptokeyColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - "
                        + currentKEYADRESS + " - "
                        + String.format("%08X - %012X", currentUIDKEY, currentCRYPTOKEY)
                ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            cursor.close();
        }

        // Делаем запрос
        cursor = db.query(
                KeyBaseContract.Recovery.TABLE_NAME,   // таблица
                null,            // столбцы
                null,
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            TextWin.append("\n\nКорзина содержит " + cursor.getCount() + " ключей.\n\n");
            TextWin.append(KeyBaseContract.Recovery._ID + " - "
                    +  KeyBaseContract.Recovery.COLUMN_UID + " - "
                    + KeyBaseContract.Recovery.COLUMN_CRYPTOKEY);

            // Узнаем индекс каждого столбца
            int idColumnIndex = cursor.getColumnIndex(KeyBaseContract.Recovery._ID);
            int uidkeyColumnIndex = cursor.getColumnIndex(KeyBaseContract.Recovery.COLUMN_UID);
            int cryptokeyColumnIndex = cursor.getColumnIndex(KeyBaseContract.Recovery.COLUMN_CRYPTOKEY);

            // Проходим через все ряды
            while (cursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = cursor.getInt(idColumnIndex);
                int currentUIDKEY = cursor.getInt(uidkeyColumnIndex);
                long currentCRYPTOKEY = cursor.getLong(cryptokeyColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - "
                        + String.format("%08X - %012X", currentUIDKEY, currentCRYPTOKEY)
                ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            cursor.close();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
