package com.alistar.service;

import com.alistar.dao.IndexDao;

public class CnstService {

	private IndexDao indexDao;

	public CnstService(IndexDao indexDao) {
		this.indexDao = indexDao;
	}

	public void find() {
		System.out.println("CnstService constructor begin");
		indexDao.sayHello();
		System.out.println("CnstService constructor end");
	}
}
