package com.alistar;

import org.openjdk.jol.info.ClassLayout;

import java.util.concurrent.TimeUnit;

/**
 * JVM 默认延迟4.3秒开启偏向锁
 * -XX:BiasedLockingStartupDelay=0 设置偏向锁延迟启动
 */
public class JOLExample3 {
	public static void main(String[] args) throws InterruptedException {
		Thread.sleep(5000);
		A a = new A();
		System.out.println("before lock");
		System.out.println(ClassLayout.parseInstance(a).toPrintable());
		synchronized (a){
			System.out.println("during lock");
			System.out.println(ClassLayout.parseInstance(a).toPrintable());
		}
		System.out.println("after lock");
		System.out.println(ClassLayout.parseInstance(a).toPrintable());
	}
}
