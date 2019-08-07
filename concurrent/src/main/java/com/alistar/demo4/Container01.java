package com.alistar.demo4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 一道面试题：实现一个容器，提供两个方法，add,size
 * 写两个线程，线程1添加10个元素到容器中，线程2实现监控元素的个数，
 * 当个数到5个时，线程2给出提示并结束线程2
 *
 * 这里list在两个线程之间不保证可见性，所以线程2始终结束不了
 * 有两个问题，第一由于没有加同步，可能size等于5的时候，有另外一个线程加了一下才break，不是很精确
 * 第二个问题就是浪费cpu，T2线程用的是死循环
 */
public class Container01 {

	private List<Object> list = new ArrayList<>();

	public void add(Object o){
		list.add(o);
	}

	public int size(){
		return list.size();
	}

	public static void main(String[] args) {
		Container01 c = new Container01();
		new Thread(()->{
			for (int i = 0; i < 10; i++) {
				c.list.add(new Object());
				System.out.println("add " + i);

				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "T1").start();

		new Thread(()->{
			while (true){
				if(c.list.size() == 5){
					break;
				}
			}
			System.out.println("t2 线程结束");
		}, "T2").start();
	}
}
