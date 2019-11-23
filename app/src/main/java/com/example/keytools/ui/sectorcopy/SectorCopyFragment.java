package com.example.keytools.ui.sectorcopy;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.R;
import com.example.usbserial.util.HexDump;

import java.io.IOException;
import java.util.Locale;

import static com.example.keytools.MainActivity.sPort;

public class SectorCopyFragment extends Fragment {

    private TextView TextWin;
    Button btnReadSector;
    Button btnWriteSector;
    private EditText NumSniff;
    ProgressDialog pd;
    byte[][] sectorbuffer = new byte[4][16];
    boolean emptyBuffer = true;
    Toast toast;

    READSECTOR readsector;
    WRITESECTOR writesector;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sectorcopy, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        NumSniff = root.findViewById(R.id.NumSniff);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadSector:
                        ReadSector(v);
                        break;
                    case R.id.btnWriteSector:
                        WriteSector(v);
                        break;
                }
            }
        };
        btnReadSector = (Button)root.findViewById(R.id.btnReadSector);
        btnWriteSector = (Button)root.findViewById(R.id.btnWriteSector);
        btnReadSector.setOnClickListener(oclBtn);
        btnWriteSector.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.Отмена), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });
        return root;
    }


    void Cancel(){
        if (readsector != null) {
            readsector.cancel(true);
        }
        if (writesector != null) {
            writesector.cancel(true);
        }
    }


    void ReadSector(View v){

        if(KeyTools.Busy){
            return;
        }
        try {
            int nSniff = Integer.parseInt(NumSniff.getText().toString());
            if(nSniff < 2){
                Toast toast = Toast.makeText(this.getContext(), R.string.Ошибка_ввода_Число_захватов_меньше_2, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            readsector = new READSECTOR(nSniff);
        }catch(NumberFormatException e){
            Toast toast = Toast.makeText(this.getContext(), getString(R.string.Ошибка_ввода) + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        if (!readsector.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), readsector.keytools.ErrMsg , Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        readsector.execute();
        KeyTools.Busy = true;
    }


    void WriteSector(View v){
        if(emptyBuffer){
            toast = Toast.makeText(this.getContext(), "Буфер для записи пуст" + "!" , Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        if(KeyTools.Busy){
            return;
        }
        writesector = new WRITESECTOR();
        if (!writesector.keytools.TestPort(sPort)) {
            toast = Toast.makeText(this.getContext(), writesector.keytools.ErrMsg , Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        KeyTools.Busy = true;
        writesector.execute();
    }


    class WRITESECTOR extends AsyncTask<Void, Integer, Integer> {

        KeyTools keytools;
        byte block = 0, AB = 0;
        long crkey,defkey = 0xFFFFFFFFFFFFL;
        String s;
        Toast toast;
        int i;

        public WRITESECTOR() {
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle(getString(R.string.Запись_заготовки));
            pd.setMessage(getString(R.string.Поднесите_заготовку_ZERO_к_устройству));
            pd.show();
        }


        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                while (true) {                                 // Ждем ZERO
                    while (!keytools.readuid(sPort)) {        // Считывание UID
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    if (!keytools.authent(sPort, block, AB, defkey)) {
                        publishProgress(1);
                    }else{
                        keytools.readuid(sPort);
                        if (keytools.unlock(sPort)) {             // Попытка разблокировки
                            break;
                        }
                        publishProgress(2);
                    }
                    while (keytools.readuid(sPort)) {        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }
                while(!keytools.writeblock(sPort,block,sectorbuffer[0])){
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(3);
                }
                while (!keytools.readuid(sPort)) {        // Считывание UID
                    if (isCancelled()) {
                        return null;
                    }
                }
                if (!keytools.authent(sPort, block, AB, defkey)) {
                    return -2;
                }
                for(i = 1; i < 4; i++){
                    if(!keytools.writeblock(sPort, (byte)i, sectorbuffer[i])){
                        return -3;
                    }
                }

            } catch (IOException e1) {
                try {
                    sPort.close();
                } catch (IOException e) {
                }
                sPort = null;
                return -1;
            }

            return 1;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            Toast toast;
            super.onProgressUpdate(values);
            switch (values[0]) {

                case 1:
                    toast = Toast.makeText(getContext(), R.string.Ключ_сектора_неизвестен_попробуйте_другую_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 2:
                    toast = Toast.makeText(getContext(), R.string.Запись_блока_0_невозможна_попробуйте_другую_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 3:
                    pd.setMessage(getString(R.string.Ошибка_записи_сектора_0));
                    break;

                default:
                    break;

            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            Toast toast;
            super.onPostExecute(arg);
            switch (arg) {
                case 1:
                    TextWin.append("\n\n" + getString(R.string.Дамп_записан_успешно) + " :");
                    for (int i = 0; i < 4; i++) {
                        TextWin.append("\n" + KeyTools.BlockToString(sectorbuffer[i]));
                    }
                    break;

                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case -2:
                    TextWin.append("\n" + getString(R.string.Ошибка_аутентификации));

                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_аутентификации), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                default:
                    break;
            }
            keytools.Busy = false;
            pd.dismiss();
        }


        @Override
        protected void onCancelled(Integer arg) {
            super.onCancelled(arg);
            TextWin.append(getString(R.string.Операция_прервана));
            keytools.Busy = false;
            pd.dismiss();
        }
    }


    class READSECTOR extends AsyncTask<Void, Integer, Integer> {

        KeyTools keytools;
        KeyTools.CryptoKey[] Crk;
        long crkey, defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB = 0;
        String s;
        String StrKeyAB[] = {"Key A", "Key B"};
        int i;


        public  READSECTOR(int n){
            keytools = new KeyTools(n);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle(getString(R.string.Считывание_UID));
            pd.setMessage(getString(R.string.Поднесите_оригинал_метки_к_устройству));
            pd.show();
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }


        @Override
        protected Integer doInBackground(Void... voids) {
            int uid;
            int err, tagkod = 0;

            try{
                while(!keytools.readuid(sPort)){        // Считывание UID
                    if (isCancelled()) {
                        return null;
                    }
                }
                uid  = keytools.uid;

                if(keytools.authent(sPort, block, AB, defkey)){
                    crkey = defkey;
                }else{
                    publishProgress(1);                 //Поднесите PN532 к считывателю
                    for(i = 0; i < keytools.nSniff; i++){   // Захват данных от считывателя
                        while(!keytools.getsniff(sPort,i)){
                            if (isCancelled()) {
                                return null;
                            }
                        }
                        if(i < keytools.nSniff - 1){
                            publishProgress(2, (i + 1) );   //Подождите - идет захват!
                        }
                    }

                    publishProgress(3);     //Расчет ключей  Подождите ...
                    Crk = keytools.CulcKeys();      // Расчет ключей
                    if(Crk.length == 0){
                        return -3;
                    }
                    publishProgress(4);

                    waittag(uid);
                    crkey = 0;
                    for(i = 0; i < Crk.length; i++){
                        if(Crk[i].block != 1 || Crk[i].AB != 0){
                            continue;
                        }
                        if(keytools.readuid(sPort) && keytools.authent(sPort, block, AB, Crk[i].key)){
                            crkey = Crk[i].key;
                            break;
                        }
                    }
                    if(crkey == 0){
                        return -2;
                    }
                }

                for(i = 0; i < 4; i++){                             // Читаем сектор
                    while(!keytools.readblock(sPort, (byte)i, sectorbuffer[i])){
                        if (isCancelled()) {
                            return null;
                        }
                        publishProgress(6);
                    }
                }
                keytools.KeyToByteArray(crkey, sectorbuffer[3], 0);         // Добавляем ключ

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
            Toast toast;
            super.onProgressUpdate(values);
            switch(values[0]){

                case 1:
                    pd.setTitle(getString(R.string.Захват_данных));
                    pd.setMessage(getString(R.string.Поднесите_устройство_к_считывателю_1_я_попытка));
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                    break;

                case 2:
                    s = String.format(Locale.US,"%d - я",values[1] + 1);
                    pd.setMessage(getString(R.string.Подождите_идет_захват) + s + getString(R.string.попытка));
                    break;

                case 3:
                    pd.setTitle(getString(R.string.Расчет_ключей));
                    pd.setMessage(getString(R.string.Подождите));
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
                    break;

                case 4:
                    TextWin.setText(R.string.Результат_расчета_криптоключей);        // Вывод ключей
                    if(keytools.sn[0].filter != 0){
                        TextWin.append(getString(R.string.Обнаружен_ФИЛЬТР_ОТР));
                    }
                    s = String.format(Locale.US, getString(R.string.UID_Найдено_ключей), keytools.uid, Crk.length);
                    TextWin.append(s);
                    for(int i = 0; i < Crk.length; i++){
                        TextWin.append(String.format(Locale.US,getString(R.string.Block_KEY),
                                Crk[i].block, StrKeyAB[Crk[i].AB], i, Crk[i].key));
                    }
                    pd.setTitle(getString(R.string.Чтение_метки));      // Рассчет ключей
                    pd.setMessage(getString(R.string.Поднесите_оригинал_метки_к_устройству));
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                    break;

                case 5:
                    toast = Toast.makeText(getContext(), R.string.UID_не_совпадает_Попробуйте_другую_заготовку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 6:
                    pd.setTitle(getString(R.string.Чтение_метки));
                    pd.setMessage(getString(R.string.Ошибка_чтения));
                    break;

                default:
                    break;

            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            Toast toast;
            super.onPostExecute(arg);
            switch(arg){
                case 1:
                    TextWin.append("\n\n" + getString(R.string.Дамп_сектора) + ":");
                    for(int i = 0; i < 4; i++){
                        TextWin.append("\n" + KeyTools.BlockToString(sectorbuffer[i]));
                    }
                    emptyBuffer = false;
                    break;

                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case -2:
                    s = String.format(Locale.US,"\n"+getString(R.string.Найденные_ключи_не_подходят_к_этой_метке));
                    TextWin.append(s);
                    toast = Toast.makeText(getContext(), getString(R.string.Найденные_ключи_не_подходят_к_этой_метке), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                default:
                    break;
            }

            keytools.Busy = false;
            pd.dismiss();

        }


        @Override
        protected void onCancelled(Integer arg) {
            super.onCancelled(arg);
            TextWin.append(getString(R.string.Операция_прервана));
            keytools.Busy = false;
            pd.dismiss();
        }


        protected boolean waittag(int uid) throws IOException {
            while(true) {
                // Ждем метку
                while( !keytools.readuid(sPort)) {
                    if (isCancelled()) {
                        return false;
                    }
                }
                if (uid == keytools.uid) {
                    break;
                }
                publishProgress(5);     //UID не совпадает! Попробуйте другую заготовку!

                // Ждем пока уберут метку
                while( keytools.readuid(sPort)) {
                    if (isCancelled()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}