package com.cat.zeus.bcop._javassit;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * Javassist的使用 首先获取到class定义的容器ClassPool，
 * 通过它获取已经编译好的类(Compile time class)，并给这个类设置一个父类，
 * 而writeFile讲这个类的定义从新写到磁盘，以便后面使用。
 * <p>
 * Javassist是通过默认的classloader加载类
 * 默认加载到当前线程的ClassLoader中，也可以选择输出的ClassLoader。
 */
public class Test1 {
    public static String tempDir = "./ByteCodeOP/temp";

    public static void main(String[] args) {
        System.out.println("**************** main ****************");
        test2();
    }

    public static void test4(){
//        CtMethod method = null;
//        method.getAnnotations()
    }

    /**
     * $0,$1,$2:分别代表this,第一个参数，第二个参数。
     * $args:将所有参数做为数组放入。
     * $$:所有方法参数的简写，主要用在方法调用上。move(String a,String b)相当于move($$)。
     * $cflow:一个方法调用的深度。
     * $r：方法返回值的类型。
     * $_：方法返回值。（修改方法时不支持）。
     * addCatch()：方法中加入try catch块,$e代表异常。
     * $class:this 的类型（Class）。也就是$0的类型。
     * $sig；方法参数的类型（Class）数组，数组的顺序为
     */
    public static void test3() {
        try {
            //1.获得类池
            ClassPool pool = ClassPool.getDefault();
            //2.创建类
            CtClass ctClass = pool.makeClass("com.earnest.Emp");
            //3.创建属性
            CtField name = CtField.make("private String name;", ctClass);
            CtField age = CtField.make("private int age;", ctClass);
            //4.添加属性
            ctClass.addField(name);
            ctClass.addField(age);
            //5.创建方法
            CtMethod make = CtMethod.make("public String getName() {return name;}", ctClass);
            CtMethod make1 = CtMethod.make("public void setName(String name) {this.name = name;}", ctClass);
            //6.添加方法
            ctClass.addMethod(make);
            ctClass.addMethod(make1);
            //7.添加构造器
            //无参数构造器
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
            ctConstructor.setBody("{}");
            //指定参数构造器
            CtConstructor ctConstructor1 = new CtConstructor(new CtClass[]{CtClass.intType, pool.get("java.lang.String")}, ctClass);
            //$1代表第一个参数，$2代表第二个参数，$0代表this
            ctConstructor1.setBody("{this.name=$2;this.age=$1;}");
            //8.添加构造器
            ctClass.addConstructor(ctConstructor);
            ctClass.addConstructor(ctConstructor1);
            //8.写入工作空间
            ctClass.writeFile(tempDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void test2() {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath("D:\\code\\android\\helloapt\\ByteCodeOP\\build\\classes\\java\\main\\com\\cat\\zeus\\bcop");
            CtClass aclass = classPool.get("com.cat.zeus.bcop.A");
            CtMethod test_A1 = aclass.getDeclaredMethod("test_A1");
            // 插入方法最前面第一句 ($1)获取第一个参数
            test_A1.insertBefore("System.out.println($1);");
            aclass.writeFile(tempDir);

            // CtClass cc = ...;
            // Class c = cc.toClass(bean.getClass().getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void test1() {
        try {
            ClassPool classPool = ClassPool.getDefault();

            // 添加class路径dir
            classPool.appendClassPath("D:\\code\\android\\helloapt\\ByteCodeOP\\build\\classes\\java\\main\\com\\cat\\zeus\\bcop");

            CtClass aClass = classPool.get("com.cat.zeus.bcop.A");
            System.out.println("aClass: " + aClass);
            // 给A设置一个父类B
            CtClass bClass = classPool.get("com.cat.zeus.bcop.B");
            aClass.setSuperclass(bClass);
            // 写到磁盘
            aClass.writeFile(tempDir);

            // 由CtClass可以方便的获取字节码和加载字节码
            aClass = classPool.get("com.cat.zeus.bcop.A");
            byte[] b = aClass.toBytecode();
            Class clazz = aClass.toClass();

            // 定义一个新类
            CtClass xxxx = classPool.makeClass("XXXXXX");
            xxxx.writeFile(tempDir);

            // 通过CtMethod和CtField构造方法、成员或者Annotation
            CtClass Test_x1 = classPool.makeClass("Test_x1");
            String methodStr = "public Integer getInteger() { return null; }";
            CtMethod ctMethod = CtNewMethod.make(methodStr, Test_x1);
            // 添加一个方法
            Test_x1.addMethod(ctMethod);
            // 添加一个成员变量
            CtField ctField = new CtField(CtClass.intType, "i", Test_x1);
            Test_x1.addField(ctField);
            Test_x1.writeFile(tempDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
