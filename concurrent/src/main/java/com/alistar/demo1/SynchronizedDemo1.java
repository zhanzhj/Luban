package com.alistar.demo1;

import java.util.concurrent.TimeUnit;

/**
 * 锁对象的改变
 * 锁定某对象O，如果O的属性发生改变，不影响锁的使用
 * 但是如果O变成另外一个对象，则锁定的对象发生了改变，
 * 应该避免将锁定对象的引用改变成另外一个对象
 */
public class SynchronizedDemo1 {

	Object object = new Object();

	public  void  test(){
		synchronized (object){
			while (true){
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println(Thread.currentThread().getName());
			}
		}
	}

	public static void main(String[] args) {
		SynchronizedDemo1 demo1 = new SynchronizedDemo1();

		new Thread(demo1::test, "t1").start();

		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Thread t2 = new Thread(demo1::test, "t2");

		demo1.object = new Object();
		//T2 能否执行？
		t2.start();
	}
}
