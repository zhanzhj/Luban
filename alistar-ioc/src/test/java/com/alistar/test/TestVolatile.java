package com.alistar.test;

public class TestVolatile {
	private static volatile int i = 0;

	private static void increment(){
		i++;
	}

	public static void main(String[] args) {
		for (int j = 0; j < 10; j++) {
			new Thread(()->{
				for (int k = 0; k < 5; k++) {
					increment();
				}
			}).start();
		}

		System.out.println(i);
	}
}
