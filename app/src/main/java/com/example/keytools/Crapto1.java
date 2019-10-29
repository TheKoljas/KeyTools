package com.example.keytools;

public class Crapto1 {

    public int uid;
    public int chal;
    public int rchal;
    public int rresp;
    public int chal2;
    public int rchal2;
    public int rresp2;
    public long key;

    protected class Crypto1State {
        int odd, even;
    }

    byte filterlut[] = new byte[1 << 20];
    int LF_POLY_ODD = 0x29CE5C;
    int LF_POLY_EVEN = 0x870804;


    public boolean RecoveryKey() {

        Crypto1State s[];
        int  t;

        s = lfsr_recovery32(rresp ^ prng_successor(chal, 64), 0);

        for(t = 0; (s[t].odd != 0) | (s[t].even != 0); ++t) {
            lfsr_rollback_word(s, t, 0, 0);
            lfsr_rollback_word(s, t, rchal, 1);
            lfsr_rollback_word(s, t, uid ^ chal, 0);
            key = crypto1_get_lfsr(s, t, key);
            crypto1_word(s, t, uid ^ chal2, 0);
            crypto1_word(s, t, rchal2, 1);
            if (rresp2 == (crypto1_word(s, t, 0, 0) ^ prng_successor(chal2, 64))){
                return true;
            }
        }
        return false;
    }

    public Crapto1() {
        for(int i = 0; i < 1 << 20; ++i)
            filterlut[i] = (byte)filter(i);
    }


    int bit(int x, int n) {		//#define BIT(x, n) ((x) >> (n) & 1)
        return ((x >>> n) & 1);
    }


    int bebit(int x, int n) {		//#define BEBIT(x, n) BIT(x, (n) ^ 24)
        return ((x >>> (n^24)) & 1);
    }


    int filter(int x){
        int f;

        f  = 0xf22c0 >>> (x       & 0xf) & 16;
        f |= 0x6c9c0 >>> (x >>>  4 & 0xf) &  8;
        f |= 0x3c8b0 >>> (x >>>  8 & 0xf) &  4;
        f |= 0x1e458 >>> (x >>> 12 & 0xf) &  2;
        f |= 0x0d938 >>> (x >>> 16 & 0xf) &  1;
        return  bit(0xEC57E80A, f); //((0xEC57E80A) >>> (f) & 1); //BIT(0xEC57E80A, f);bit(0xEC57E80A, f);
    }


    int parity(int x)
    {
        x ^= x >>> 16;
        x ^= x >>> 8;
        x ^= x >>> 4;
        return bit(0x6996, x & 0xf); //((0x6996 >>> (x & 0xf)) & 1);		//BIT(0x6996, x & 0xf);
    }


    int swapendian(int x) { //(x = (x >> 8 & 0xff00ff) | (x & 0xff00ff) << 8, x = x >> 16 | x << 16)
        x = (x >>> 8 & 0xff00ff) | (x & 0xff00ff) << 8;
        x = x >>> 16 | x << 16;
        return x;
    }


    int prng_successor(int x, int n)
    {
        x = swapendian(x);
        while ((n--) > 0)
            x = x >>> 1 | (x >>> 16 ^ x >>> 18 ^ x >>> 19 ^ x >>> 21) << 31;

        return swapendian(x);
    }


    int extend_table_simple(int data[], int tbl, int end, int bit){

        for(data[ tbl ] <<= 1; tbl <= end; data[++tbl] <<= 1)
            if((filter(data[ tbl ]) ^ filter(data[ tbl ] | 1)) !=0 )
                data[ tbl ] |= filter(data[ tbl ]) ^ bit;
            else if(filter(data[ tbl ]) == bit) {
                data[ ++end ] = data[ ++tbl ];
                data[ tbl ] = data[ tbl - 1 ] | 1;
            } else
                data[ tbl-- ] = data[ end-- ];
        return end;
    }


