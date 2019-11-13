package com.example.keytools;

import java.io.IOException;

import com.example.usbserial.driver.UsbSerialPort;


public class KeyTools {

    public int nSniff;                //Число попыток аутентификации для рассчета ключа
    final static byte CMD = (byte) 0xA5;
    final static byte GETINFO = 0x02;
    final static byte GETUID = 0x04;
    final static byte GETSNIFF = 0x06;
    final static byte AUTHENT = 0x08;
    final static byte READBLOCK = 0x0A;
    final static byte WRITEBLOCK = 0x0C;
    final static byte UNLOCK = 0x0E;

    public static boolean Busy;                        // Семафор занятости

    byte[] buffer = new byte[128];
    byte[] info = new byte[4];
    public int uid;
//    public long[] criptokey;
//    public byte[] keyerror;

    public Sniff[] sn;
    public String ErrMsg;
    public byte error;


    public static class CryptoKey{
        public long key;
        public byte block;
        public byte AB;
    }


    public static class Sniff {
        public byte filter;
        public byte nkey;
        public byte[] keyAB;
        public byte[] blockNumber;
        public int[] TagChall;
        public int[] ReadChall;
        public int[] ReadResp;
    }

    public KeyTools(int nsn) {
        nSniff = nsn;
        sn = new Sniff[nSniff];
        for (int i = 0; i < nSniff; i++) {
            sn[i] = new Sniff();
        }
    }


    public CryptoKey[] CulcKeys(){
        Crapto1 cr = new Crapto1();
        CryptoKey[] Crk, crk1;
        int n = sn[0].nkey *(nSniff - 1)*nSniff / 2;    //Максимально возможное число ключей
        Crk = new CryptoKey[n];
        int jcrk = 0;
        cr.uid = uid;

        for (int i = 0; i < sn[0].nkey; i++) {
            for (int j1 = 0; j1 < (nSniff - 1); j1++) {
                for (int j2 = (j1 + 1); j2 < nSniff; j2++) {
                    if((sn[j1].blockNumber[i] == sn[j2].blockNumber[i]) &&
                            (sn[j1].keyAB[i] == sn[j2].keyAB[i])){
                        cr.chal = sn[j1].TagChall[i];
                        cr.rchal = sn[j1].ReadChall[i];
                        cr.rresp = sn[j1].ReadResp[i];

                        cr.chal2 = sn[j2].TagChall[i];
                        cr.rchal2 = sn[j2].ReadChall[i];
                        cr.rresp2 = sn[j2].ReadResp[i];
                        cr.key = -1L;
                        if (cr.RecoveryKey()) {
                            if(NoDuble (Crk, jcrk, cr.key, sn[j1].blockNumber[i],
                                    sn[j1].keyAB[i])){
                                Crk[jcrk] = new CryptoKey();
                                Crk[jcrk].key = cr.key;
                                Crk[jcrk].block = sn[j1].blockNumber[i];
                                Crk[jcrk].AB = sn[j1].keyAB[i];
                                jcrk++;
                            }
                        }
                    }
                }
            }
        }
        crk1 = new CryptoKey[jcrk];
        for(int i = 0; i < jcrk; i ++){
            crk1[i] = Crk[i];
        }
        return crk1;
    }


    protected boolean NoDuble(KeyTools.CryptoKey[] crk, int jcrk, long key, byte block, byte AB){
        for(int i = 0; i < jcrk; i++){
            if((crk[i].key == key ) &&
                    (crk[i].block == block) &&
                    (crk[i].AB == AB)){
                return false;
            }
        }
        return true;
    }


    public int ByteArrayToInt(byte b[], int n) {
        int s = 0;
        s = 0xFF & b[n + 3];
        s <<= 8;
        s |= 0xFF & b[n + 2];
        s <<= 8;
        s |= 0xFF & b[n + 1];
        s <<= 8;
        s |= 0xFF & b[n];
        return s;
    }


    public void IntToByteArray(int d, byte[] b, int n){
        b[n + 3] = (byte)(d & 0xFF);
        d >>>= 8;
        b[n + 2] = (byte)(d & 0xFF);
        d >>>= 8;
        b[n + 1] = (byte)(d & 0xFF);
        d >>>= 8;
        b[n] = (byte)(d & 0xFF);
    }


    public void KeyToByteArray(long autent_key, byte[] writebuffer, int offset){
        writebuffer[offset + 5] = (byte)(0xFF & autent_key);
        autent_key >>>= 8;
        writebuffer[offset + 4] = (byte)(0xFF & autent_key);
        autent_key >>>= 8;
        writebuffer[offset + 3] = (byte)(0xFF & autent_key);
        autent_key >>>= 8;
        writebuffer[offset + 2] = (byte)(0xFF & autent_key);
        autent_key >>>= 8;
        writebuffer[offset + 1] = (byte)(0xFF & autent_key);
        autent_key >>>= 8;
        writebuffer[offset] = (byte)(0xFF & autent_key);
    }


    public boolean TestPort(UsbSerialPort sPort) {
        if (sPort == null) {
            ErrMsg = "Ошибка адаптера - порт не открыт!";
            return false;
        }
        try {
            if (!GetInfo(sPort)) {
                ErrMsg = "Ошибка устройства STM32 + PN532";
                return false;
            }
        } catch (IOException e) {
            ErrMsg = "Ошибка адаптера: " + e.toString();
            return false;
        }
        return true;
    }


