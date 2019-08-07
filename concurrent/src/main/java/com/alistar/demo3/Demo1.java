package com.alistar.demo3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * atomicXXX: 多个atomic类连续调用能否构成原子性？  不能
 */
public class Demo1 {

	AtomicInteger count = new AtomicInteger(0);

	/**
	 * 比如count加到999了，这时候一个线程拿到count判断，虽然get()方法保证原子性，
	 * 但是他阻止不了其他线程来判断，所以第一个线程还没加完，第二个线程也进来了，这时候两个线程都给count加了
	 */
	public void test(){
		for (int i = 0; i < 10000; i++) {
			if(count.get() < 1000){
				count.incrementAndGet();
			}
		}
	}

	public static void main(String[] args) {
		Demo1 demo1 = new Demo1();
		for (int i = 0; i < 10; i++) {
			new Thread(demo1::test).start();
		}
		System.out.println(demo1.count);
	}
}
