package com.alistar.demo1;

import java.util.concurrent.TimeUnit;

/**
 * 不要以字符串常量作为锁定对象
 */
public class SynchronizedDemo2 {
	private String str1 = "hello";
	private String str2 = "hello";

	public void test1(){
		synchronized (str1){
			System.out.println(Thread.currentThread().getName() +" t1 start");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(Thread.currentThread().getName() +" t1 end");
		}
	}

	public void test2(){
		synchronized (str2){
			System.out.println(Thread.currentThread().getName() +" t2 start");
		}
	}

	public static void main(String[] args) {
		SynchronizedDemo2 demo2 = new SynchronizedDemo2();
		new Thread(demo2::test1, "T1").start();
		new Thread(demo2::test2, "T2").start();
	}
}
