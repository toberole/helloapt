package com.zhouwei;

import android.util.Log;

public class AAA {
    public static final String TAG = "xxxx-plugin";

    public static void a() {
        Log.i(TAG, "a ......");
    }

    public class AAA_BBB {
        public void b() {
            Log.i(TAG, "b ......");
        }
    }

    public static class AAA_CCC {
        public static void c() {
            Log.i(TAG, "c ......");
        }
    }

    public interface D {
        void d();
    }
}
