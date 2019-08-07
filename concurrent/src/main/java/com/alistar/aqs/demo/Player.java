package com.alistar.aqs.demo;

import com.alistar.aqs.MyCountDownLatch;

import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class Player extends Thread{
	private static int count  = 1;
	private int id = count++;
	private MyCountDownLatch latch;

	public Player(MyCountDownLatch countDownLatch){
		this.latch = countDownLatch;
	}

	@Override
	public void run() {
		System.out.println("【玩家" + id + "已入场】");
		latch.countDown();
	}

	public static void main(String[] args) throws InterruptedException {
		MyCountDownLatch latch = new MyCountDownLatch(3);
		System.out.println("牌局开始, 等待玩家入场...");
		new Player(latch).start();
		new Player(latch).start();
		new Player(latch).start();
		latch.await();
		System.out.println("玩家已到齐, 开始发牌...");
	}
}
