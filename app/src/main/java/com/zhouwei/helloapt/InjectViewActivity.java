package com.zhouwei.helloapt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.zhouwei.InjectView;
import com.zhouwei.helloapt.util.InjectUtil;

public class InjectViewActivity extends AppCompatActivity {

    @InjectView(R.id.btn)
    private Button btn;// 反射注入类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inject_view);

        InjectUtil.bindView(this);
    }
}
