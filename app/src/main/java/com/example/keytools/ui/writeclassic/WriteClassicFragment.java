package com.example.keytools.ui.writeclassic;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.R;
import com.example.keytools.SettingsActivity;
import com.example.keytools.ui.sectorcopy.SectorCopyFragment;

import java.io.IOException;
import java.util.Locale;

import static com.example.keytools.MainActivity.sPort;

public class WriteClassicFragment extends Fragment {

    private TextView TextWin;
    private EditText TextUID;
    private ProgressDialog pd;
    private Toast toast;

    private UID readuid;
    private WriteClassic writekey;
    private  volatile int smf;          // Семафор

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.fragment_writeclassic, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);

        TextUID = root.findViewById(R.id.TextUID);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID2:
                        ReadUID();
                        break;
                    case R.id.btnWrite:
                        WriteKey();
                        break;
                }
            }
        };

        Button btnReadUID2 = root.findViewById(R.id.btnReadUID2);
        Button btnWrite = root.findViewById(R.id.btnWrite);
        btnReadUID2.setOnClickListener(oclBtn);
        btnWrite.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.Отмена), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });

        pd.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                smf = 1;
            }
        });
        pd.setButton(Dialog.BUTTON_NEUTRAL, getString(R.string.Еще), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                smf = 2;
            }
        });
        pd.setTitle(getString(R.string.Захват_данных_от_считывателя));
        pd.setMessage(getString(R.string.Поднесите_устройство_к_считывателю));

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
        TextUID.setText(String.format("%08X", kod));
    }


    private void ReadUID(){

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


    private void WriteKey(){

        if(KeyTools.Busy){
            return;
        }

        Integer kod;
        try {
            writekey = new WriteClassic(SettingsActivity.nsniff);
            String s = TextUID.getText().toString();
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


    private void Cancel(){
        if (readuid != null) {
            readuid.cancel(true);
        }
        if (writekey != null) {
            writekey.cancel(true);
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class WriteClassic extends AsyncTask<Integer, Integer, Integer> {

        KeyTools keytools;
        KeyTools.CryptoKey[] Crk;
        byte[] blockBuffer = new byte[16];
        long crkey, defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB;
        String s;
        String[] StrKeyAB = {"Key A", "Key B"};
        int i;


        WriteClassic(int n){
            keytools = new KeyTools(n);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle(getString(R.string.Считывание_UID));
            pd.setMessage(getString(R.string.Поднесите_заготовку_Classic_к_устройству));
            pd.show();
            pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }


        @Override
        protected Integer doInBackground(Integer... kod) {
            int uid;
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
                    if(keytools.authent(sPort,block, AB, defkey)){
                        break;
                    }
                    publishProgress(0);
                    while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }

                uid  = keytools.uid;

                publishProgress(1);                 //Поднесите PN532 к считывателю
                for(i = 0; i < keytools.nSniff; i++){   // Захват данных от считывателя
                    SystemClock.sleep(SettingsActivity.pause);
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

                tagkod = kod[0];
                crkey = defkey;
                for(i = 0; i < Crk.length; i++){
                    publishProgress(6,0);  // Поднесите заготовку
                    if(!waittag(uid)){      // Ожидание метки
                        return null;
                    }
                    while( 0 != (writesector(crkey , Crk[i].key, tagkod, uid))){    // Запись данных 0-го сектора
                        if (isCancelled()) {
                            return null;
                        }
                        publishProgress(6,1);
                    }

                    publishProgress(7, i);
                    smf = 0;
                    while(smf == 0) {               // Ждем выбор диалога
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    crkey = Crk[i].key;
                    switch(smf){
                        case 1:             // ОК
                            return 1;

                        case 2:             // Стереть или Ещё
                            if(i == (Crk.length - 1)){      // Если ключ был последний, то стереть и выйти
                                tagkod = 0;
                                publishProgress(6,0);  // Поднесите заготовку
                                if(!waittag(uid)){      // Ожидание метки
                                    return null;
                                }
                                while( 0 != (writesector(crkey , defkey, tagkod, uid))){    // Запись данных 0-го сектора
                                    if (isCancelled()) {
                                        return null;
                                    }
                                    publishProgress(6,1);
                                }
                                return -2;
                            }
                    }
                }
                i--;
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
                    toast = Toast.makeText(getContext(), R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 1:
                    pd.setTitle(getString(R.string.Захват_данных));
                    pd.setMessage(getString(R.string.Поднесите_устройство_к_считывателю_1_я_попытка));
                    pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
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
                    pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                    break;

                case 4:
                    TextWin.setText(R.string.Результат_расчета_криптоключей);
                    if(keytools.sn[0].filter != 0){
                        TextWin.append(getString(R.string.Обнаружен_ФИЛЬТР_ОТР));
                    }
                    s = String.format(Locale.US, getString(R.string.UID_Найдено_ключей), keytools.uid, Crk.length);
                    TextWin.append(s);
                    for(int i = 0; i < Crk.length; i++){
                        TextWin.append(String.format(Locale.US,getString(R.string.Block_KEY),
                                Crk[i].block, StrKeyAB[Crk[i].AB], i, Crk[i].key));
                    }
                    break;

                case 5:
                    toast = Toast.makeText(getContext(), R.string.UID_не_совпадает_Попробуйте_другую_заготовку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 6:
                    pd.setTitle(getString(R.string.Запись_заготовки));
                    switch(values[1]){
                        case 0:
                            pd.setMessage(getString(R.string.Поднесите_заготовку_Mifare_Classic_к_устройству));
                            break;
                        case 1:
                            pd.setMessage(getString(R.string.Ошибка_записи));
                            break;
                    }
                    pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                    pd.show();
                    break;

                case 7:
                    pd.setTitle(getString(R.string.Проверка));
                    pd.setMessage(getString(R.string.Проверьте_записанную_метку));
                    if(values[1] < Crk.length - 1){
                        pd.getButton(Dialog.BUTTON_NEUTRAL).setText(R.string.Еще);
                    }else{
                        pd.getButton(Dialog.BUTTON_NEUTRAL).setText(R.string.Стереть);
                    }
                    pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
                    break;


            }
        }


        @Override
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch(arg){
                case 1:
                    s = String.format(Locale.US,getString(R.string.Запись_успешно_завершена_KEY),Crk[i].key);
                    TextWin.append(s);
                    break;

                case -1:
                    TextWin.append(getString(R.string.Ошибка_адаптера_Операция_прервана));
                    TextWin.append("\n" + "Ошибка адаптера! Операция прервана!");
                    toast = Toast.makeText(getContext(), "Ошибка адаптера! Операция прервана!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case -2:
                    TextWin.append(getString(R.string.Метка_стерта));
                    break;

                case -3:
                    s = "\n"+getString(R.string.Ключи_не_найдены_Попробуйте_увеличить_число_захватов);
                    TextWin.append(s);
                    toast = Toast.makeText(getContext(), getString(R.string.Ключи_не_найдены_Попробуйте_увеличить_число_захватов), Toast.LENGTH_LONG);
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
        protected void onCancelled(Integer arg) {
            super.onCancelled(arg);
            TextWin.append(getString(R.string.Операция_прервана));
            KeyTools.Busy = false;
            pd.dismiss();
        }


        boolean waittag(int uid) throws IOException {
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


        int writesector(long oldkey, long newkey, int kod, int uid) throws IOException{
            byte block = 1;
            byte AB = 0;
            crkey = oldkey;
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
            crkey = newkey;
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
            pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
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
                    TextUID.setText(s);
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

}