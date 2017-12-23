package com.zhouwei.helloapt.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zhouwei.helloapt.bean.Student;

/**
 * Created by zhouwei on 2017/12/23.
 */

public class StudentDBOpenHelperDemo {
    private Context context;

    public StudentDBOpenHelperDemo(Context context) {
        this.context = context;
    }

    public void add(Student student) {
        StudentDBDemo studentDBDemo = new StudentDBDemo(context, "name");
        SQLiteDatabase helper = studentDBDemo.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("", "");
        helper.insert(student.getClass().getSimpleName(), null, values);
    }
}
