package com.example.keytools.ui.emulator;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.keytools.KeyTools;
import com.example.keytools.OpenFileDialog;
import com.example.keytools.R;
import com.example.keytools.ui.writeclassic.WriteClassicFragment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.keytools.MainActivity.sPort;


public class EmulatorFragment extends Fragment {

    private TextView TextWin;
    private TextView TextFileName;
    private static String FileName;
    private static byte[][] dump = new byte[64][16];
    private static boolean empty = true;
    private KeyTools keytools;
    private ProgressDialog pd;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_emulator, container, false);

        TextWin = root.findViewById(R.id.textWin);
        TextWin.setMovementMethod(new ScrollingMovementMethod());
        TextWin.setTextIsSelectable(true);
        TextFileName  = root.findViewById(R.id.textFileName);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.btnOpen:
                        FileOpen();
                        break;
                    case R.id.btnSave:
                        FileSave();
                        break;
                    case R.id.btnStart:
                        StartEmulator();
                        break;
                    case R.id.btnWriteEmul:
                        WriteEmul();
                        break;
                    case R.id.btnReadEmul:
                        ReadEmul();
                        break;
                }
            }
        };
        Button btnOpen = root.findViewById(R.id.btnOpen);
        Button btnSave = root.findViewById(R.id.btnSave);
        Button btnStart = root.findViewById(R.id.btnStart);
        Button btnWriteEmul = root.findViewById(R.id.btnWriteEmul);
        Button btnReadEmul = root.findViewById(R.id.btnReadEmul);

        btnOpen.setOnClickListener(oclBtn);
        btnSave.setOnClickListener(oclBtn);
        btnStart.setOnClickListener(oclBtn);
        btnWriteEmul.setOnClickListener(oclBtn);
        btnReadEmul.setOnClickListener(oclBtn);

        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.Отмена), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                StopEmulator();
            }
        });

        pd.setTitle("Идет эмуляция Mifare Classic ! ");
        pd.setMessage("Для окончания нажмите кнопку");

        return root;
    }


    void  WriteEmul(){

        if(KeyTools.Busy){
            return;
        }
        if(empty){
            Toast toast = Toast.makeText(this.getContext(), "Буфер дампа пуст!" , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        keytools = new KeyTools(1);
        if (!keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        try{
            for(byte i = 0; i < 64; i++){
                if(!keytools.writecard(sPort, i, dump[i])){
                    Toast toast = Toast.makeText(this.getContext(), "Ошибка чтения эмулятора" , Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }
            }
        }catch(IOException e1){
            try{
                sPort.close();
            }catch(IOException e){
            }
            sPort = null;
        }
        Toast toast = Toast.makeText(this.getContext(), "Дамп эмулятора записан успешно !" , Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }


    void ReadEmul(){
        if(KeyTools.Busy){
            return;
        }
        keytools = new KeyTools(1);
        if (!keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        try{
            for(byte i = 0; i < 64; i++){
                if(!keytools.readcard(sPort, i, dump[i])){
                    Toast toast = Toast.makeText(this.getContext(), "Ошибка чтения эмулятора" , Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }
            }
        }catch(IOException e1){
            try{
                sPort.close();
            }catch(IOException e){
            }
            sPort = null;
        }
        empty = false;
        FileName = "Дамп эмулятора :";
        TextFileName.setText("Дамп эмулятора :");
        KeyTools.PrintDump(dump, TextWin);
        Toast toast = Toast.makeText(this.getContext(), "Дамп эмулятора считан успешно !" , Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    private void StartEmulator(){
        if(KeyTools.Busy){
            return;
        }
        keytools = new KeyTools(1);
        if (!keytools.TestPort(sPort)) {
            Toast toast = Toast.makeText(this.getContext(), keytools.ErrMsg , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        try{
        if(!keytools.emulator(sPort)){
                Toast toast = Toast.makeText(this.getContext(), "Ошибка старта эмуляции" , Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }catch(IOException e1){
            try{
                sPort.close();
            }catch(IOException e){
            }
            sPort = null;
        }
        KeyTools.Busy = true;
        pd.show();
    }


    private void StopEmulator(){
        if(!KeyTools.Busy){
            return;
        }
//        keytools = new KeyTools(1);
//        if (!keytools.TestPort(sPort)) {
//            Toast toast = Toast.makeText(this.getContext(), keytools.ErrMsg , Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.CENTER, 0, 0);
//            toast.show();
//            return;
//        }
        try{
            if(!keytools.break_emulator(sPort)){
            Toast toast = Toast.makeText(this.getContext(), "Ошибка остановки эмуляции !" , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            }
        }catch(IOException e1){
            try{
                sPort.close();
            }catch(IOException e){
            }
            sPort = null;
        }
        KeyTools.Busy = false;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(!empty){
            TextFileName.setText(FileName);
            KeyTools.PrintDump(dump, TextWin);
        }
    }

    private void FileOpen(){

        OpenFileDialog fileDialog = new OpenFileDialog(getContext())
//                .setFilter("")
                .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                    @Override
                    public void OnSelectedFile(String fName) {
                        try{
                            File file = new File(fName);
                            if(file.length() != 1024){
                                Toast.makeText(getContext(), "Размер файла не равен 1024 байт !", Toast.LENGTH_LONG).show();
                                return;
                            }
                            DataInputStream dis = new DataInputStream(new FileInputStream(file));
                            for(int i = 0; i < 64; i++){
                                dis.read(dump[i]);
                            }
                            empty = false;
                            dis.close();
                            FileName = fName;
                            TextFileName.setText(fName);
                            KeyTools.PrintDump(dump, TextWin);
                        }
                        catch (IOException e)
                        {
                            Toast.makeText(getContext(), "Error\n " + e.toString(), Toast.LENGTH_LONG).show();
                        }

                    }
                });
        fileDialog.show();
    }

    private void FileSave(){
        File file = new File(getContext().getExternalFilesDir(null), "EmulatorDump.mfd");
        try
        {
            FileOutputStream fOut = new FileOutputStream(file);
            DataOutputStream myOutWriter = new DataOutputStream(fOut);
            for(int i = 0; i < 64; i++){
                myOutWriter.write(dump[i], 0, 16);
            }
            myOutWriter.flush();
            myOutWriter.close();
            fOut.close();
        }
        catch (IOException e)
        {
            Toast.makeText(getContext(), "Error\n " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }


}


