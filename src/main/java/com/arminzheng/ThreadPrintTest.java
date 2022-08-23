package com.arminzheng;

/**
 * ThreadPrintTest
 *
 * <p>A线程打印A B线程打印B C线程打印C
 *
 * @author ape
 * @since 2022.08.23
 */
public class ThreadPrintTest {

    private static final Object lock = new Object();
    /* 0-A, 1-B, 2-C */
    private static Integer state = 0;
    private static Integer count = 0;

    public static void main(String[] args) {
        new Thread(
                        () -> {
                            while (true) {
                                synchronized (lock) {
                                    if (count <= 100 - 1 && state == 0) {
                                        System.out.print("A");
                                        lock.notifyAll();
                                        count++;
                                        state = 1;
                                        try {
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        },
                        "t1")
                .start();
        new Thread(
                        () -> {
                            while (true) {
                                synchronized (lock) {
                                    if (count <= 100 - 1 && state == 1) {
                                        System.out.print("B");
                                        lock.notifyAll();
                                        count++;
                                        state = 2;
                                        try {
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        },
                        "t2")
                .start();
        new Thread(
                        () -> {
                            while (true) {
                                synchronized (lock) {
                                    if (count <= 100 - 1 && state == 2) {
                                        System.out.println("C");
                                        lock.notifyAll();
                                        count++;
                                        state = 0;
                                        try {
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        },
                        "t3")
                .start();
    }
}
