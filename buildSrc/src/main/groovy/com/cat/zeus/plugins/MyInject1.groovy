package com.cat.zeus.plugins

import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod

class MyInject1 {

    private static ClassPool pool = ClassPool.getDefault()
    //注意这里需要替换你的anroid.jar路径
    static String androidJar = "D:\\Application\\Android\\sdkUpDate\\platforms\\android-24\\android.jar"
    static String myPackageName = "com.zjw.appmethodtime"
    static String myCostTimeAnnotation = "@com.zjw.appmethodtime.ExtraInfo"
    static String CostTime = "ExtraInfo"

    public static void injectDir(String path, String packageName, boolean enabeCostTime) {
        pool.appendClassPath(path)
        // 以下为windows环境下你的相应android.jar路径
        pool.insertClassPath(androidJar)

        println("enabeCostTime is " + enabeCostTime)

        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                //确保当前文件是class文件，并且不是系统自动生成的class文件以及注解文件
                if (filePath.endsWith(".class")
                        && !filePath.contains('R$')
                        && !filePath.contains('R.class')
                        && !filePath.contains("BuildConfig.class")
                        && !filePath.contains("ExtraInfo")
                ) {
                    // 判断当前目录是否是在我们的应用包里面
                    int index = filePath.indexOf(packageName);
                    boolean isMyPackage = index != -1;
                    if (isMyPackage) {
                        int end = filePath.length() - 6 // .class = 6
                        String className = filePath.substring(index, end).replace('\\', '.').replace('/', '.')
                        //开始修改class文件
                        CtClass c = pool.getCtClass(className)

                        if (c.isFrozen()) {
                            c.defrost()
                        }
                        pool.importPackage(myPackageName)

                        //给类添加计时变量
                        CtField startTime = new CtField(CtClass.longType, "sStart", c);
                        startTime.setModifiers(Modifier.STATIC);
                        c.addField(startTime);

                        //给类添加计时变量
                        CtField endTime = new CtField(CtClass.longType, "sEnd", c);
                        endTime.setModifiers(Modifier.STATIC);
                        c.addField(endTime);

                        //遍历类的所有方法
                        CtMethod[] methods = c.getDeclaredMethods();
                        for (CtMethod method : methods) {
                            println("method ====" + method.longName)
                            if (enabeCostTime
                                    && method.getAvailableAnnotations() != null
                                    && method.getAvailableAnnotations().length >= 1
                                    && "${method.getAvailableAnnotations()[0]}".contains(CostTime)
                            ) {
                                insertCostTimeCode(method)
                            } else if (!enabeCostTime) {
                                insertCostTimeCode(method)
                            }
                        }//END   for (CtMethod method : methods)
                        c.writeFile(path)
                        c.detach()
                    }//END if(isMyPackage)
                }
            }
        }
    }

    private static void insertCostTimeCode(CtMethod method) {
        //插入到函数第一句
        StringBuilder startInjectStr = new StringBuilder();
        startInjectStr.append("sStart = System.nanoTime();");
        print("方法第一句插入了：" + startInjectStr.toString() + "语句\n")
        method.insertBefore(startInjectStr.toString())

        //插入到函数最后一句
        StringBuilder endInjectStr = new StringBuilder();
        endInjectStr.append("sEnd = System.nanoTime();\n");
        endInjectStr.append("android.util.Log.e(\"AppMethodTime\",");
        endInjectStr.append("\"" + method.longName + "\"");
        endInjectStr.append("+(sEnd - sStart)/1000000+\" (毫秒)\");");
        print("方法最后一句插入了：" + endInjectStr.toString() + "语句\n")
        method.insertAfter(endInjectStr.toString(), true)
    }
}
