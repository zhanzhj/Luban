package com.alistar.demo2;

import java.util.concurrent.TimeUnit;

/**
 * 一个同步方法调用另一个同步方法是否可以获得锁？ 可以
 *
 */
public class Demo3 {

	public synchronized void test1(){
		System.out.println("test1 start");
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		test2();
		System.out.println("test1 end");
	}

	public synchronized void test2(){
		System.out.println("test2 start");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("test2 end");
	}

	public static void main(String[] args) {
		Demo3 demo = new Demo3();
		demo.test1();
	}
}
