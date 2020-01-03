package com.cat.zeus.bcop._javassit;

import com.cat.zeus.bcop.A;

import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;

/**
 * Javassist的使用 首先获取到class定义的容器ClassPool，
 * 通过它获取已经编译好的类(Compile time class)，并给这个类设置一个父类，
 * 而writeFile讲这个类的定义从新写到磁盘，以便后面使用。
 * <p>
 * Javassist是通过默认的classloader加载类
 * 默认加载到当前线程的ClassLoader中，也可以选择输出的ClassLoader。
 */

/**
 * 冻结Class
 * <p>
 * 当CtClass 调用writeFile()、toClass()、toBytecode() 这些方法的时候，Javassist会冻结CtClass Object，对CtClass object的修改将不允许。这个主要是为了警告开发者该类已经被加载，而JVM是不允许重新加载该类的。
 * 突破该限制cc.defrost();
 */
public class Test1 {
    public static String tempDir = "./ByteCodeOP/temp";

    public static void main(String[] args) {
        System.out.println("**************** main ****************");
        test5();
    }

    private static void op_jar() {
        try {
            String srcJarName = "test_xxxx.jar";
            String destJarName = "aaaa_" + srcJarName;

            File directory = new File("");
            String canonicalPath = directory.getCanonicalPath();
            System.out.println("canonicalPath: " + canonicalPath);

            String srcJarPath = canonicalPath + File.separator + "ByteCodeOP" + File.separator + "libs" + File.separator + srcJarName;
            System.out.println("srcJarPath: " + srcJarPath);

            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(srcJarPath);

            String destJarPath = canonicalPath + File.separator + "ByteCodeOP" + File.separator + "libs" + File.separator + destJarName;
            System.out.println("destJarPath: " + destJarPath);

            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(destJarPath));

            JarFile jarFile = new JarFile(srcJarPath);
            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
            while (jarEntryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                String jarEntryName = jarEntry.getName();

                ZipEntry zipEntry = new ZipEntry(jarEntryName);
                jarOutputStream.putNextEntry(zipEntry);

                jarEntryName = jarEntryName.replace("/", ".").substring(0, jarEntryName.length() - ".class".length());
                System.out.println("jarEntryName: " + jarEntryName);

                CtClass ctClass = classPool.getCtClass(jarEntryName);
                CtMethod[] methods = ctClass.getDeclaredMethods();
                for (CtMethod method : methods) {
                    String name = method.getName();
                    method.addLocalVariable("startTime", CtClass.longType);
                    method.addLocalVariable("endTime", CtClass.longType);
                    method.insertBefore("startTime = System.currentTimeMillis();");
                    method.insertBefore("System.out.println(\"insert before ......\");");
                    method.insertAfter("endTime = System.currentTimeMillis();");
                    String s = "System.out.println(\"{0} total time: \" + (endTime - startTime));";
                    method.insertAfter(MessageFormat.format(s, name));
                }

                byte[] code = ctClass.toBytecode();
                jarOutputStream.write(code);
                jarOutputStream.closeEntry();
            }

            jarOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加统计时间的代码
     */
    public static void test5() {
        try {
            long startTime = System.currentTimeMillis();

            ClassPool classPool = ClassPool.getDefault();
            // 获取A.class文件的路径
            String path = A.class.getResource("").getPath();
            classPool.insertClassPath(path);
            CtClass ctClass = classPool.getCtClass("com.cat.zeus.bcop.A");
            CtMethod method = ctClass.getDeclaredMethod("m3");
            String name = method.getName();
            // 添加局部变量
            // 如果不同过addLocalVariable设置
            // 在调用属性时将出现compile error: no such field: startTime
            method.addLocalVariable("startTime", CtClass.longType);
            method.addLocalVariable("endTime", CtClass.longType);

            method.insertBefore("startTime = System.currentTimeMillis();");
            method.insertBefore("System.out.println(\"insert before ......\");");

            method.insertAfter("endTime = System.currentTimeMillis();");
            String s = "System.out.println(\"{0} total time: \" + (endTime - startTime));";
            method.insertAfter(MessageFormat.format(s, name));

            // 添加try ... catch
            CtClass eType = classPool.get("java.lang.Exception");
            method.addCatch("{ System.out.println($e); throw $e; }", eType);

            ctClass.writeFile(tempDir);
            long endTime = System.currentTimeMillis();
            System.out.println(method.getName() + "total time: " + (endTime - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void test4() {
        try {
            ClassPool classPool = ClassPool.getDefault();
            // 获取A.class文件的路径
            String path = A.class.getResource("").getPath();
            classPool.insertClassPath(path);
            CtClass ctClass = classPool.getCtClass("com.cat.zeus.bcop.A");

            CtMethod method = ctClass.getDeclaredMethod("m3");

            method.setBody("$1+=100;");

            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    System.out.println("+++++++++++++++MethodCall+++++++++++++");
                    System.out.println("MethodCall: " + m);

                    String className = m.getClassName();
                    String methodName = m.getMethodName();
                    System.out.println("className: " + className);
                    System.out.println("methodName: " + methodName);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(Cast c) throws CannotCompileException {
                    System.out.println("++++++++++++++Cast++++++++++++++");
                    System.out.println("Cast: " + c);

                    String fileName = c.getFileName();
                    int lineNumber = c.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(Handler h) throws CannotCompileException {
                    System.out.println("++++++++++++++Handler++++++++++++++");
                    System.out.println("Handler: " + h);
                    String fileName = h.getFileName();
                    int lineNumber = h.getLineNumber();

                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    System.out.println("++++++++++++++NewExpr++++++++++++++");
                    System.out.println("NewExpr: " + e);
                    String fileName = e.getFileName();
                    int lineNumber = e.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(NewArray a) throws CannotCompileException {
                    System.out.println("++++++++++++++ NewArray ++++++++++++++");
                    System.out.println("NewArray: " + a);

                    String fileName = a.getFileName();
                    int lineNumber = a.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");

                }

                @Override
                public void edit(Instanceof _instanceof) throws CannotCompileException {
                    System.out.println("++++++++++++++ Instanceof ++++++++++++++");
                    System.out.println("Instanceof: " + _instanceof);

                    String fileName = _instanceof.getFileName();
                    int lineNumber = _instanceof.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    System.out.println("++++++++++++++ FieldAccess ++++++++++++++");
                    System.out.println("FieldAccess: " + f);

                    String fileName = f.getFileName();
                    int lineNumber = f.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }

                @Override
                public void edit(ConstructorCall c) throws CannotCompileException {
                    System.out.println("++++++++++++++ ConstructorCall ++++++++++++++");
                    System.out.println("ConstructorCall: " + c);

                    String fileName = c.getFileName();
                    int lineNumber = c.getLineNumber();
                    System.out.println("fileName: " + fileName);
                    System.out.println("lineNumber: " + lineNumber);
                    System.out.println("++++++++++++++++++++++++++++");
                }
            });

            ctClass.writeFile(tempDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void test0() {
        try {
            File directory = new File("");
            //获取标准路径
            String canonicalPath = directory.getCanonicalPath();
            //获取绝对路径
            String absolutePath = directory.getAbsolutePath();
            System.out.println("canonicalPath: " + canonicalPath);
            System.out.println("absolutePath: " + absolutePath);

            System.out.println("---------------------------------");
            directory = new File(".");
            canonicalPath = directory.getCanonicalPath();
            //获取绝对路径
            absolutePath = directory.getAbsolutePath();
            System.out.println("canonicalPath: " + canonicalPath);
            System.out.println("absolutePath: " + absolutePath);

            System.out.println("---------------------------------");
            directory = new File("..");
            canonicalPath = directory.getCanonicalPath();
            //获取绝对路径
            absolutePath = directory.getAbsolutePath();
            System.out.println("canonicalPath: " + canonicalPath);
            System.out.println("absolutePath: " + absolutePath);

            System.out.println("---------------------------------");
            directory = new File("./src");
            canonicalPath = directory.getCanonicalPath();
            //获取绝对路径
            absolutePath = directory.getAbsolutePath();
            System.out.println("canonicalPath: " + canonicalPath);
            System.out.println("absolutePath: " + absolutePath);

            System.out.println("----------------+++++++-----------------");
            String path = Test1.class.getResource("").getPath();
            System.out.println("path: " + path);
            System.out.println("----------------+++++++-----------------");
            path = Test1.class.getResource("/").getPath();
            System.out.println("path: " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
