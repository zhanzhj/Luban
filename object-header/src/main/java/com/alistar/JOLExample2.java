package com.alistar;

import org.openjdk.jol.info.ClassLayout;

/**
 * 没有进行hashcode前的对象头信息，可以看到1-7B的 56bit没有值
 * 打印完hashcode后就有值了，为什么是1-7B，不是0-6B呢？ 因为是小端存储。
 * 可以确定java对象头当中mark word里面的后七个字节存储的是hashcode信息，
 * 那么第一个字节当中的八位分别存储的就是 分代年龄，偏向锁信息，和对象状态，
 * 这个8 bit中 四位表示分代年龄， 1 位表示偏向锁， 2位表示对象状态
 * 对象进行HASHCODE之后就不能偏向了
 *
 */
public class JOLExample2 {
	public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
		A a = new A();
		System.out.println("before hash");
		//没有计算HASHCODE之前的对象头
		System.out.println(ClassLayout.parseInstance(a).toPrintable());
		//JVM计算的hashcode
		System.out.println("JVM------------------" + Integer.toHexString(a.hashCode()));
		HashUtil.countHash(a);
		//计算完HASHCODE，我们可以查看对象头的信息变化
		System.out.println(ClassLayout.parseInstance(a).toPrintable());
	}
}
