package com.zhouwei.helloapt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zhouwei.Test;


@Test("hello apt")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
