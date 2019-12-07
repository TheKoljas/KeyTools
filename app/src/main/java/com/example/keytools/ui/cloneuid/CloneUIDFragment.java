package com.example.keytools.ui.cloneuid;

import android.annotation.SuppressLint;
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
    private ProgressDialog pd;
    private Toast toast;

    private final byte[] blockzero = {0x21, (byte)0xCA, (byte)0xC3, 0x39, 0x11, 0x08, 0x04, 0x00,
            0x01, 0x4A, 0x73, 0x48, (byte)0xE7, 0x5E, 0x26, 0x1D};
    private final byte[] blockkey = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, 0x07,  (byte)0x80, 0x69,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
    private UID  readuid;
    private WriteUID writeuid;
    private UNBRICK unbrick;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root;
        root = inflater.inflate(R.layout.fragment_cloneuid, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        TextUID = root.findViewById(R.id.EditUID);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID:
                        ReadUID();
                        break;
                    case R.id.btnCloneUID:
                        WriteUID();
                        break;
                    case R.id.btnUnbrick:
                    case R.id.btnRestore:
                        Unbrick(v);
                        break;
                }
            }
        };
        Button btnReadUID = root.findViewById(R.id.btnReadUID);
        Button btnCloneUID = root.findViewById(R.id.btnCloneUID);
        Button btnUnbrick = root.findViewById(R.id.btnUnbrick);
        Button btnRestore = root.findViewById(R.id.btnRestore);
        btnReadUID.setOnClickListener(oclBtn);
        btnCloneUID.setOnClickListener(oclBtn);
        btnUnbrick.setOnClickListener(oclBtn);
        btnRestore.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.Отмена), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Cancel();
            }
        });

        return root;
    }


    private void ReadUID(){

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


    private void WriteUID(){

        int uid;

        if(KeyTools.Busy){
            return;
        }

        try {
            writeuid = new WriteUID();
            String s = TextUID.getText().toString();
            if(s.length() != 8){
                Toast toast = Toast.makeText(this.getContext(), R.string.Ошибка_ввода_UID, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            uid = (int)Long.parseLong(s, 16);
        }catch(NumberFormatException e){
            Toast toast = Toast.makeText(this.getContext(), getString(R.string.Ошибка_ввода) + e.toString() , Toast.LENGTH_LONG);
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


    private void Cancel(){
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


    private void Unbrick(View v){

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
        switch(v.getId()){

            case R.id.btnUnbrick:
                unbrick.execute(0);
                break;

            case R.id.btnRestore:
                unbrick.execute(1);
                break;
        }

        KeyTools.Busy = true;
    }


    @SuppressLint("StaticFieldLeak")
    class UNBRICK extends AsyncTask<Integer, Integer, Integer> {
        KeyTools keytools;
        int i;
        byte block = 0;


        UNBRICK(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle(getString(R.string.Восстановление_ZERO));
            pd.setMessage(getString(R.string.Поднесите_метку_ZERO_к_устройству));
            pd.show();
        }

        @Override
        protected Integer doInBackground(Integer... arg) {
            try{
                switch (arg[0]){
                    case 0:
                        publishProgress(0,1);
                        while (true) {
                            if (isCancelled()) {
                                return null;
                            }
                            if (keytools.readuid(sPort)) {
                                return 1;
                            }
                            if (!keytools.unlock(sPort)) {
                                continue;
                            }
                            if (keytools.readuid(sPort)) {
                                return 1;
                            }
                            if (!keytools.unlock(sPort)) {
                                continue;
                            }
                            if (!keytools.writeblock(sPort, block, blockzero)) {
                                publishProgress(1);
                                continue;
                            }
                            if (keytools.readuid(sPort)) {
                                return 2;
                            }
                        }

                    case 1:
                        publishProgress(0,2);
                        while (true) {
                            if (isCancelled()) {
                                return null;
                            }
                            if (!keytools.readuid(sPort)) {
                                continue;
                            }
                            if (!keytools.unlock(sPort)) {
                                publishProgress(2);
                                while(keytools.readuid(sPort)){
                                    if (isCancelled()) {
                                        return null;
                                    }
                                }
                                continue;
                            }
                            for (i = 0; i < 16; i++) {
                                while(!keytools.writeblock(sPort, (byte) (4 * i + 3), blockkey)) {
                                    publishProgress(1,1);
                                    if (isCancelled()) {
                                        return null;
                                    }
                                }
                                publishProgress(1,2);
                            }
                            return 3;
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
        protected void onProgressUpdate(Integer... values) {
            Toast toast;
            super.onProgressUpdate(values);
            switch (values[0]) {
                case 0:
                    switch (values[1]) {
                        case 1:
                            pd.setTitle(getString(R.string.Восстановление_блока_0_ZERO));
                            break;
                        case 2:
                            pd.setTitle(getString(R.string.Сброс_криптоключей_ZERO));
                            break;
                    }
                    break;

                case 1:
                    switch (values[1]) {
                        case 1:
                            pd.setMessage(getString(R.string.Ошибка_записи_блока));
                            break;
                        case 2:
                            pd.setMessage(getString(R.string.Блок_записан));
                            break;
                    }

                    break;

                case 2:
                    toast = Toast.makeText(getContext(), R.string.Метка_не_ZERO_Сброс_ключей_невозможен, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
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
                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 1:
                    toast = Toast.makeText(getContext(), R.string.Метка_не_требует_восстановления, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    String s  = "\n" + getString(R.string.Метка_не_требует_восстановления);
                    TextWin.setText(s);
                    break;

                case 2:
                    toast = Toast.makeText(getContext(), R.string.Метка_восстановлена, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    s  = "\n" + getString(R.string.Метка_восстановлена);
                    TextWin.setText(s);
                    break;

                case 3:
                    s = getString(R.string.Криптоключи_сброшены);
                    toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    s = "\n" + s;
                    TextWin.setText(s);
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
            TextWin.setText(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }
    }


    @SuppressLint("StaticFieldLeak")
    class WriteUID extends AsyncTask<Integer, Integer, Integer> {
        KeyTools keytools;
        byte[] blockBuffer = new byte[16];
        byte block;
        int i;


        WriteUID() {
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle(getString(R.string.Запись_UID));
            pd.setMessage(getString(R.string.Поднесите_метку_ZERO_к_устройству));
            pd.show();
        }

        @Override
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
                while( !keytools.readblock(sPort,block,blockBuffer)){    // Чтение данных 0-го сектора
                    if (isCancelled()) {
                        return null;
                    }
                    publishProgress(2);
                }
                for(i = 0; i < 16; i++){
                    if(blockBuffer[i] != 0x00){
                        break;
                    }
                }
                if( i == 16){  // Если блок 0 содержит только 0x00
                    System.arraycopy(blockzero, 0, blockBuffer, 0, 16);
                }
                KeyTools.IntToByteArray(uid[0], blockBuffer, 0);
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
            switch (values[0]) {
                case 1:
                    toast = Toast.makeText(getContext(), R.string.Запись_на_эту_метку_невозможна_Поменяйте_метку, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 2:
                    pd.setMessage(getString(R.string.Ошибка_чтения_блока));
                    break;
                case 3:
                    pd.setMessage(getString(R.string.Ошибка_записи_блока));
                    break;
                case 4:
                    pd.setMessage(getString(R.string.Ошибка_чтения_UID));
                    break;

                    default:
                        break;
            }
        }

        @Override
        protected void onPostExecute(Integer values) {
            super.onPostExecute(values);
            switch(values){
                case -3:
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_чтения), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    String s = "\n" + getString(R.string.Ошибка_чтения);
                    TextWin.setText(s);
                    break;
                case -2:
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_записи), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    s = "\n" + getString(R.string.Ошибка_записи);
                    TextWin.setText(s);
                    break;
                case -1:
                    TextWin.append("\n" + getString(R.string.Ошибка_адаптера_Операция_прервана));
                    toast = Toast.makeText(getContext(), getString(R.string.Ошибка_адаптера_Операция_прервана), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                case 1:
                    toast = Toast.makeText(getContext(), R.string.UID_записан, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    s = "\n" + getString(R.string.UID_записан);
                    TextWin.setText(s);
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
            TextWin.setText(s);
            KeyTools.Busy = false;
            pd.dismiss();
        }
    }


    @SuppressLint("StaticFieldLeak")
    class UID extends AsyncTask<Void, Void, Integer> {
        KeyTools keytools;

        UID() {
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle(getString(R.string.Считывание_UID));
            pd.setMessage(getString(R.string.Поднесите_оригинал_метки_к_устройству));
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                while (!keytools.readuid(sPort)) {
                    if (isCancelled()) {
                        return null;
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
        protected void onPostExecute(Integer arg) {
            super.onPostExecute(arg);
            switch (arg) {
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