package com.example.keytools.ui.keygrab;

import android.os.AsyncTask;
import android.os.Bundle;
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

import java.io.IOException;

import static com.example.keytools.MainActivity.sPort;

public class KeyGrabFragment extends Fragment {



//    private KeyGrabViewModel keyGrabViewModel;

    private TextView TextWin;
    private EditText TextEdit;
    private EditText NumSniff;
    private TextView TextBar;
    ProgressBar progressBar;
    Button btnReadUID;
    Button btnKeyGrab;
    Button btnCancel;
    String s;

    UID  readuid;
    KEY sniffkey;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
//        keyGrabViewModel = ViewModelProviders.of(this).get(KeyGrabViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_keygrab, container, false);

        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);

        TextWin = root.findViewById(R.id.textView4);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);

        TextBar = root.findViewById(R.id.TextBar);

        TextEdit = (EditText) root.findViewById(R.id.EditUID);
//        TextEdit.setText("1234ABCD");
        NumSniff = (EditText) root.findViewById(R.id.NumSniff);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnReadUID:
                        ReadUID(v);
                        break;
                    case R.id.btnKeyGrab:
                        ReadKey(v);
                        break;
                    case R.id.btnCancel:
                        Cancel(v);
                        break;
                }
            }
        };

        btnReadUID = (Button)root.findViewById(R.id.btnReadUID);
        btnKeyGrab = (Button)root.findViewById(R.id.btnKeyGrab);
        btnCancel = (Button)root.findViewById(R.id.btnCancel);
        btnReadUID.setOnClickListener(oclBtn);
        btnKeyGrab.setOnClickListener(oclBtn);
        btnCancel.setOnClickListener(oclBtn);

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
        KeyTools.Busy = true;
        readuid.execute();
    }

    public  void ReadKey(View view){

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
            sniffkey = new KEY(nSniff);
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


    void Cancel(View view) {
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

        public  KEY(int n){
            keytools = new KeyTools(n);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextBar.setText(getString(R.string.Захват_данных ) + " ...");
            TextWin.setText("");
            progressBar.setIndeterminate(true);
            progressBar.setProgress(0);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            TextEdit.setEnabled(false);
            NumSniff.setEnabled(false);
            btnReadUID.setEnabled(false);
            btnKeyGrab.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                for(int i = 0; i < keytools.nSniff; i++){
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
                    TextBar.setText(getString(R.string.Расчет_ключей) + " ...");
                    progressBar.setIndeterminate(true);
                    btnCancel.setEnabled(false);
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
            String StrKeyAB[] = {"Key A", "Key B"};
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
            NumSniff.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            TextBar.setText(R.string.Операция_прервана);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            NumSniff.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }
    }


    class UID extends AsyncTask<Void, Void, Void> {
        KeyTools keytools;

        public UID(){
            keytools = new KeyTools(1);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TextBar.setText("Поднесите метку");
            progressBar.setVisibility(ProgressBar.VISIBLE);
            TextEdit.setEnabled(false);
            NumSniff.setEnabled(false);
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
            TextBar.setText("UID считан");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            String s = String.format("%08X", keytools.uid);
            TextEdit.setText(s);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            NumSniff.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            TextBar.setText("Операция прервана");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
            TextEdit.setEnabled(true);
            NumSniff.setEnabled(true);
            btnReadUID.setEnabled(true);
            btnKeyGrab.setEnabled(true);
        }
    }


}