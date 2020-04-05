package com.example.keytools.ui.database;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
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

import com.example.keytools.Crapto1;
import com.example.keytools.KeyTools;
import com.example.keytools.R;
import com.example.keytools.SettingsActivity;
import com.example.keytools.ui.database.data.KeyBaseDbHelper;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import static com.example.keytools.MainActivity.sPort;
import com.example.keytools.ui.database.data.KeyBaseContract.UidKey;
import com.example.keytools.ui.database.data.KeyBaseContract.Adresses;
import com.example.keytools.ui.database.data.KeyBaseContract.KeyAdress;
import com.example.keytools.ui.database.data.KeyBaseContract.Recovery;
import com.example.keytools.ui.sectorcopy.SectorCopyFragment;

public class DataBaseFragment extends Fragment {

    private TextView TextWin;
    private TextView TextAdress;
    private TextView TextTagNumber;
    private TextView TextAdressTagNumber;
    private EditText TextKod;
    private ProgressDialog pd;
    private Toast toast;
    private String s;
    public static KeyBaseDbHelper mDbHelper ;
    public static int AdressIndex = 0;

    private KEYGRAB keygrab;
    private UID readuid;
    private WriteClassic writekey;
    private ADD_UID adduid;
    private RECOVERY recovery;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_database, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        TextAdress = root.findViewById(R.id.textAdress);
        TextTagNumber = root.findViewById(R.id.textTagNumber);
        TextAdressTagNumber = root.findViewById(R.id.textAdressTagNumber);
        TextKod =  root.findViewById(R.id.textKod);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnAddUID:
                        StartAddUid();
//                        AddUid();
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
                    case R.id.btnGrab:
                        GrabKey();
                        break;
                    case R.id.btnWrtCls:
                        WriteKey();
                        break;
                    case R.id.btnReadKod:
                        ReadKod();
                        break;
                    case R.id.btnRecovery:
                        Recovery();
                        break;
                }
            }
        };
        Button btnAddUID = root.findViewById(R.id.btnAddUID);
        Button btnDel = root.findViewById(R.id.btnDelAdress);
        Button btnAddAdress = root.findViewById(R.id.btnAddAdress);
        Button btnSelectAdress = root.findViewById(R.id.btnSelectAdress);
        Button btnGrab = root.findViewById(R.id.btnGrab);
        Button btnWrtCls = root.findViewById(R.id.btnWrtCls);
        Button btnReadKod = root.findViewById(R.id.btnReadKod);
        Button btnRecovery = root.findViewById(R.id.btnRecovery);

        btnAddUID.setOnClickListener(oclBtn);
        btnDel.setOnClickListener(oclBtn);
        btnAddAdress.setOnClickListener(oclBtn);
        btnSelectAdress.setOnClickListener(oclBtn);
        btnGrab.setOnClickListener(oclBtn);
        btnWrtCls.setOnClickListener(oclBtn);
        btnReadKod.setOnClickListener(oclBtn);
        btnRecovery.setOnClickListener(oclBtn);

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


    @Override
    public void onResume() {
        super.onResume();
        int kod;
        if(!SectorCopyFragment.emptyBuffer){
            kod = 0xFF & SectorCopyFragment.sectorbuffer[1][0];
            kod <<= 8;
            kod |= 0xFF & SectorCopyFragment.sectorbuffer[1][1];
            kod <<= 8;
            kod |= 0xFF & SectorCopyFragment.sectorbuffer[1][2];
            kod <<= 8;
            kod |= 0xFF & SectorCopyFragment.sectorbuffer[1][3];
        }else{
            kod = 0x1234ABCD;
        }
        TextKod.setText(String.format("%08X", kod));
        ShowDef();
    }


    private void Recovery(){

        if(KeyTools.Busy){
            return;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor= db.query(
                Recovery.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );
        try{
            if(cursor.getCount() == 0){
                toast = Toast.makeText(this.getContext(), "Корзина пуста !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
        }finally {
            cursor.close();
        }
        recovery = new RECOVERY();
        if (!recovery.keytools.TestPort(sPort)) {
            toast = Toast.makeText(this.getContext(), writekey.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        recovery.execute();
        KeyTools.Busy = true;
    }


    private long getRecoveryKey(int uid ){

        long key;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] projection = {
                KeyAdress.COLUMN_CRYPTOKEY
        };
        Cursor cursor = db.query(
                Recovery.TABLE_NAME,   // таблица
                projection,            // столбцы
                Recovery.COLUMN_UID + " = " + uid ,
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки
        try {
            int keyColumnIndex = cursor.getColumnIndex(Recovery.COLUMN_CRYPTOKEY);
            if(!cursor.moveToFirst()){
                return -1;
            }
            key = cursor.getLong(keyColumnIndex);

        } finally {
            cursor.close();
        }
        return key;
    }


    private static void DeleteFromRecovery(int uid){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                Recovery.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);
        try{
            db.delete(
                    Recovery.TABLE_NAME,
                    Recovery.COLUMN_UID + "=" + uid,
                    null);
            if (0 == cursor.getCount()) {
                String SQL_CREATE_RECOVERY_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                        + 0 + " WHERE NAME='" + Recovery.TABLE_NAME + "'";
                db.execSQL(SQL_CREATE_RECOVERY_TABLE);
            }
        }finally {
            cursor.close();
        }
    }


    public static boolean AddToRecovery(int uid, long key){

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = db.query(
                Recovery.TABLE_NAME,
                null,
                Recovery.COLUMN_UID + " = " + uid,
                null,
                null,
                null,
                null);

        ContentValues values = new ContentValues();
        values.put(Recovery.COLUMN_UID, uid);
        values.put(Recovery.COLUMN_CRYPTOKEY, key);
        try{
            if(cursor.getCount() != 0) {
                DeleteFromRecovery(uid);
            }
            if(1 != db.insert(Recovery.TABLE_NAME, null, values)) {
                return false;
            }
        }finally {
            cursor.close();
        }

        return true;
    }


    private void WriteKey(){

        if(KeyTools.Busy){
            return;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                KeyAdress.TABLE_NAME,
                null,
                KeyAdress.COLUMN_KEYADRESS + "=" + AdressIndex,
                null,
                null,
                null,
                null
        );
        try{
            if(cursor.getCount() == 0){
                toast = Toast.makeText(this.getContext(), "База меток пуста !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
        }finally {
            cursor.close();
        }


        Integer kod;
        try {
            writekey = new WriteClassic();
            String s = TextKod.getText().toString();
            if(s.length() != 8){
                toast = Toast.makeText(this.getContext(), R.string.Ошибка_ввода_UID, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            kod = (int)Long.parseLong(s, 16);
        }catch(NumberFormatException e){
            toast = Toast.makeText(this.getContext(), getString(R.string.Ошибка_ввода) + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        if (!writekey.keytools.TestPort(sPort)) {
            toast = Toast.makeText(this.getContext(), writekey.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        writekey.execute(kod);
        KeyTools.Busy = true;
    }


    private void ReadKod() {
        if(KeyTools.Busy){
            return;
        }
        readuid = new UID();
        if (!readuid.keytools.TestPort(sPort)) {
            toast = Toast.makeText(this.getContext(), readuid.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        readuid.execute();
        KeyTools.Busy = true;

    }


    private void GrabKey(){

        if(KeyTools.Busy){
            return;
        }

        if(AdressIndex == 0){
            toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String SQL_QUERY = "SELECT "
                + UidKey.COLUMN_UID
                + " FROM "
                + UidKey.TABLE_NAME
                + " EXCEPT "
                + " SELECT "
                + KeyAdress.COLUMN_UID
                + " FROM "
                + KeyAdress.TABLE_NAME
                + " WHERE "
                + KeyAdress.COLUMN_KEYADRESS + "=" + AdressIndex;
        Cursor cursor = db.rawQuery(SQL_QUERY, null);
        try {

            if(cursor.getCount() == 0){
                toast = Toast.makeText(getContext(), "Все ключи для этого адреса рассчитаны!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            int uidColumnIndex = cursor.getColumnIndex(UidKey.COLUMN_UID);

            keygrab = new KEYGRAB(SettingsActivity.nsniff, cursor.getCount());
            int i= 0;
            while (cursor.moveToNext()) {
                keygrab.crd[i++].uid = cursor.getInt(uidColumnIndex);
            }
        }
        catch (RuntimeException e){
            toast = Toast.makeText(getContext(), "Ошибка !\n" +
                    e.toString(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.append(e.toString());
            return;
        }
        finally {
            cursor.close();
        }

        if (!keygrab.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), keygrab.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        KeyTools.Busy = true;
        keygrab.execute();
    }



    private void DeleteUid(int uid){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Делаем запрос
        Cursor cursor = db.query(
                UidKey.TABLE_NAME,   // таблица
                null,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки


        try {
            if(!cursor.moveToFirst()){
                toast = Toast.makeText(getContext(), "База меток пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            db.delete(
                    UidKey.TABLE_NAME,
                    UidKey.COLUMN_UID + "=" + uid,
                    null);
            db.delete(
                    KeyAdress.TABLE_NAME,
                    KeyAdress.COLUMN_UID + " = " + uid,
                    null);

            cursor = db.query(
                    UidKey.TABLE_NAME,   // таблица
                    null,            // столбцы
                    null,                  // столбцы для условия WHERE
                    null,                  // значения для условия WHERE
                    null,                  // Don't group the rows
                    null,                  // Don't filter by row groups
                    null);                   // порядок сортировки
            if(!cursor.moveToFirst()) {
                toast = Toast.makeText(getContext(), "База меток пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                String SQL_CREATE_UIDKEY_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                        + 0 + " WHERE NAME='" + UidKey.TABLE_NAME + "'";
                db.execSQL(SQL_CREATE_UIDKEY_TABLE);

                String SQL_CREATE_KEYADRESS_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                        + 0 + " WHERE NAME='" + KeyAdress.TABLE_NAME + "'";
                db.execSQL(SQL_CREATE_KEYADRESS_TABLE);
            }
        }catch (RuntimeException e) {
            toast = Toast.makeText(getContext(), "Ошибка !\n" +
                    e.toString(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.append(e.toString());
        }
            finally {
            // Всегда закрываем курсор после чтения
            cursor.close();
        }
        ShowDef();
    }


    private void DelAdress(){

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


    private void deleteStringAdress(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

            db.delete(
                    Adresses.TABLE_NAME,
                    "_id = " + AdressIndex,
                    null);
            db.delete(
                    KeyAdress.TABLE_NAME,
                    KeyAdress.COLUMN_KEYADRESS + " = " + AdressIndex,
                    null);
            Cursor cursor = db.query(
                    Adresses.TABLE_NAME,   // таблица
                    null,            // столбцы
                    null,                  // столбцы для условия WHERE
                    null,                  // значения для условия WHERE
                    null,                  // Don't group the rows
                    null,                  // Don't filter by row groups
                    null);                   // порядок сортировки
        try {
            int idColumnIndex = cursor.getColumnIndex(Adresses._ID);
            int adressColumnIndex = cursor.getColumnIndex(Adresses.COLUMN_ADRESS);
            if(!cursor.moveToFirst()){
                toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                TextAdress.setText("");
                AdressIndex = 0;
                String SQL_CREATE_ADRESSES_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                + 0 +" WHERE NAME='" + Adresses.TABLE_NAME +"'";
                db.execSQL(SQL_CREATE_ADRESSES_TABLE);
                String SQL_CREATE_KEYADRESS_TABLE = "UPDATE SQLITE_SEQUENCE SET SEQ="
                        + 0 +" WHERE NAME='" + KeyAdress.TABLE_NAME +"'";
                db.execSQL(SQL_CREATE_KEYADRESS_TABLE);
            }else{
                AdressIndex = cursor.getInt(idColumnIndex);
                s = cursor.getString(adressColumnIndex);
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
            cursor.close();
        }
    }


    private void SelectAdress(){

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                null,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                Adresses.COLUMN_ADRESS );                   // порядок сортировки
            AlertDialog.Builder adb = new AlertDialog.Builder(getContext())
                    .setTitle("Выберите адрес :")
                    .setCursor(cursor, myClickListener, Adresses.COLUMN_ADRESS);
            adb.show();
    }


    // обработчик нажатия на пункт списка диалога

    private DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    Adresses.TABLE_NAME,   // таблица
                    null,            // столбцы
                    null,                  // столбцы для условия WHERE
                    null,                  // значения для условия WHERE
                    null,                  // Don't group the rows
                    null,                  // Don't filter by row groups
                    Adresses.COLUMN_ADRESS );
            try{
                cursor.moveToPosition(which);
                int idColumnIndex = cursor.getColumnIndex(Adresses._ID);
                AdressIndex = cursor.getInt(idColumnIndex);
                ShowDef();
            }finally {
                cursor.close();
            }

        }
    };


    private void AddAdress(){
        final EditText txtAdr = new EditText(getContext());

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


    private void addAdrToBase(String s){

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
        AdressIndex = 0;    // Будет показан последний введенный адрес
        ShowDef();
    }


    public void ShowDef(){

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                UidKey._ID,
                UidKey.COLUMN_UID };
        Cursor cursor = db.query(
                UidKey.TABLE_NAME,   // таблица
                projection,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки

        try {
            s = "" + cursor.getCount();
            TextTagNumber.setText( s );
        } finally {
            cursor.close();
        }


        cursor = db.query(
                Adresses.TABLE_NAME,   // таблица
                null,            // столбцы
                null,                  // столбцы для условия WHERE
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки
        try {
            int idColumnIndex = cursor.getColumnIndex(Adresses._ID);
            int adressColumnIndex = cursor.getColumnIndex(Adresses.COLUMN_ADRESS);
            if(AdressIndex == 0){
                if(!cursor.moveToLast()){
                    toast = Toast.makeText(getContext(), "База адресов пуста !", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }else{
                    AdressIndex = cursor.getInt(idColumnIndex);
                    String s = cursor.getString(adressColumnIndex);
                    TextAdress.setText(s);
                }
            }else{
                cursor = db.query(
                        Adresses.TABLE_NAME,   // таблица
                        null,            // столбцы
                        Adresses._ID + "=" + AdressIndex, // столбцы для условия WHERE
                        null,                  // значения для условия WHERE
                        null,                  // Don't group the rows
                        null,                  // Don't filter by row groups
                        null);                   // порядок сортировки
                cursor.moveToFirst();
                String s = cursor.getString(adressColumnIndex);
                TextAdress.setText(s);
            }

        } finally {
            cursor.close();
        }

        cursor = db.query(
                KeyAdress.TABLE_NAME,
                null,
                KeyAdress.COLUMN_KEYADRESS + "=" + AdressIndex,
                null,
                null,
                null,
                null
        );
        try{
            s = "" + cursor.getCount();
            TextAdressTagNumber.setText(s);
        }finally {
            cursor.close();
        }

    }


    private void StartAddUid(){

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
        if (keygrab != null) {
            keygrab.cancel(true);
        }
        if ( writekey!= null) {
            writekey.cancel(true);
        }
        if ( readuid!= null) {
            readuid.cancel(true);
        }
        if ( recovery!= null) {
            recovery.cancel(true);
        }
    }


    private long getAdressKey(int uid, int adindex){

        long key;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] projection = {
                KeyAdress.COLUMN_CRYPTOKEY
        };
        Cursor cursor = db.query(
                KeyAdress.TABLE_NAME,   // таблица
                projection,            // столбцы
                KeyAdress.COLUMN_UID + " = " + uid + " AND " +
                        KeyAdress.COLUMN_KEYADRESS + " = " + adindex,
                null,                  // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null);                   // порядок сортировки
        try {
            int keyColumnIndex = cursor.getColumnIndex(KeyAdress.COLUMN_CRYPTOKEY);
            if(!cursor.moveToFirst()){
                return -1;
            }
            key = cursor.getLong(keyColumnIndex);

        } finally {
            cursor.close();
        }
        return key;
    }


    @SuppressLint("StaticFieldLeak")
    class KEYGRAB extends AsyncTask<Void, Integer, Integer>{

        KeyTools keytools;
        Crapto1 cr = new Crapto1();
        Crapto1.CraptoData[]  crd;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String  s;
        int numkey = 0;

        KEYGRAB(int n, int m){
            keytools = new KeyTools(n);
            crd = new Crapto1.CraptoData[m];
            for(int i = 0; i < m; i++){
                crd[i] = new Crapto1.CraptoData();
                crd[i].chal = new int[n];
                crd[i].rchal = new int[n];
                crd[i].rresp = new int[n];

            }

        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle(getString(R.string.Захват_данных_от_считывателя));
            pd.setMessage(getString(R.string.Поднесите_устройство_к_считывателю));
            pd.show();
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }


        @Override
        protected Integer doInBackground(Void... voids) {

            try{
                for(int i = 0; i < crd.length; i++){
                    keytools.uid = crd[i].uid;
                    for(int j = 0; j < keytools.nSniff; j++){   // Захват данных от считывателя
                        SystemClock.sleep(SettingsActivity.pause);
                        while(!keytools.getsniff(sPort,j)){
                            if (isCancelled()) {
                                return null;
                            }
                        }
                        crd[i].chal[j] = keytools.sn[j].TagChall[0];
                        crd[i].rchal[j] = keytools.sn[j].ReadChall[0];
                        crd[i].rresp[j] = keytools.sn[j].ReadResp[0];
                    }
                    if( i < crd.length - 1){
                        publishProgress(1, i );
                    }

                }
            } catch(IOException e1){
                try{
                    sPort.close();
                }catch(IOException e){}
                sPort = null;
                return -1;
            }
            ContentValues values = new ContentValues();
            for (int i = 0; i < crd.length; i++) {
                publishProgress(2, i);
                out:
                {
                    for (int j1 = 0; j1 < (keytools.nSniff - 1); j1++) {
                        for (int j2 = (j1 + 1); j2 < keytools.nSniff; j2++) {
                            if (cr.RecoveryKey(crd[i], j1, j2)) {
                                values.put(KeyAdress.COLUMN_UID, crd[i].uid);
                                values.put(KeyAdress.COLUMN_KEYADRESS, AdressIndex);
                                values.put(KeyAdress.COLUMN_CRYPTOKEY, crd[i].key);
                                long newRowId = db.insert(KeyAdress.TABLE_NAME, null, values);
                                numkey++;
                                break out;
                            }
                        }
                    }
                }
            }
            return 1;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            switch(values[0]){
                case 1:
                    pd.setTitle(getString(R.string.Захват_данных));
                    s = String.format(Locale.US,"%d - й",values[1] + 1);
                    pd.setMessage("Захвачены данные  " + "\n" + s + " метки из " + crd.length );
                    break;

                case 2:
                    pd.setTitle(getString(R.string.Расчет_ключей));
                    s = String.format(Locale.US,"%d - й",values[1] + 1);
                    pd.setMessage("Подождите - идет рассчет ключей" + "\n" + s + " ключ  из " + crd.length );
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
                    break;
            }
        }


        @Override
        protected void onPostExecute(Integer val) {
            super.onPostExecute(val);
            switch ( val ){
                case -1:
                    TextWin.append(getString(R.string.Ошибка_адаптера_Операция_прервана));
                    TextWin.append("\n" + "Ошибка адаптера! Операция прервана!");
                    toast = Toast.makeText(getContext(), "Ошибка адаптера! Операция прервана!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 1:
                    String s = " Удачно записано ключей - " + numkey + "\n из " + crd.length;
                    toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.append("\n" + s);
                    ShowDef();
                    break;
            }
            KeyTools.Busy = false;
            pd.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            String s = "\n" + getString(R.string.Операция_прервана);
            TextWin.append(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }
    }


    @SuppressLint("StaticFieldLeak")
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
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
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
                                publishProgress(2, ++ntag);
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


        @SuppressLint("DefaultLocale")
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
                    s = getString(R.string.Поднесите_заготовку_Classic_к_устройству) + "\n\n"
                            + String.format(" Добавлено меток - %d", values[1]);
                    pd.setMessage(s);
                    break;

                case 3:
                    toast = Toast.makeText(getContext(), "Сектор 0 закрыт !\n Эта метка не подходит !", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch (arg){
                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                default:
            }
            KeyTools.Busy = false;
            pd.dismiss();
        }


        @SuppressLint("DefaultLocale")
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
            ShowDef();
        }
    }


    @SuppressLint("StaticFieldLeak")
    class UID extends AsyncTask<Void, Void, Integer> {
        KeyTools keytools;


        UID(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle(getString(R.string.Считывание_UID));
            pd.setMessage(getString(R.string.Поднесите_оригинал_метки_к_устройству));
            pd.show();
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try{
                while(!keytools.readuid(sPort)){
                    if (isCancelled()) {
                        return null;
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
            return 1;
        }

        @Override
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch (arg){
                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 1:
                    toast = Toast.makeText(getContext(), R.string.UID_оригинала_считан, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    String s = "\n" + getString(R.string.UID_оригинала_считан);
                    TextWin.setText(s);
                    s = String.format("%08X", keytools.uid);
                    TextKod.setText(s);
                    break;
                default:
            }
            KeyTools.Busy = false;
            pd.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            String s = "\n" + getString(R.string.Операция_прервана);
            TextWin.setText(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class WriteClassic extends AsyncTask<Integer, Integer, Integer> {

        KeyTools keytools;
        byte[] blockBuffer = new byte[16];
        long crkey, defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB;
        String s;
        int err;


        WriteClassic(){
            keytools = new KeyTools(1);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle("Запись метки");
            pd.setMessage(getString(R.string.Поднесите_заготовку_Classic_к_устройству));
            pd.show();
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }





        @Override
        protected Integer doInBackground(Integer... kod) {
            int tagkod;

            try{
                block = 1;
                AB = 0;
                while(true){
                    while(!keytools.readuid(sPort)){        // Считывание UID
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    if(!keytools.authent(sPort,block, AB, defkey)){
                        publishProgress(0);
                    }else if((crkey = getAdressKey(keytools.uid, AdressIndex)) == -1 ){
                        publishProgress(1);
                    }else{
                        break;
                    }

                    while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }

                tagkod = kod[0];
                while (0 != (err = writesector(defkey, crkey, tagkod, keytools.uid))) {    // Запись данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(2, err);
                }


            }catch(IOException e1){
                try{
                    sPort.close();
                }catch(IOException e){
                }
                sPort = null;
                return -1;
            }

            return 1;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {

            super.onProgressUpdate(values);
            switch(values[0]){

                case 0:
                    toast = Toast.makeText(getContext(), "Сектор 0 закрыт" +
                            "\n" + getString(R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 1:
                    toast = Toast.makeText(getContext(), "Этой метки нет в базе" +
                            "\n" + getString(R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 2:
                    pd.setMessage(getString(R.string.Ошибка_записи) + " " + values[1]);
                    break;

                default:
                    break;
            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch(arg){
                case 1:
                    s = String.format(Locale.US,getString(R.string.Запись_успешно_завершена_KEY),crkey);
                    TextWin.append(s);
                    toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    DeleteUid(keytools.uid);
                    AddToRecovery(keytools.uid, crkey);
                    break;

                case -1:
                    TextWin.append(getString(R.string.Ошибка_адаптера_Операция_прервана));
                    TextWin.append("\n" + "Ошибка адаптера! Операция прервана!");
                    toast = Toast.makeText(getContext(), "Ошибка адаптера! Операция прервана!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                default:
                    break;
            }

            KeyTools.Busy = false;
            pd.dismiss();

        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            String s = "\n" + getString(R.string.Операция_прервана);
            TextWin.append(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }


        int writesector(long oldkey, long newkey, int kod, int uid) throws IOException{
            byte block = 1;
            byte AB = 0;
            if(!keytools.readuid(sPort)){
                return -1;
            }
            if (uid != keytools.uid) {
                publishProgress(5);
                while(keytools.readuid(sPort));
                return -1;
            }

            if(!keytools.authent(sPort, block, AB, oldkey)){
                return -2;
            }

            clrbuf(blockBuffer);
            KeyTools.IntToByteArray(kod, blockBuffer, 0);

            block = 1;
            if(!keytools.writeblock(sPort,block, blockBuffer)){
                return -3;
            }

            block = 3;
            if(!keytools.readblock(sPort, block, blockBuffer)){
                return -4;
            }
            KeyTools.KeyToByteArray(newkey, blockBuffer, 0);
            if(!keytools.writeblock(sPort, block, blockBuffer)){
                return -5;
            }

            block = 1;
            if(!keytools.readuid(sPort) || !keytools.authent(sPort, block, AB, newkey)
                    || !keytools.readblock(sPort, block, blockBuffer)){
                return -6;
            }
            return 0;
        }


        void clrbuf(byte[] buf){
            for(int i = 0; i < buf.length; i++){
                buf[i] = 0;             }
        }


    }


    @SuppressLint("StaticFieldLeak")
    private class RECOVERY extends AsyncTask<Void, Integer, Integer> {

        KeyTools keytools;
        byte[] blockBuffer = new byte[16];
        long crkey, defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB;
        String s;
        int err;


        RECOVERY(){
            keytools = new KeyTools(1);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle("Запись метки");
            pd.setMessage(getString(R.string.Поднесите_заготовку_Classic_к_устройству));
            pd.show();
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }





        @Override
        protected Integer doInBackground(Void... avoid) {

            try{
                block = 1;
                AB = 0;
                while(true){
                    while(!keytools.readuid(sPort)){        // Считывание UID
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    if((crkey = getRecoveryKey(keytools.uid)) == -1 ){
                        publishProgress(1);
                    }else{
                        break;
                    }

                    while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }

                while (0 != (err =(writesector(crkey, defkey, 0, keytools.uid)))) {    // Запись данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(2, err);
                }


            }catch(IOException e1){
                try{
                    sPort.close();
                }catch(IOException e){
                }
                sPort = null;
                return -1;
            }

            return 1;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {

            super.onProgressUpdate(values);
            switch(values[0]){

                case 0:
                    toast = Toast.makeText(getContext(), "Сектор 0 закрыт" +
                            "\n" + R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 1:
                    toast = Toast.makeText(getContext(), "Этой метки нет в корзине " +
                            "\n" + R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 2:
                    pd.setMessage(getString(R.string.Ошибка_записи) + " " + values[1] );
                    break;

                default:
                    break;
            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch(arg){
                case 1:
                    s = "Метка успешно восстановлена !";
                    TextWin.append(s);
                    toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    DeleteFromRecovery(keytools.uid);
                    break;

                case -1:
                    TextWin.append(getString(R.string.Ошибка_адаптера_Операция_прервана));
                    TextWin.append("\n" + "Ошибка адаптера! Операция прервана!");
                    toast = Toast.makeText(getContext(), "Ошибка адаптера! Операция прервана!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                default:
                    break;
            }

            KeyTools.Busy = false;
            pd.dismiss();

        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            String s = "\n" + getString(R.string.Операция_прервана);
            TextWin.append(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }


        int writesector(long oldkey, long newkey, int kod, int uid) throws IOException{
            byte block = 1;
            byte AB = 0;
            if(!keytools.readuid(sPort)){
                return -1;
            }

            if (uid != keytools.uid) {
                publishProgress(5);
                while(keytools.readuid(sPort));
                return -1;
            }

            if(!keytools.authent(sPort, block, AB, oldkey)){
                return -2;
            }

            clrbuf(blockBuffer);
            KeyTools.IntToByteArray(kod, blockBuffer, 0);

            block = 1;
            if(!keytools.writeblock(sPort,block, blockBuffer)){
                return -3;
            }

            block = 3;
            if(!keytools.readblock(sPort, block, blockBuffer)){
                return -4;
            }
            KeyTools.KeyToByteArray(newkey, blockBuffer, 0);
            if(!keytools.writeblock(sPort, block, blockBuffer)){
                return -5;
            }

            block = 1;
            if(!keytools.readuid(sPort) || !keytools.authent(sPort, block, AB, newkey)
                    || !keytools.readblock(sPort, block, blockBuffer)){
                return -6;
            }
            return 0;
        }


        void clrbuf(byte[] buf){
            for(int i = 0; i < buf.length; i++){
                buf[i] = 0;             }
        }


    }

}