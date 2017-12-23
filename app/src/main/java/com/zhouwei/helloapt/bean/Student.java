package com.zhouwei.helloapt.bean;

import com.zhouwei.AutoIncrement;
import com.zhouwei.DB;
import com.zhouwei.Primarykey;
import com.zhouwei.Property;

/**
 * Created by zhouwei on 2017/12/23.
 */

@DB
public class Student {
    @AutoIncrement
    private int id;

    @Property(type = "String")
    private String name;

    @Property(type = "String")
    private String gender;

    @Property(type = "int")
    private int age;
}
