package com.alistar.pool;

import java.util.concurrent.*;

public class MyThreadPool {

	public static void main(String[] args) {
		ExecutorService executorService = Executors.newFixedThreadPool(5);
	}
}
