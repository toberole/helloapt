package com.zhouwei.helloapt.util;

import android.app.Activity;

import com.zhouwei.InjectView;

import java.lang.reflect.Field;

/**
 * Created by zhouwei on 2017/12/22.
 * <p>
 * IOC
 */

public class InjectUtil {
    /**
     * 绑定View
     *
     * @param activity
     */
    public static void bindView(Activity activity) {
        Class clazz = activity.getClass();
        Field[] fields = clazz.getDeclaredFields();
        if (null != fields && fields.length > 0) {
            for (Field field : fields) {
                if (field.isAnnotationPresent(InjectView.class)) {
                    try {
                        InjectView injectView = field.getAnnotation(InjectView.class);
                        int id = injectView.value();
                        field.setAccessible(true);
                        field.set(activity, activity.findViewById(id));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
