package com.dao;

import org.springframework.stereotype.Repository;

@Repository
public class IndexDao {

	public void sayHello(){
		System.out.println("Hello AOP");
	}
}