    Crypto1State[] lfsr_recovery32(int ks2, int in) {
        Crypto1State statelist[] = new Crypto1State[1 << 18];
        int stl = 0;
        int odd[] = new int[1 << 21];
        int even[] = new int[1 << 21];
        int odd_head = 0, odd_tail = -1, oks = 0;
        int even_head = 0, even_tail = -1, eks = 0;
        int i;

        for(i = 0; i < (1 << 18); i++) {
            statelist[i] = new Crypto1State();
            statelist[i].odd = 0;
            statelist[i].even = 0;
        }

        for (i = 31; i >= 0; i -= 2)
            oks = oks << 1 | bebit(ks2, i);
        for (i = 30; i >= 0; i -= 2)
            eks = eks << 1 | bebit(ks2, i);

        statelist[stl].odd = statelist[stl].even = 0;

        for (i = 1 << 20; i >= 0; --i) {
            if (filter(i) == (oks & 1))
                odd[++odd_tail] = i; // *++odd_tail = i;
            if (filter(i) == (eks & 1))
                even[++even_tail] = i; // *++even_tail = i;
        }

        for (i = 0; i < 4; i++) {
            odd_tail = extend_table_simple(odd, odd_head, odd_tail, ((oks >>>= 1) & 1));
            even_tail = extend_table_simple(even, even_head, even_tail, ((eks >>>= 1) & 1));
        }

        in = (in >>> 16 & 0xff) | (in << 16) | (in & 0xff00);
        recover(odd, odd_head, odd_tail, oks, even, even_head, even_tail, eks, 11, statelist, 0, in << 1);

        return statelist;
    }


    int extend_table(int data[], int tbl, int end, int bit, int m1, int m2, int in) {

        in <<= 24;
        for (data[tbl] <<= 1; tbl <= end; data[++tbl] <<= 1)
            if ((filter(data[tbl]) ^ filter(data[tbl] | 1)) != 0) {
                data[tbl] |= filter(data[tbl]) ^ bit;
                update_contribution(data, tbl, m1, m2);
                data[tbl] ^= in;
            } else if (filter(data[tbl]) == bit) {
                data[++end] = data[tbl + 1];
                data[tbl + 1] = data[tbl] | 1;
                update_contribution(data, tbl, m1, m2);
                data[tbl++] ^= in;
                update_contribution(data, tbl, m1, m2);
                data[tbl] ^= in;
            } else
                data[tbl--] = data[end--];
        return end;
    }


    void update_contribution(int data[], int item, int mask1, int mask2) {
        int p = data[item] >>> 25;

        p = p << 1 | parity(data[item] & mask1);
        p = p << 1 | parity(data[item] & mask2);
        data[item] = p << 24 | (data[item] & 0xffffff);
    }


    void quicksort(int data[], int start, int stop){

        int it = start + 1, rit = stop, t;

        if(it > rit)
            return;

        while(it < rit)
            if( (data[it] ^ 0x80000000) <= (data[start] ^ 0x80000000) ) {
                ++it;
            }
            else if((data[rit] ^ 0x80000000) > (data[start] ^ 0x80000000)) {
                --rit;
            }
            else {
                t = data[it];
                data[it] = data[rit];
                data[rit] = t;
            }

        if((data[rit] ^ 0x80000000) >= (data[start] ^ 0x80000000)) {
            --rit;
        }
        if(rit != start) {
            t = data[rit];
            data[rit] = data[start];
            data[start] = t;
        }

        quicksort(data, start, rit - 1);
        quicksort(data, rit + 1, stop);
    }


    void quickSort(int[] array, int low, int high) {
        if (array.length == 0)
            return;// завершить выполнение если длина массива равна 0

        if (low >= high)
            return;// завершить выполнение если уже нечего делить

        // выбрать опорный элемент
        int middle = low + (high - low) / 2;
        int opora = array[middle];

        // разделить на подмассивы, который больше и меньше опорного элемента
        int i = low, j = high;
        while (i <= j) {
            while (array[i] < opora) {
                i++;
            }

            while (array[j] > opora) {
                j--;
            }

            if (i <= j) {// меняем местами
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                i++;
                j--;
            }
        }

        // вызов рекурсии для сортировки левой и правой части
        if (low < j)
            quickSort(array, low, j);

        if (high > i)
            quickSort(array, i, high);
    }


