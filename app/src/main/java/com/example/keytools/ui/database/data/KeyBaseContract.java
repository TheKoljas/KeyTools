package com.example.keytools.ui.database.data;

import android.provider.BaseColumns;

public class KeyBaseContract {

    private KeyBaseContract(){
    }

    public static final class UidKey implements BaseColumns {

        public final static String TABLE_NAME = "uidkey";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_UID = "uid";
    }

    public static final class Adresses implements BaseColumns {

        public final static String TABLE_NAME = "adresses";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_ADRESS = "adress";
    }

    public static final class KeyAdress implements BaseColumns {

        public final static String TABLE_NAME = "keyadress";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_KEYADRESS = "keyadress";
        public final static String COLUMN_UID = "uid";
        public final static String COLUMN_CRYPTOKEY = "cryptokey";
    }

    public static final class Recovery implements BaseColumns {

        public final static String TABLE_NAME = "recovery";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_UID = "uid";
        public final static String COLUMN_CRYPTOKEY = "cryptokey";
    }

}
