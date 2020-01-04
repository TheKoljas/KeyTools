package com.example.keytools.ui.keygrab;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.R;
import com.example.keytools.SettingsActivity;

import java.io.IOException;

import static com.example.keytools.MainActivity.sPort;

public class KeyGrabFragment extends Fragment {

    private TextView TextWin;
    private EditText TextEdit;
    private TextView TextBar;
    private ProgressBar progressBar;
    private Button btnReadUID;
    private Button btnKeyGrab;
    private Button btnCancel;

    private UID  readuid;
    private KEY sniffkey;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root;
        root = inflater.inflate(R.layout.fragment_keygrab, container, false);

        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);

        TextBar = root.findViewById(R.id.TextUID);

        TextEdit = root.findViewById(R.id.EditUID);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID:
                        ReadUID();
                        break;
                    case R.id.btnKeyGrab:
                        ReadKey();
                        break;
                    case R.id.btnCancel:
                        Cancel();
                        break;
                }
            }
        };

        btnReadUID = root.findViewById(R.id.btnReadUID);
        btnKeyGrab = root.findViewById(R.id.btnKeyGrab);
        btnCancel = root.findViewById(R.id.btnCancel);
        btnReadUID.setOnClickListener(oclBtn);
        btnKeyGrab.setOnClickListener(oclBtn);
        btnCancel.setOnClickListener(oclBtn);

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
        KeyTools.Busy = true;
        readuid.execute();
    }

    private void ReadKey(){

        if(KeyTools.Busy){
            return;
        }
        try {
            sniffkey = new KEY(SettingsActivity.nsniff);
            sniffkey.keytools.uid = (int)Long.parseLong(TextEdit.getText().toString(), 16);
        }catch(NumberFormatException e){
            Toast toast = Toast.makeText(this.getContext(), "Ошибка ввода UID !" + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        if (!sniffkey.keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), sniffkey.keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        KeyTools.Busy = true;
        sniffkey.execute();
    }


    private void Cancel() {

        if (readuid != null) {
            readuid.cancel(true);
        }
        if (sniffkey != null) {
            sniffkey.cancel(true);
        }
    }


    class KEY extends AsyncTask<Void, Integer, Void> {

        KeyTools keytools;
        KeyTools.CryptoKey[] Crk;

        KEY(int n){
            keytools = new KeyTools(n);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String s = getString(R.string.Захват_данных ) + " ...";
            TextBar.setText(s);
            TextWin.setText("");
            progressBar.setIndeterminate(true);
            progressBar.setProgress(0);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            TextEdit.setEnabled(false);
            btnReadUID.setEnabled(false);
            btnKeyGrab.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                for(int i = 0; i < keytools.nSniff; i++){
                    SystemClock.sleep(SettingsActivity.pause);
                    while(!keytools.getsniff(sPort,i)){
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    publishProgress((i+1)*100/keytools.nSniff);
                }
                publishProgress(100);
            }catch(IOException e1){
                this.cancel(true);
                try{
                    sPort.close();
                }catch(IOException e){
                }
                sPort = null;
                return null;
            }

            publishProgress(-1);
            Crk = keytools.CulcKeys();
            return null;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch(values[0]){
                case -1:
                    String s = getString(R.string.Расчет_ключей) + " ...";
                    TextBar.setText(s);
                    progressBar.setIndeterminate(true);
                    btnCancel.setEnabled(false);
//                    for(int i = 0; i < keytools.nSniff; i++){
//                        for(int j = 0; j < keytools.sn[i].nkey; j++){
//                            s = String.format("\ni = %d j = %d Block = %d AB = %d",i, j, keytools.sn[i].blockNumber[j], keytools.sn[i].keyAB[j]);
//                            TextWin.append(s);
//                        }
//
//                    }
                    break;

                case -2:
                    btnCancel.setEnabled(true);
                    break;

                default:
                    btnCancel.setEnabled(true);
                    if(values[0] > 0){
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(values[0]);
                    }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            String s;
            String[] StrKeyAB = {"Key A", "Key B"};
            super.onPostExecute(aVoid);
            TextBar.setText(R.string.Рассчет_окончен);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            if(keytools.sn[0].filter != 0){
                TextWin.append(getString(R.string.Обнаружен_ФИЛЬТР_ОТР));
            }
            s = String.format(getString(R.string.UID_Найдено_ключей), keytools.uid, Crk.length);
            TextWin.append(s);
            for(int i = 0; i < Crk.length; i++){
                s = String.format(getString(R.string.Block_KEY), Crk[i].block, StrKeyAB[Crk[i].AB], i, Crk[i].key);
                TextWin.append(s);
            }
            keytools.Busy = false;
            btnCancel.setEnabled(true);
            TextEdit.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextBar.setText(R.string.Операция_прервана);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }
    }


    class UID extends AsyncTask<Void, Void, Void> {
        KeyTools keytools;

        UID(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextBar.setText(R.string.Поднесите_метку);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            TextEdit.setEnabled(false);
            btnReadUID.setEnabled(false);
            btnKeyGrab.setEnabled(false);
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
            TextBar.setText(R.string.UID_считан);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            String s = String.format("%08X", keytools.uid);
            TextEdit.setText(s);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast toast = Toast.makeText(getContext(), getString(R.string.Операция_прервана), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            TextBar.setText(R.string.Операция_прервана);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        Cancel();
    }

}