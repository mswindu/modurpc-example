package ru.snilov.modu.rpc.api.dto;

public class PrimitiveDTO {
    private int a1;
    private long a2;
    private boolean a3;
    private char a4;
    private double a5;

    public PrimitiveDTO(int a1, long a2, boolean a3, char a4, double a5) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;
        this.a5 = a5;
    }

    public int getA1() {
        return a1;
    }

    public long getA2() {
        return a2;
    }

    public boolean isA3() {
        return a3;
    }

    public char getA4() {
        return a4;
    }

    public double getA5() {
        return a5;
    }

    public void setA5(double a5) {
        this.a5 = a5;
    }

    public void setA4(char a4) {
        this.a4 = a4;
    }

    public void setA3(boolean a3) {
        this.a3 = a3;
    }

    public void setA2(long a2) {
        this.a2 = a2;
    }

    public void setA1(int a1) {
        this.a1 = a1;
    }

    @Override
    public String toString() {
        return "PrimitiveDTO{" +
                "a1=" + a1 +
                ", a2=" + a2 +
                ", a3=" + a3 +
                ", a4=" + a4 +
                ", a5=" + a5 +
                '}';
    }
}
