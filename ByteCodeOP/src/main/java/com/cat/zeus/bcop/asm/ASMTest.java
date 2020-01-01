package com.cat.zeus.bcop.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ASMTest {
    public static void main(String[] args) {
        System.out.println("****** main ******");

    }

    public static void test1(){
        String inputPath = "D:\\code\\android\\helloapt\\ByteCodeOP\\build\\classes\\java\\main\\com\\cat\\zeus\\bcop\\A.class";
        String outPath = "D:\\code\\android\\helloapt\\ByteCodeOP\\build\\classes\\java\\main\\com\\cat\\zeus\\bcop\\Test1.class";
        copy(inputPath, outPath);
    }

    public static void copy(String inputPath, String outPath) {
        try {
            FileInputStream is = new FileInputStream(inputPath);
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cr.accept(cw, 0);
            FileOutputStream fos = new FileOutputStream(outPath);
            fos.write(cw.toByteArray());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void weave(String inputPath, String outPath) {
        try {
            FileInputStream is = new FileInputStream(inputPath);
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            CallClassAdapter adapter = new CallClassAdapter(cw);
            cr.accept(adapter, 0);
            FileOutputStream fileOutputStream = new FileOutputStream(outPath);
            fileOutputStream.write(cw.toByteArray());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
