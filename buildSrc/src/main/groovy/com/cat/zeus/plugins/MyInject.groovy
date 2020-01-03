package com.cat.zeus.plugins

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

import org.gradle.api.Project

/**
 * 使用javassit操作字节码
 */
class MyInject {
    static ClassPool classPool = ClassPool.getDefault()

    static void injectOnCreate(String dirPath, Project project) {
        classPool.appendClassPath(dirPath)
        classPool.appendClassPath(project.android.bootClasspath[0].toString())
        classPool.importPackage("android.os.Bundle")

        File dir = new File(dirPath)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                if (file.getName().equals("MainActivity.class")) {
                    // 获取 MainActivity
                    CtClass ctClass = classPool.getCtClass("com.zhouwei.helloapt.MainActivity")
                    System.out.println("******** ctClass = " + ctClass)

                    // 解冻
                    if (ctClass.isFrozen()) {
                        ctClass.defrost()
                    }

                    // 获取到 onCreate() 方法
                    CtMethod ctMethod = ctClass.getDeclaredMethod("onCreate")
                    System.out.println("******** ctMethod = " + ctMethod)

                    // 插入日志打印代码
                    String insertBeforeStr = """android.util.Log.e("--->", "Hello Plugin Test");"""
                    ctMethod.insertBefore(insertBeforeStr)

                    ctClass.writeFile(dirPath)
                    ctClass.detach()
                }
            }
        }
    }
}

/**
 * 说明：
 *
 * 自定义的 Transform 在编译的时候并不会被触发执行，在安装 apk 时会触发执行；
 *
 * 自定义的 Transform 会自动生成几种不同 gradle task，任务名称规则为：transformClassWith$${getName}For${variant}*
 *
 * 双击上述自定义的 transform 任务会去执行 Transform 中的 transform() 方法，进行字节码操作代码。这一步可以看到我们再 groovy 中的打印日志，很方便调试。
 *
 * 在自定义的 MyTransform 中，使用 transform() 方法处理字节码，除了调用 MyInject 类的方法处理不同，其他的处理步骤都是统一的。
 *
 * transform() 处理步骤大致可以分为：1）对类型为目录的 input 遍历；2）调用 javassist api 处理字节码；3）生成输出路径，将操作后的 input 目录复制到 output 指定目录；4）对类型为 jar 的 input 遍历；5）重命名输出文件（防止复制文件冲突）；5）生成输出路径 & 将输入内容复制到输出。
 *
 */
