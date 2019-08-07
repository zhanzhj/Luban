package com.alistar.service;

import com.alistar.dao.IndexDao;

public class IndexService {

	private IndexDao indexDao;

	public void setIndexDao(IndexDao indexDao) {
		this.indexDao = indexDao;
	}

	public void service(){
		System.out.println("IndexService");
		indexDao.sayHello();
	}
}