    boolean SerialRead(UsbSerialPort sPort, byte[] buffer, int timeout)throws IOException {
        long start = System.currentTimeMillis();
        byte[] b = new byte[128];
        int a1 = 0, a2 = 0, i;
        buffer[2] = 127;
        do{
            a1 = sPort.read(b, timeout);
            for(i = 0; i < a1; i++){
                buffer[i + a2] = b[i];
            }
            a2 += a1;
            if( a2 >= buffer[2]) {
                return true;
            }
        }while((System.currentTimeMillis() - start) < timeout);
        return false;
    }


    boolean GetInfo(UsbSerialPort sPort) throws IOException {

        int lentgh = 9;
        byte[] writebuffer = new byte[3];
        writebuffer[0] = (byte) CMD;
        writebuffer[1] = GETINFO;
        writebuffer[2] = 3;
        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[0] != (CMD + 1)) || (buffer[1] != (GETINFO + 1)) || (buffer[2] != lentgh)) {
            error = buffer[3];
            return false;
        }
        for (int i = 0; i < 4; i++) {
            info[i] = buffer[i + 3];
        }
        return true;
    }


    public boolean readuid(UsbSerialPort sPort) throws IOException {

        final int n = 3;
        int l, lentgh = 7;
        byte writebuffer[] = new byte[n];
        writebuffer[0] = CMD;
        writebuffer[1] = GETUID;
        writebuffer[2] = n;

        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[2] != lentgh) || (buffer[0] != (CMD + 1)) || (buffer[1] != (GETUID + 1))) {
            error = buffer[3];
            return false;
        }
        uid = ByteArrayToInt(buffer, 3);

        return true;
    }


    public boolean getsniff(UsbSerialPort sPort, int jsn) throws IOException {

        final int n = 7;
        byte writebuffer[] = new byte[n];
        writebuffer[0] = CMD;
        writebuffer[1] = GETSNIFF;
        writebuffer[2] = n;

        writebuffer[3] = (byte) ((uid >>> 24) & 0xFF);
        writebuffer[4] = (byte) ((uid >>> 16) & 0xFF);
        writebuffer[5] = (byte) ((uid >>> 8) & 0xFF);
        writebuffer[6] = (byte) (uid & 0xFF);

        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[0] != (CMD + 1)) || (buffer[1] != (GETSNIFF + 1)) || (buffer[4] == 0)){
            error = buffer[3];
            return false;
        }
        sn[jsn].filter = buffer[3];
        sn[jsn].nkey = buffer[4];


        sn[jsn].keyAB = new byte[sn[jsn].nkey];
        sn[jsn].blockNumber = new byte[sn[jsn].nkey];
        sn[jsn].TagChall = new int[sn[jsn].nkey];
        sn[jsn].ReadChall = new int[sn[jsn].nkey];
        sn[jsn].ReadResp = new int[sn[jsn].nkey];

        int a = 5;
        for (int i = 0; i < sn[jsn].nkey; i++) {
            sn[jsn].keyAB[i] = buffer[a++];
            sn[jsn].blockNumber[i] = buffer[a++];
            sn[jsn].TagChall[i] = ByteArrayToInt(buffer, a);
            a += 4;
            sn[jsn].ReadChall[i] = ByteArrayToInt(buffer, a);
            a += 4;
            sn[jsn].ReadResp[i] = ByteArrayToInt(buffer, a);
            a += 4;
        }
        return true;
    }


    public boolean authent(UsbSerialPort sPort, byte block, byte keyAB, long autent_key) throws IOException {
        final int n = 11;    //Длина запроса
        int lentgh = 3;         // Длина верного ответа
        byte writebuffer[] = new byte[n];

        writebuffer[0] = CMD;
        writebuffer[1] = AUTHENT;
        writebuffer[2] = n;
        writebuffer[3] = block;
        writebuffer[4] = keyAB;
        KeyToByteArray(autent_key,writebuffer,5);
        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[2] != lentgh) || (buffer[0] != (CMD + 1)) || (buffer[1] != (AUTHENT + 1))) {
            error = buffer[3];
            return false;
        }

        return true;
    }


    public boolean readblock(UsbSerialPort sPort, byte block, byte[] data) throws IOException{
        final int n = 4;    //Длина запроса
        int lentgh = 19;         // Длина верного ответа
        byte writebuffer[] = new byte[n];

        writebuffer[0] = CMD;
        writebuffer[1] = READBLOCK;
        writebuffer[2] = n;
        writebuffer[3] = block;

        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[2] != lentgh) || (buffer[0] != (CMD + 1)) || (buffer[1] != (READBLOCK + 1))) {
            error = buffer[3];
            return false;
        }
        for(int i = 0; i < 16; i++){
            data[ i ] = buffer [ i + 3];
        }
        return true;
    }


    public boolean writeblock(UsbSerialPort sPort, byte block, byte[] data) throws IOException{
        final int n = 20;    //Длина запроса
        int lentgh = 3;         // Длина верного ответа
        byte writebuffer[] = new byte[n];

        writebuffer[0] = CMD;
        writebuffer[1] = WRITEBLOCK;
        writebuffer[2] = n;
        writebuffer[3] = block;
        for(int i = 0; i < 16; i++){
            writebuffer[ i + 4 ] = data[ i ];
        }

        sPort.write(writebuffer, 500);
        if(!SerialRead(sPort,buffer,2000)){
            return false;
        }
        if ((buffer[2] != lentgh) || (buffer[0] != (CMD + 1)) || (buffer[1] != (WRITEBLOCK + 1))) {
            error = buffer[3];
            return false;
        }

        return true;
    }


}