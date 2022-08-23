package com.arminzheng;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadPrintReentrantLock
 *
 * @author ape
 * @since 2022.08.23
 */
public class ThreadPrintReentrantLockTest {

    public static final ReentrantLock lock = new ReentrantLock();
    private static Integer count = 0;

    public static void main(String[] args) {
        Condition conditionA = lock.newCondition();
        Condition conditionB = lock.newCondition();
        Condition conditionC = lock.newCondition();

        new Thread(new PrintRunner(lock, new Condition[]{conditionA, conditionB}, 34, 'A'), "t1").start();
        new Thread(new PrintRunner(lock, new Condition[]{conditionB, conditionC}, 33, 'B'), "t2").start();
        new Thread(new PrintRunner(lock, new Condition[]{conditionC, conditionA}, 33, 'C'), "t3").start();
        /*
        new Thread(() -> {
            while (true) {
                lock.lock();
                if (count <= 100 - 1) {
                    System.out.print("A");
                    count++;
                }
                conditionB.signal();
                try {
                    conditionA.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
            }
        }, "t1").start();
        new Thread(() -> {
            while (true) {
                lock.lock();
                if (count <= 100 - 1) {
                    System.out.print("B");
                    count++;
                }
                conditionC.signal();
                try {
                    conditionB.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
            }
        }, "t2").start();
        new Thread(() -> {
            while (true) {
                lock.lock();
                if (count <= 100 - 1) {
                    System.out.println("C");
                    count++;
                }
                conditionA.signal();
                try {
                    conditionC.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
            }
        }, "t3").start();
        */
    }

    static class PrintRunner implements Runnable {
        private ReentrantLock reentrantLock;
        private Condition[] conditions;
        private Integer count;
        private Character character;
        private int index;

        public PrintRunner(ReentrantLock reentrantLock, Condition[] conditions, Integer count, Character character) {
            this.reentrantLock = reentrantLock;
            this.conditions = conditions;
            this.count = count;
            this.character = character;
        }

        @Override
        public void run() {
            while (true) {
                reentrantLock.lock();
                if (index <= count) {
                    System.out.println(character);
                    index++;
                }
                conditions[1].signal();
                try {
                    conditions[0].await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
                if (index > count) {
                    break;
                }
            }
        }
    }
}
