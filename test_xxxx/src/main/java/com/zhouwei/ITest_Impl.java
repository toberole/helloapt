package com.zhouwei;

import android.util.Log;

public class ITest_Impl implements ITest {
    public static final String TAG = "xxxx-plugin";

    @Override
    public void a_ITest() {
        Log.i(TAG, "ITest_Impl#a_ITest ......");
    }
}
