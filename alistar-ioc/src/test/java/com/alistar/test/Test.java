package com.alistar.test;

import com.alistar.factory.BeanFactory;
import com.alistar.service.CnstService;
import com.alistar.service.IndexService;

public class Test {
	public static void main(String[] args) {
		BeanFactory factory = new BeanFactory();
		IndexService service = (IndexService)factory.getBean("indexService");
		CnstService cnstService = (CnstService)factory.getBean("cnstService");
		service.service();
		cnstService.find();
	}
}
