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
        TextEdit.setText("1234ABCD");
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

        Button btnReadUID = (Button)root.findViewById(R.id.btnReadUID);
        Button btnKeyGrab = (Button)root.findViewById(R.id.btnKeyGrab);
        Button btnCancel = (Button)root.findViewById(R.id.btnCancel);
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

        protected void onPreExecute() {
            super.onPreExecute();
            TextBar.setText("Захват данных ...");
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

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


        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch(values[0]){
                case -1:
                    TextBar.setText("Рассчет ключей ...");
                    progressBar.setIndeterminate(true);
                    break;

                case -2:
                    break;

                default:
                    if(values[0] > 0){
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(values[0]);
                    }
            }
        }

        protected void onPostExecute(Void aVoid) {
            String s;
            String StrKeyAB[] = {"Key A", "Key B"};
            super.onPostExecute(aVoid);
            TextBar.setText("Рассчет окончен");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            s = String.format("nSniff = %d ",keytools.nSniff);
            TextWin.setText(s);
            if(keytools.sn[0].filter != 0){
                TextWin.append("\n\rОбнаружен ФИЛЬТР ОТР !");
            }
            s = String.format("\n\rUID2 %08X\n\rНайдено ключей - %d :", keytools.uid, Crk.length);
            TextWin.append(s);
            for(int i = 0; i < Crk.length; i++){
                s = String.format("\nBlock %d  %s\nKEY %d = %012X", Crk[i].block, StrKeyAB[Crk[i].AB], i, Crk[i].key);
                TextWin.append(s);
            }
            keytools.Busy = false;
        }

        protected void onCancelled() {
            super.onCancelled();
            TextBar.setText("Операция прервана");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
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
            TextBar.setText("UID2 считан");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            String s = String.format("%X", keytools.uid);
            TextEdit.setText(s);
            keytools.Busy = false;
        }

        protected void onCancelled() {
            super.onCancelled();
            TextBar.setText("Операция прервана");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            keytools.Busy = false;
        }
    }


}