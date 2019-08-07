package com.aspect;

import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class MyAspect {

	@Pointcut(value = "execution(* com.dao.*.*(..))")
	public void pointCut(){

	}

	@Before("pointCut()")
	public void beforeAdvice(){
		System.out.println("Begin before Advice");
	}

	@After("pointCut()")
	public void afterAdvice(){
		System.out.println("End after Advice");
	}
}
