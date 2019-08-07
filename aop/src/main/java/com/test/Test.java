package com.test;


import com.config.MyConifg;
import com.dao.IndexDao;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {

	public static void main(String[] args){
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyConifg.class);
		IndexDao indexDao = context.getBean(IndexDao.class);
		indexDao.sayHello();
	}
}
