package com.example.keytools.ui.cloneuid;

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

import java.io.IOException;

import static com.example.keytools.MainActivity.sPort;

public class CloneUIDFragment extends Fragment {

    private TextView TextWin;
    private EditText TextUID;
    Button btnReadUID;
    Button btnCloneUID;
    Button btnUnbrick;
    ProgressDialog pd;


    UID  readuid;
    WriteUID writeuid;
    UNBRICK unbrick;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_cloneuid, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        TextUID = (EditText) root.findViewById(R.id.EditUID);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID:
                        ReadUID(v);
                        break;
                    case R.id.btnCloneUID:
                        WriteUID(v);
                        break;
                    case R.id.btnUnbrick:
                        Unbrick(v);
                        break;
                }
            }
        };
        btnReadUID = root.findViewById(R.id.btnReadUID);
        btnCloneUID = root.findViewById(R.id.btnCloneUID);
        btnUnbrick = root.findViewById(R.id.btnUnbrick);
        btnReadUID.setOnClickListener(oclBtn);
        btnCloneUID.setOnClickListener(oclBtn);
        btnUnbrick.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.wc6), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });

        return root;
    }


    public void ReadUID(View view){

        if(KeyTools.Busy){
            return;
        }
        readuid = new UID();
        if (!readuid.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), readuid.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        readuid.execute();
        KeyTools.Busy = true;
    }


    void WriteUID(View v){

        int uid;

        if(KeyTools.Busy){
            return;
        }

        try {
            writeuid = new WriteUID();
            String s = TextUID.getText().toString();
            if(s.length() != 8){
                Toast toast = Toast.makeText(this.getContext(), R.string.wc12, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            uid = (int)Long.parseLong(s, 16);
        }catch(NumberFormatException e){
            Toast toast = Toast.makeText(this.getContext(), getString(R.string.wc13) + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        if (!writeuid.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), writeuid.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        writeuid.execute(uid);
        KeyTools.Busy = true;
    }


    void Cancel(){
        if (readuid != null) {
            readuid.cancel(true);
        }
        if (writeuid != null) {
            writeuid.cancel(true);
        }
        if(unbrick != null){
            unbrick.cancel(true);
        }
    }


    void Unbrick(View v){

        if(KeyTools.Busy){
            return;
        }
        unbrick = new UNBRICK();
        if (!unbrick.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), unbrick.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        unbrick.execute();
        KeyTools.Busy = true;
    }


    class UNBRICK extends AsyncTask<Void, Integer, Integer> {
        KeyTools keytools;
        byte block = 0;
        byte[] blockbuffer = {0x21, (byte)0xCA, (byte)0xC3, 0x39, 0x11, 0x08, 0x04, 0x00,
                                    0x01, 0x4A, 0x73, 0x48, (byte)0xE7, 0x5E, 0x26, 0x1D};


        protected UNBRICK(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle("Восстановление ZERO");
            pd.setMessage("Поднесите метку");
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try{
                while(true){
                    if (isCancelled()) {
                        return null;
                    }
                    if(keytools.readuid(sPort)){
                        return 1;
                    }
                    if(!keytools.unlock(sPort)){
                        continue;
                    }
                    if(keytools.readuid(sPort)){
                        return 1;
                    }
                    if(!keytools.unlock(sPort)){
                        continue;
                    }
                    if(!keytools.writeblock(sPort, block, blockbuffer)) {
                        publishProgress(1);
                        continue;
                    }
                        if (keytools.readuid(sPort)){
                            return 2;
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
        }


        protected void onProgressUpdate(Integer... values) {
            Toast toast;
            super.onProgressUpdate(values);
            switch (values[0]) {
                case 1:
                    pd.setMessage("Ошибка записи блока");
                    break;

                default:
                    break;
            }
        }


        @Override
        protected void onPostExecute(Integer values) {
            super.onPostExecute(values);
            Toast toast;
            switch(values){
                case 1:
                    toast = Toast.makeText(getContext(), "Метка не требует восстановления", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.setText("\nМетка не требует восстановления");
                    break;
                case 2:
                    toast = Toast.makeText(getContext(), "Метка восстановлена", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.setText("\nМетка восстановлена");
                    break;
            }
            keytools.Busy = false;
            pd.dismiss();
        }

        protected void onCancelled() {
            super.onCancelled();
            Toast toast = Toast.makeText(getContext(), "Операция прервана", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.setText("\nОперация прервана");
            keytools.Busy = false;
            pd.dismiss();
        }
    }


    class WriteUID extends AsyncTask<Integer, Integer, Integer> {
        KeyTools keytools;
        byte[] blockBuffer = new byte[16];
        byte block;


        protected WriteUID() {
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle("Запись UID");
            pd.setMessage("Поднесите метку");
            pd.show();
        }

        protected Integer doInBackground(Integer... uid) {

            try{
                while(true){                                 // Ждем ZERO
                    while(!keytools.readuid(sPort)){        // Считывание UID
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    if(keytools.unlock(sPort)){             // Попытка разблокировки
                        break;
                    }
                    publishProgress(1);
                    while(keytools.readuid(sPort)){        // Ждем когда уберут метку
                        if (isCancelled()) {
                            return null;
                        }
                    }
                }
                block =0;
                if(!keytools.readblock(sPort,block,blockBuffer)){
                    return -1;
                }
                while( !keytools.readblock(sPort,block,blockBuffer)){    // Запись данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(2);
                }
                keytools.IntToByteArray(uid[0], blockBuffer, 0);
                blockBuffer[4] = 0;
                for(int i = 0; i < 4; i++){
                    blockBuffer[4] ^= blockBuffer[i];
                }
                while( !keytools.writeblock(sPort,block,blockBuffer)){    // Запись данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(3);
                }
                while(!keytools.readuid(sPort)){    // Запись данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(4);
                }
                if(keytools.uid != uid[0]){
                    return -2;
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

        protected void onProgressUpdate(Integer... values) {
            Toast toast;
            super.onProgressUpdate(values);
            switch (values[0]) {
                case 1:
                    toast = Toast.makeText(getContext(), R.string.wc16, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 2:
                    pd.setMessage("Ошибка чтения блока");
                    break;
                case 3:
                    pd.setMessage("Ошибка записи блока");
                    break;
                case 4:
                    pd.setMessage("Ошибка чтения UID");
                    break;

                    default:
                        break;
            }
        }

        protected void onPostExecute(Integer values) {
            Toast toast;
            super.onPostExecute(values);
            switch(values){
                case 1:
                    toast = Toast.makeText(getContext(), "UID записан", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.setText("\nUID записан");
                    break;
                case -1:
                    toast = Toast.makeText(getContext(), "Ошибка чтения", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.setText("\nОшибка чтения");
                    break;
                case -2:
                    toast = Toast.makeText(getContext(), "Ошибка записи", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    TextWin.setText("\nОшибка записи");
                    break;
            }
            keytools.Busy = false;
            pd.dismiss();
        }

        protected void onCancelled() {
            super.onCancelled();
            Toast toast = Toast.makeText(getContext(), "Операция прервана", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.setText("\nОперация прервана");
            keytools.Busy = false;
            pd.dismiss();
        }
    }


    class UID extends AsyncTask<Void, Void, Void> {
        KeyTools keytools;

        protected UID(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle("Считывание UID");
            pd.setMessage("Поднесите оригинал метки");
            pd.show();
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
            Toast toast = Toast.makeText(getContext(), "UID оригинала считан", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.setText("\nUID оригинала считан");
            String s = String.format("%X", keytools.uid);
            TextUID.setText(s);
            keytools.Busy = false;
            pd.dismiss();
        }

        protected void onCancelled() {
            super.onCancelled();
            Toast toast = Toast.makeText(getContext(), "Операция прервана", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextWin.setText("\nОперация прервана");
            keytools.Busy = false;
            pd.dismiss();
        }
    }
}