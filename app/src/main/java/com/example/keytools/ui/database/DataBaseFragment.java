package com.example.keytools.ui.database;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.R;
import com.example.keytools.ui.database.data.KeyBaseDbHelper;

import java.io.IOException;
import java.util.Random;

import static com.example.keytools.MainActivity.sPort;
import com.example.keytools.ui.database.data.KeyBaseContract.UidKey;
import com.example.keytools.ui.database.data.KeyBaseContract.Adresses;

public class DataBaseFragment extends Fragment {

    private TextView TextWin;
    private TextView TextAdress;
    private TextView TextTagNumber;
    private KeyTools keytools;
    private ProgressDialog pd;
    Toast toast;
    String s;
    ADD_UID adduid;
    KeyBaseDbHelper mDbHelper ;
    private static int AdressIndex = 1;
    private Cursor AdressCursor;
    private Cursor UidCursor;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_database, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        TextAdress = root.findViewById(R.id.textAdress);
        TextTagNumber = root.findViewById(R.id.textTagNumber);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnAddUID:
//                        StartAddUid();
                        AddUid();
                        break;
                    case R.id.btnShow:
                        ShowBase();
                        break;
                    case R.id.btnDelAdress:

                        DelAdress();
                        break;
                    case R.id.btnAddAdress:
                        AddAdress();
                        break;
                    case R.id.btnSelectAdress:
                        SelectAdress();
                        break;
                }
            }
        };
        Button btnAddUID = root.findViewById(R.id.btnAddUID);
        Button btnShow = root.findViewById(R.id.btnShow);
        Button btnDel = root.findViewById(R.id.btnDelAdress);
        Button btnAddAdress = root.findViewById(R.id.btnAddAdress);
        Button btnSelectAdress = root.findViewById(R.id.btnSelectAdress);

        btnAddUID.setOnClickListener(oclBtn);
        btnShow.setOnClickListener(oclBtn);
        btnDel.setOnClickListener(oclBtn);
        btnAddAdress.setOnClickListener(oclBtn);
        btnSelectAdress.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.Отмена), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });

        mDbHelper = new KeyBaseDbHelper(getContext());
        ShowDef();

        return root;
    }


    void DelAdress(){

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Удаление адреса объекта !")
                .setMessage("Вы действительно хотите удалить текущий адрес из базы данных ?")
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        deleteStringAdress();
                    }
                })
                .show();
    }


    void deleteStringAdress(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        try {
        String where = "_id = " + AdressIndex;
        AdressCursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                null,            // столбцы
                where,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки
            int idColumnIndex = AdressCursor.getColumnIndex(Adresses._ID);
            int adressColumnIndex = AdressCursor.getColumnIndex(Adresses.COLUMN_ADRESS);
            if(!AdressCursor.moveToFirst()){
                toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            AdressIndex = AdressCursor.getInt(idColumnIndex);
            String s = AdressCursor.getString(adressColumnIndex);
            TextWin.setText("Удаление " + AdressIndex + " " + s);
            db.delete(
                    Adresses.TABLE_NAME,
                    where,
                    null);
            AdressCursor = db.query(
                    Adresses.TABLE_NAME,   // таблица
                    null,            // столбцы
                    null,                  // столбцы для условия WHERE
                    null,                  // значения для условия WHERE
                    null,                  // Don't group the rows
                    null,                  // Don't filter by row groups
                    null);                   // порядок сортировки
            if(!AdressCursor.moveToFirst()){
                toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                TextAdress.setText("");
                AdressIndex = 0;
                String SQL_CREATE_ADRESSES_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                + 0 +" WHERE NAME='" + Adresses.TABLE_NAME +"'";
                db.execSQL(SQL_CREATE_ADRESSES_TABLE);
            }else{
                AdressIndex = AdressCursor.getInt(idColumnIndex);
                s = AdressCursor.getString(adressColumnIndex);
                TextAdress.setText(s);
            }

        } catch (RuntimeException e){
            toast = Toast.makeText(getContext(), "Ошибка удаления !\n" +
                    e.toString(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.append(e.toString());
        }
        finally {
            AdressCursor.close();
        }

    }


    void SelectAdress(){

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        AdressCursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                null,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                Adresses.COLUMN_ADRESS );                   // порядок сортировки
            android.app.AlertDialog.Builder adb = new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Адрес")
                    .setCursor(AdressCursor, myClickListener, Adresses.COLUMN_ADRESS);
            adb.show();
    }


    // обработчик нажатия на пункт списка диалога

    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            AdressCursor.moveToPosition(which);
            int idColumnIndex = AdressCursor.getColumnIndex(Adresses._ID);
            int adressColumnIndex = AdressCursor.getColumnIndex(Adresses.COLUMN_ADRESS);
            AdressIndex = AdressCursor.getInt(idColumnIndex);
            String s = AdressCursor.getString(adressColumnIndex);
            TextAdress.setText(s);
            TextWin.append("\n AdressIndex = " + AdressIndex);
        }
    };


    void AddAdress(){
        final EditText txtAdr = new EditText(getContext());

        txtAdr.setHint("");
        new AlertDialog.Builder(getContext())
                .setTitle("Ввод адреса")
                .setMessage("Введите адрес объекта :")
                .setView(txtAdr)
                .setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String s = txtAdr.getText().toString();
                        addAdrToBase(s);
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }


    void addAdrToBase(String s){

        if(s.length() == 0){
            toast = Toast.makeText(getContext(), "Ошибка !\nНе введен адрес !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Adresses.COLUMN_ADRESS, s);
        long newRowId = db.insert(Adresses.TABLE_NAME, null, values);
        if(newRowId == -1){
            toast = Toast.makeText(getContext(), "Ошибка !\nАдрес уже в базе !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }else{
            toast = Toast.makeText(getContext(), "Адрес добавлен в базу !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        ShowDef();
    }


    void AddUid(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        Random random = new Random();
        values.put(UidKey.COLUMN_UID, random.nextInt());
        long newRowId = db.insert(UidKey.TABLE_NAME, null, values);
        if(newRowId == -1){
            toast = Toast.makeText(getContext(), "Эта метка уже есть в базе !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }else{
            toast = Toast.makeText(getContext(), "Метка добавлена в базу !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        ShowDef();
    }


    void ShowDef(){

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                UidKey._ID,
                UidKey.COLUMN_UID };
        UidCursor = db.query(
                UidKey.TABLE_NAME,   // таблица
                projection,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки

        try {
            TextTagNumber.setText( "" + UidCursor.getCount());
        } finally {
            UidCursor.close();
        }


        AdressCursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                null,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                "adress");                   // порядок сортировки
        try {
            int idColumnIndex = AdressCursor.getColumnIndex(Adresses._ID);
            int adressColumnIndex = AdressCursor.getColumnIndex(Adresses.COLUMN_ADRESS);
            if(!AdressCursor.moveToFirst()){
                toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }else{
                AdressIndex = AdressCursor.getInt(idColumnIndex);
                String s = AdressCursor.getString(adressColumnIndex);
                TextAdress.setText(s);
            }
        } finally {
            AdressCursor.close();
        }
    }


    void ShowBase(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Зададим условие для выборки - список столбцов
        String[] projection = {
                UidKey._ID,
                UidKey.COLUMN_UID };

        // Делаем запрос
        UidCursor = db.query(
                UidKey.TABLE_NAME,   // таблица
                projection,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            TextWin.setText("Таблица содержит " + UidCursor.getCount() + " меток.\n\n");
            TextWin.append(UidKey._ID + " - " +
                    UidKey.COLUMN_UID + "\n");

            // Узнаем индекс каждого столбца
            int idColumnIndex = UidCursor.getColumnIndex(UidKey._ID);
            int uidColumnIndex = UidCursor.getColumnIndex(UidKey.COLUMN_UID);

            // Проходим через все ряды
            while (UidCursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = UidCursor.getInt(idColumnIndex);
                int currentUID = UidCursor.getInt(uidColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - " + String.format("%08X", currentUID)
                        ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            UidCursor.close();
        }

        String[] projection1 = {
                Adresses._ID,
                Adresses.COLUMN_ADRESS };

        // Делаем запрос
        AdressCursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                projection1,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            TextWin.append("\nТаблица содержит " + AdressCursor.getCount() + " Адресов.\n\n");
            TextWin.append(Adresses._ID + " - " +
                    Adresses.COLUMN_ADRESS + "\n");

            // Узнаем индекс каждого столбца
            int idColumnIndex = AdressCursor.getColumnIndex(Adresses._ID);
            int uidColumnIndex = AdressCursor.getColumnIndex(Adresses.COLUMN_ADRESS);

            // Проходим через все ряды
            while (AdressCursor.moveToNext()) {
                // Используем индекс для получения строки или числа
                int currentID = AdressCursor.getInt(idColumnIndex);
                String s = AdressCursor.getString(uidColumnIndex);
                // Выводим значения каждого столбца
                TextWin.append(("\n" + currentID + " - " + s
                ));
            }
        } finally {
            // Всегда закрываем курсор после чтения
            AdressCursor.close();
        }
    }


    void StartAddUid(){

        if(KeyTools.Busy){
            return;
        }
        adduid = new ADD_UID();
        if (!adduid.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), adduid.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        adduid.execute();
        KeyTools.Busy = true;
    }


    private void Cancel(){
        if (adduid != null) {
            adduid.cancel(true);
        }
    }


    class ADD_UID extends AsyncTask<Void, Integer, Integer> {

        KeyTools keytools;
        long defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB = 0;
        int ntag = 0;
        ContentValues values;
        SQLiteDatabase db;
        long newRowId;


        ADD_UID(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            db = mDbHelper.getWritableDatabase();
            values = new ContentValues();


            pd.setTitle("Добавление меток в базу");
            pd.setMessage(getString(R.string.Поднесите_заготовку_Classic_к_устройству));
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try{
                while(true){
                    while(true){
                        while(!keytools.readuid(sPort)){        // Считывание UID
                            if (isCancelled()) {
                                return null;
                            }
                        }
                        if(keytools.authent(sPort,block, AB, defkey)){
                            values.put(UidKey.COLUMN_UID, keytools.uid);
                            newRowId = db.insert(UidKey.TABLE_NAME, null, values);
                            if(newRowId == -1){
                                publishProgress(1);
                            }else{
                                publishProgress(2);
                                ntag++;
                            }
                        }else{
                            publishProgress(3);
                        }
                        while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                            if (isCancelled()) {
                                return null;
                            }
                        }
                    }
                }
            }catch(IOException e1){
                try{
                    sPort.close();
                }catch(IOException e){
                }
                sPort = null;
                return -1;
            }
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch(values[0]) {
                case 1:
                    toast = Toast.makeText(getContext(), "Эта метка уже есть в базе !", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 2:
                    toast = Toast.makeText(getContext(), "Метка добавлена в базу !", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 3:
                    toast = Toast.makeText(getContext(), "Сектор 0 закрыт !\n Эта метка не подходит !", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

            }
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            s = "Запись завершена !" + "\n" + String.format("Добавлено меток - %d ", ntag);
            toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.setText(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }
    }


    @Override
    public void onStop() {
        AdressCursor.close();
        UidCursor.close();
        super.onStop();
    }
}