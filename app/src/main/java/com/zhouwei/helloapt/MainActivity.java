package com.zhouwei.helloapt;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

import com.cat.zeus.test_xxxx.TestXXXXX;
import com.zhouwei.AAA;
import com.zhouwei.ITest;
import com.zhouwei.ITest_Impl;
import com.zhouwei.TestAnno;
import com.zhouwei.Test_ABS;
import com.zhouwei.Test_ABS_Impl;

@TestAnno
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TestXXXXX.print();

        AAA.a();
        AAA aaa = new AAA();
        AAA.AAA_BBB b = aaa.new AAA_BBB();
        b.b();
        AAA.AAA_CCC.c();

        ITest iTest = new ITest_Impl();
        iTest.a_ITest();

        Test_ABS test_abs = new Test_ABS_Impl();
        test_abs.abs_a();
        test_abs.abs_b();
    }

    private void test(){
        Looper looper = getMainLooper();
        Handler handler = new Handler(looper);
        handler.post(new Runnable() {public void run() {}});

        String s = "Looper looper = getMainLooper();\nHandler handler = new Handler(looper);handler.post(new Runnable() {public void run() {}});";
    }
}
