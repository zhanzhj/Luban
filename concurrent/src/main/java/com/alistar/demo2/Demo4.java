package com.alistar.demo2;

import java.util.concurrent.TimeUnit;

/**
 * 重入锁的另一个特性，继承
 *
 */
public class Demo4 {

	public synchronized void test(){
		System.out.println("test1 start");
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("test1 end");
	}

	public static void main(String[] args) {
		new Demo5().test();
	}
}


class Demo5 extends Demo4{

	@Override
	public synchronized void test() {
		System.out.println("test2 start");
		super.test();
		System.out.println("test2 end");
	}
}