package com.alistar.test;

import java.util.concurrent.TimeUnit;

public class TestSync {

	public void sync(){
		synchronized (this){
			try {
				TimeUnit.MINUTES.sleep(2);
				System.out.println(Thread.currentThread().getName() + " is Running");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	public static void main(String[] args) {
		TestSync obj = new TestSync();
		for (int i = 0; i < 5; i++) {
			new Thread(obj::sync).start();
		}
	}
}
