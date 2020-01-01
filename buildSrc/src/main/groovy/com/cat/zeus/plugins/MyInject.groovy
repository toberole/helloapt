package com.cat.zeus.plugins

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

class MyInject {
    static ClassPool classPool = ClassPool.getDefault()

    static void injectOnCreate(String path, Project project) {
        classPool.appendClassPath(path)
        classPool.appendClassPath(project.android.bootClasspath[0].toString())
        classPool.importPackage("android.os.Bundle")

        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (file.getName().equals("MainActivity.class")) {
                    // 获取 MainActivity
                    CtClass ctClass = classPool.getCtClass("com.zhouwei.helloapt.MainActivity")
                    println("ctClass = " + ctClass)

                    // 解冻
                    if (ctClass.isFrozen()) {
                        ctClass.defrost()
                    }

                    // 获取到 onCreate() 方法
                    CtMethod ctMethod = ctClass.getDeclaredMethod("onCreate")
                    println("ctMethod = " + ctMethod)
                    // 插入日志打印代码
                    String insertBeforeStr = """android.util.Log.e("--->", "Hello Plugin Test");"""

                    ctMethod.insertBefore(insertBeforeStr)
                    ctClass.writeFile(path)
                    ctClass.detach()
                }
            }
        }
    }
}
