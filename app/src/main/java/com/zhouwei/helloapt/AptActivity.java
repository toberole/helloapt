package com.zhouwei.helloapt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.zhouwei.DIActivity;
import com.zhouwei.DIView;


@DIActivity
public class AptActivity extends AppCompatActivity {

    @DIView(R.id.tv)
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
