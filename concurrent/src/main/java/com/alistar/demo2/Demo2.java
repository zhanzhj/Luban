package com.alistar.demo2;

import java.util.concurrent.TimeUnit;

/**
 * 同步方法和非同步方法是否可以同时调用？ 可以
 */
public class Demo2 {

	public synchronized void test1(){
		System.out.println(Thread.currentThread().getName() + " start");
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + " end");
	}

	public  void test2(){
		System.out.println(Thread.currentThread().getName() + " start");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + " end");
	}

	public static void main(String[] args) {
		Demo2 demo = new Demo2();
		new Thread(demo::test1, "T1").start();
		new Thread(demo::test2, "T2").start();
	}
}
