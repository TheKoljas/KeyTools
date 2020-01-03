package com.example.keytools;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.example.keytools.ui.database.DataBaseFragment;
import com.example.keytools.ui.database.data.KeyBaseDbHelper;
import com.example.usbserial.driver.UsbSerialDriver;
import com.example.usbserial.driver.UsbSerialPort;
import com.example.usbserial.driver.UsbSerialProber;

import android.view.Gravity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public Toolbar toolbar;

    public static UsbSerialPort sPort = null;

    private AppBarConfiguration mAppBarConfiguration;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_keygrab, R.id.nav_writeclassic, R.id.nav_cloneuid,
                R.id.nav_sectorcopy, R.id.nav_emulator, R.id.nav_database)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        SettingsActivity.LoadSettings(this);

        connect(null);
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
    }


    @Override
    public void onPause() {
        unregisterReceiver(usbReceiver);
        super.onPause();
    }


    private void connect(UsbDevice device){

        UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection;

        if(device == null) {
            List availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
            if (availableDrivers.isEmpty()) {
                Toast toast = Toast.makeText(this, R.string.Адаптер_не_обнаружен, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            UsbSerialDriver driver = (UsbSerialDriver) availableDrivers.get(0);
            device = driver.getDevice();
        }

        connection = usbManager.openDevice(device);
        if (connection == null) {
            if (!usbManager.hasPermission(device)) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, permissionIntent);
                return;
            }
            Toast toast = Toast.makeText(this, R.string.Адаптер_недоступен, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        sPort = UsbSerialProber.getDefaultProber().probeDevice(device).getPorts().get(0);
        try {
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            Toast toast = Toast.makeText(this, "Serial device: " + sPort.getClass().getSimpleName(), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } catch (IOException e) {
            Toast toast = Toast.makeText(this,"Ошибка " + e.toString(), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            sPort = null;
        }
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null) {
                            connect(device);
                        }
                    }
                    else {
                        Toast toast = Toast.makeText(context, "В доступе к адаптеру отказано!", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {

            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_connect:
                connect(null);
                return true;

            case R.id.action_exportbase:
                String in = this.getDatabasePath(KeyBaseDbHelper.DATABASE_NAME).toString();
                String out = this.getExternalFilesDir("databases").getPath() + "/" + KeyBaseDbHelper.DATABASE_NAME;
                File src = new File(in);
                File dst = new File(out);
                FileChannel inChannel = null;
                FileChannel outChannel =null;
                try {
                    inChannel = new FileInputStream(src).getChannel();
                    outChannel = new FileOutputStream(dst).getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                } catch (FileNotFoundException e) {
                    Toast toast = Toast.makeText(this, "Ошибка \n" + e.toString() , Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return false;
                }
                catch(IOException e1){
                    Toast toast = Toast.makeText(this, "Ошибка \n" + e1.toString() , Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return false;
                }
                finally {
                    try{
                        if (inChannel != null)
                            inChannel.close();
                        if (outChannel != null)
                            outChannel.close();
                    }catch(IOException e2){
                        Toast toast = Toast.makeText(this, "Ошибка \n" + e2.toString() , Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                Toast toast = Toast.makeText(this, "База данных экспортирована !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;

            case R.id.action_importbase:

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Импорт базы данных !")
                        .setMessage("Вы действительно хотите заменить текущую базу данных импортированной ?")
                        .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .setPositiveButton("Заменить", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ImportBase();
                            }
                        })
                        .show();
                return true;

            case R.id.action_showbase:

                intent = new Intent(this, ShowBaseActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void ImportBase(){

        Toast toast;

        String out = this.getDatabasePath(KeyBaseDbHelper.DATABASE_NAME).toString();
        String in = this.getExternalFilesDir("databases").getPath() + "/" + KeyBaseDbHelper.DATABASE_NAME;
        File src = new File(in);
        File dst = new File(out);
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dst).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (FileNotFoundException e) {
            toast = Toast.makeText(this, getString(R.string.Ошибка) + "\n" + e.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return ;
        }
        catch(IOException e1){
            toast = Toast.makeText(this, getString(R.string.Ошибка) + "\n" + e1.toString() , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return ;
        }
        finally {
            try{
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
            }catch(IOException e2){
                toast = Toast.makeText(this, getString(R.string.Ошибка) + "\n" + e2.toString() , Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
        toast = Toast.makeText(this, R.string.База_данных_импортирована, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        DataBaseFragment.AdressIndex = 0;

        Intent intent = new Intent(this, ResumeActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}
