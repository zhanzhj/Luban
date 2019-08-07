package com.alistar;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import static java.lang.System.out;

/**
 *
 */
public class JOLExample1 {
	public static void main(String[] args) {
		A a = new A();
		out.println(VM.current().details());
		out.println(ClassLayout.parseInstance(a).toPrintable());
	}
}
