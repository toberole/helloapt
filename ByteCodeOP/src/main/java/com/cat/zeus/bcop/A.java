package com.cat.zeus.bcop;

public class A {
    public void m1() {
        System.out.println("m1 A");
    }

    public void m2(int i) {
        System.out.println("m2 A " + i);
    }

    public void m3(int i) {
        //System.out.println("A m3 *****");

        i = i + 2;
        i = i + 2;
        i = i + 2;

        System.out.println("i: " + i);
    }
}
