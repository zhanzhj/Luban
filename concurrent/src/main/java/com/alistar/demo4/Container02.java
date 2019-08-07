package com.alistar.demo4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 一道面试题：实现一个容器，提供两个方法，add,size
 * 写两个线程，线程1添加10个元素到容器中，线程2实现监控元素的个数，
 * 当个数到5个时，线程2给出提示并结束线程2
 *
 * 这里虽然T2能够及时收到消息唤醒，但是wait会释放锁，notify不会释放锁，所以T1线程结束后
 *  * T2线程才执行完成
 */
public class Container02 {

	AtomicInteger integer = new AtomicInteger(10);

	private List<Object> list = new ArrayList<>();

	public void add(Object o){
		list.add(o);
	}

	public int size(){
		return list.size();
	}

	public static void main(String[] args) {
		Container02 c = new Container02();
		Object lock = new Object();
		new Thread(()->{
			synchronized (lock){
				System.out.println("t1 线程启动");
				for (int i = 0; i < 10; i++) {
					c.list.add(new Object());
					System.out.println("add " + i);
					if(c.list.size() == 5){
						lock.notify();
					}
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}, "T1").start();

		new Thread(()->{
			System.out.println("t2 线程开始");
			synchronized (lock){
				if(c.list.size() != 5){
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("t2 线程结束");
		}, "T2").start();
	}
}
