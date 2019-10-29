package com.example.keytools.ui.writeclassic;

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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.R;

import java.io.IOException;
import java.util.Locale;

//import static com.example.keytools.MainActivity.cr;
//import static com.example.keytools.MainActivity.keytools;
import static com.example.keytools.MainActivity.sPort;

public class WriteClassicFragment extends Fragment {

    private TextView TextWin;
    private EditText TextEdit;
    private EditText NumSniff;
    Button btnReadUID2;
    Button btnWrite;
    ProgressDialog pd;
    CheckBox uidCheckBox;


    UID2 readuid;
    WriteClassic writekey;
    Integer kod;
    protected volatile int smf;          // Семафор

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.fragment_writeclassic, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);

        TextEdit = (EditText) root.findViewById(R.id.TextBar);
        TextEdit.setText("1234ABCD");

        NumSniff = (EditText) root.findViewById(R.id.NumSniff2);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID2:
                        ReadUID2(v);
//                        Dial();
                        break;
                    case R.id.btnWrite:
                        WriteKey(v);
//                        Dial1();
                        break;
                }
            }
        };

        btnReadUID2 = (Button)root.findViewById(R.id.btnReadUID2);
        btnWrite = (Button)root.findViewById(R.id.btnWrite);
        btnReadUID2.setOnClickListener(oclBtn);
        btnWrite.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, "Отмена", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });

        pd.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                smf = 1;
            }
        });
        pd.setButton(Dialog.BUTTON_NEUTRAL, "Следующий", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                smf = 2;
            }
        });
        pd.setTitle("Захват данных от считывателя");
        pd.setMessage("Поднесите PN532 к считывателю");

        return root;
    }


    void Dial() {
        pd.setTitle("Захват данных от считывателя");
        pd.setMessage("Поднесите PN532 к считывателю");
        pd.show();
    }


    void Dial1() {
        pd.setTitle("Dial1");
        pd.setMessage("Поднесите PN532 к считывателю");
        pd.show();
    }


    public void ReadUID2(View view){

        if(KeyTools.Busy){
            return;
        }
        readuid = new UID2();
        if (!readuid.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), readuid.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        readuid.execute();
        KeyTools.Busy = true;
    }


    void WriteKey(View v){

        if(KeyTools.Busy){
            return;
        }

        try {
            int nSniff = Integer.parseInt(NumSniff.getText().toString());
            if(nSniff < 2){
                Toast toast = Toast.makeText(this.getContext(), "Ошибка ввода !\n" +
                        "Число захватов меньше 2 !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            writekey = new WriteClassic(nSniff);
            String s = TextEdit.getText().toString();
            if(s.length() != 8){
                Toast toast = Toast.makeText(this.getContext(), "Ошибка ввода UID !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            kod = (int)Long.parseLong(s, 16);
        }catch(NumberFormatException e){
            Toast toast = Toast.makeText(this.getContext(), "Ошибка ввода" + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        if (!writekey.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), writekey.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        writekey.execute(kod);
        KeyTools.Busy = true;
    }


    void Cancel(){
        if (readuid != null) {
            readuid.cancel(true);
        }
        if (writekey != null) {
            writekey.cancel(true);
        }
    }


    class WriteClassic extends AsyncTask<Integer, Integer, Integer> {

        KeyTools keytools;
        KeyTools.CryptoKey[] Crk;
        byte[] blockBuffer = new byte[16];
        long crkey, defkey = 0xFFFFFFFFFFFFL;
        byte block = 1;
        byte AB;
        String s;
        String StrKeyAB[] = {"Key A", "Key B"};


        public  WriteClassic(int n){
            keytools = new KeyTools(n);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextWin.setText("");
            pd.setTitle("Считывание UID");
            pd.setMessage("Поднесите заготовку Classic\nк PN532");
            pd.show();
            pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }


        @Override
        protected Integer doInBackground(Integer... kod) {
            int uid;
            int err, tagkod = 0;

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
                    publishProgress(0, 1);
                    while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }

                uid  = keytools.uid;

                publishProgress(1);                 //Поднесите PN532 к считывателю
                for(int i = 0; i < keytools.nSniff; i++){   // Захват данных от считывателя
                    while(!keytools.getsniff(sPort,i)){
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    publishProgress(2, (i + 1) );   //Подождите - идет захват!
                }

                publishProgress(3);     //Расчет ключей  Подождите ...
                Crk = keytools.CulcKeys();      // Расчет ключей
                if(Crk.length == 0){
                    return -3;
                }
                publishProgress(4);

                tagkod = kod[0];
                long k1 = defkey;
                for(int i = 0; i < Crk.length; i++){
                    publishProgress(6,0);  // Поднесите заготовку
                    if(!waittag(uid)){      // Ожидание метки
                        return null;
                    }
                    while( 0 != (err = writesector(k1 , Crk[i].key, tagkod))){    // Запись данных 0-го сектора
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
                    k1 = Crk[i].key;
                    switch(smf){
                        case 1:             // ОК
                            return 1;

                        case 2:
                            if(i < (Crk.length - 1)){      // Следующий
                                continue;
                            }else{                          // Стереть
                                tagkod = 0;
                                publishProgress(6,0);  // Поднесите заготовку
                                if(!waittag(uid)){      // Ожидание метки
                                    return null;
                                }
                                while( 0 != (err = writesector(k1 , defkey, tagkod))){    // Запись данных 0-го сектора
                                    if (isCancelled()) {
                                        return null;
                                    }
                                    publishProgress(6,1);
                                }
                                return -8;
                            }
                    }
                }
            }catch(IOException e1){
                //this.cancel(true);
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
                    switch(values[1]){
                        case 1:
                            Toast toast = Toast.makeText(getContext(), "Запись на эту метку невозможна!\n" +
                                    "Поменяйте метку!", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            break;

                        default:
                    }



                    break;

                case 1:
                    pd.setTitle(" Захват данных ");
                    pd.setMessage("Поднесите PN532 к считывателю\n 1 - я попытка");
                    break;

                case 2:
                    s = String.format(Locale.US,"\n %d - я",values[1] + 1);
                    pd.setMessage("Подождите - идет захват!" + s + " попытка");
                    break;

                case 3:
                    pd.setTitle("Расчет ключей");
                    pd.setMessage("Подождите ...");
                    break;

                case 4:
                    TextWin.setText("Результат расчета криптоключей :");
                    if(keytools.sn[0].filter != 0){
                        TextWin.append("\nОбнаружен ФИЛЬТР ОТР !");
                    }
                    s = String.format(Locale.US, "\nUID %08X\nНайдено ключей - %d :", keytools.uid, Crk.length);
                    TextWin.append(s);
                    for(int i = 0; i < Crk.length; i++){
                        TextWin.append(String.format(Locale.US,"\nBlock %d   %s" +
                                "\nKEY %d = %012X",Crk[i].block, StrKeyAB[Crk[i].AB], i, Crk[i].key));
                    }
                    break;

                case 5:
                    Toast toast = Toast.makeText(getContext(), "UID не совпадает!\nПопробуйте другую заготовку!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case 6:
                    pd.setTitle("Запись заготовки");
                    switch(values[1]){
                        case 0:
                            pd.setMessage("Поднесите заготовку\nMifare Classic к PN532");
                            break;
                        case 1:
                            pd.setMessage("Ошибка записи");
                            break;
                    }
                    pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                    pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                    pd.show();
                    break;

                case 7:
                    pd.setTitle("Проверка ");
                    pd.setMessage("Проверьте записанную метку");
                    if(values[1] < Crk.length - 1){
                        pd.getButton(Dialog.BUTTON_NEUTRAL).setText("Еще");
                    }else{
                        pd.getButton(Dialog.BUTTON_NEUTRAL).setText("Стереть");
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
                    TextWin.append("\nЗапись успешно завершена.");
                    break;

                case -1:
                    TextWin.append("\nОшибка адаптера! Операция прервана");
                    break;

                case -2:
                    TextWin.append("\nОбнаружен ФИЛЬТР ОТР !\nЗапись на Classic невозможна!");
                    break;

                case -3:
                    TextWin.append("\nОшибка расчета ключа! Попробуйте ещё разок.");
                    break;

                case -4:
                    TextWin.append("\nНесколько Карт Объекта!\nЗапись на Classic невозможна!");
                    break;

                case -5:
                    s = String.format(Locale.US,"\nНеудачная аутентификация \nBlock %d  %s " +
                            "\nКриптоключ  %012X",block, StrKeyAB[AB], crkey);
                    TextWin.append(s);
                    break;

                case -6:
                    TextWin.append("\nОшибка записи блока " + String.format(Locale.US,"%d",block));
                    break;

                case -7:
                    s = String.format(Locale.US,"\nНеудачная верификация блока %d ключом %012X",block, crkey);
                    TextWin.append(s);
                    break;


                case -8:
                    s = String.format(Locale.US,"\nМетка стерта");
                    TextWin.append(s);
                    break;


                case -9:
                    s = String.format(Locale.US,"\nNeutral");
                    TextWin.append(s);
                    break;

            }

            keytools.Busy = false;
            pd.dismiss();

        }


        @Override
        protected void onCancelled(Integer arg) {
            super.onCancelled(arg);
            TextWin.append("\nОперация прервана");
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


        protected int writesector(long oldkey, long newkey, int kod) throws IOException{
            byte block = 1;
            byte AB = 0;
            crkey = oldkey;
            if(!keytools.readuid(sPort)){
                return -1;
            }
            if(!keytools.authent(sPort, block, AB, oldkey)){
                return -2;
            }

            clrbuf(blockBuffer);
            keytools.IntToByteArray(kod, blockBuffer, 0);

            block = 1;
            if(!keytools.writeblock(sPort,block, blockBuffer)){
                return -3;
            }

            block = 3;
            if(!keytools.readblock(sPort, block, blockBuffer)){
                return -4;
            }
            keytools.KeyToByteArray(newkey, blockBuffer, 0);
            if(!keytools.writeblock(sPort, block, blockBuffer)){
                return -5;
            }

            block = 1;
            crkey = newkey;
            if(!keytools.readuid(sPort) && !keytools.authent(sPort, block, AB, newkey)
                    && !keytools.readblock(sPort, block, blockBuffer)){
                return -6;
            }
            return 0;
        }


        protected void clrbuf(byte[] buf){
            for(int i = 0; i < buf.length; i++){
                buf[i] = 0;             }
        }


    }



    class UID2 extends AsyncTask<Void, Void, Void> {
        KeyTools keytools;

        protected UID2(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle("Считывание UID");
            pd.setMessage("Поднесите оригинал ключа");
            pd.show();
            pd.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
            pd.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                while(!keytools.readuid(sPort)){
                    if (isCancelled()) {
                        return null;
                    }
                }
            }catch(IOException e1){
                this.cancel(true);
                try{
                    sPort.close();
                }catch(IOException e){
                }
                sPort = null;
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            TextWin.append("\nUID оригинала считан");
            String s = String.format("%X", keytools.uid);
            TextEdit.setText(s);
            keytools.Busy = false;
            pd.dismiss();
        }

        protected void onCancelled() {
            super.onCancelled();
            TextWin.append("\nОперация прервана");
            keytools.Busy = false;
            pd.dismiss();
        }
    }

}