    int recover(int odd[], int o_head, int o_tail, int oks, int even[], int e_head, int e_tail, int eks, int rem,
                Crypto1State sl[], int s, int in) {

        int o, e, i;

        if (rem == -1) {
            for (e = e_head; e <= e_tail; ++e) {
                even[e] = even[e] << 1 ^ parity(even[e] & LF_POLY_EVEN) ^ (((in & 4) != 0) ? 1 : 0);
                for (o = o_head; o <= o_tail; ++o, ++s) {
                    sl[s].even = odd[o];
                    sl[s].odd = even[e] ^ parity(odd[o] & LF_POLY_ODD);
                    sl[s + 1].odd = sl[s + 1].even = 0;
                }
            }
            return s;
        }

        for (i = 0; (i < 4) && (rem-- != 0); i++) {
            oks >>>= 1;
            eks >>>= 1;
            in >>>= 2;
            o_tail = extend_table(odd, o_head, o_tail, oks & 1, LF_POLY_EVEN << 1 | 1, LF_POLY_ODD << 1, 0);
            if (o_head > o_tail)
                return s;

            e_tail = extend_table(even, e_head, e_tail, eks & 1, LF_POLY_ODD, LF_POLY_EVEN << 1 | 1, in & 3);
            if (e_head > e_tail)
                return s;
        }

        quicksort(odd, o_head, o_tail);
        quicksort(even, e_head, e_tail);

        while (o_tail >= o_head && e_tail >= e_head)
            if (((odd[o_tail] ^ even[e_tail]) >>> 24) == 0) {
                o_tail = binsearch(odd, o_head, o = o_tail);
                e_tail = binsearch(even, e_head, e = e_tail);
                s = recover(odd, o_tail--, o, oks, even, e_tail--, e, eks, rem, sl, s, in);
            } else if ((odd[o_tail] ^ 0x80000000) > (even[e_tail] ^ 0x80000000))
                o_tail = binsearch(odd, o_head, o_tail) - 1;
            else
                e_tail = binsearch(even, e_head, e_tail) - 1;

        return s;
    }


    int binsearch(int[] data, int start, int stop)
    {
        int mid, val =data[stop] & 0xff000000;
        while(start != stop) {
            mid = (stop - start) >> 1;
            if( (data[start + mid] ^ 0x80000000) > (val ^ 0x80000000))
                stop = start + mid;
            else
                start += mid + 1;
        }
        return start;
    }


    byte lfsr_rollback_bit(Crypto1State s[], int j, int in, int fb)
    {
        int out;
        byte ret;
        int t;

        s[j].odd &= 0xffffff;
        t = s[j].odd;
        s[j].odd = s[j].even;
        s[j].even = t;

        out = s[j].even & 1;
        out ^= LF_POLY_EVEN & (s[j].even >>= 1);
        out ^= LF_POLY_ODD & s[j].odd;
        out ^= (in != 0) ? 1 : 0;
        out ^= (ret = (byte)filter(s[j].odd)) & ((fb != 0) ? 1 : 0);

        s[j].even |= parity(out) << 23;
        return ret;
    }


    int lfsr_rollback_word( Crypto1State s[], int t, int in, int fb){

        int i;
        int ret = 0;
        for (i = 31; i >= 0; --i)
            ret |= lfsr_rollback_bit(s, t, bebit(in, i), fb) << (i ^ 24);
        return ret;
    }


    long crypto1_get_lfsr(Crypto1State state[], int t, long lfsr){

        int i;
        for(lfsr = 0, i = 23; i >= 0; --i) {
            lfsr = lfsr << 1 | bit(state[t].odd, i ^ 3);
            lfsr = lfsr << 1 | bit(state[t].even, i ^ 3);
        }
        return lfsr;
    }


    int crypto1_word(Crypto1State s[], int t, int in, int is_encrypted)
    {
        int i, ret = 0;

        for (i = 0; i < 32; ++i) {
            ret |= crypto1_bit(s, t, bebit(in, i), is_encrypted) << (i ^ 24);
        }

        return ret;
    }


    byte crypto1_bit(Crypto1State s[], int t, int in, int is_encrypted)
    {
        int feedin;
        byte ret = (byte)filter(s[t].odd);

        feedin  = ret & ((is_encrypted != 0) ? 1 : 0);
        feedin ^= ((in != 0) ? 1 : 0);
        feedin ^= LF_POLY_ODD & s[t].odd;
        feedin ^= LF_POLY_EVEN & s[t].even;
        s[t].even = s[t].even << 1 | parity(feedin);

        s[t].odd ^= s[t].even;
        s[t].even ^= s[t].odd;;
        s[t].odd ^= s[t].even;

        return ret;
    }


}
