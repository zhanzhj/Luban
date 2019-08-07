package com.alistar.demo2;

public class Demo1 {
	private int count = 10;

	public void decrease(){
		count--;
		System.out.println(Thread.currentThread().getName() + " Count = " + count);
	}

	public static void main(String[] args) {
		Demo1 demo1 = new Demo1();
		for (int i = 0; i < 100; i++) {
			new Thread(demo1::decrease, "T" + i).start();
		}
	}
}
