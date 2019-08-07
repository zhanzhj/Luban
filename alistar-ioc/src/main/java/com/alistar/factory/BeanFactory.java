package com.alistar.factory;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BeanFactory {

	public BeanFactory() {
		init();
	}

	private Map<String, Object> factory = new HashMap();

	public  Object getBean(String beanName){
		return factory.get(beanName);
	}


	public void init() {
		SAXReader reader = new SAXReader();
		try {
			String path = "D:/Luban/alistar-ioc/src/main/resources/spring.xml";
			Document document = reader.read(new File(path));
			Element rootElement = document.getRootElement();
			Iterator<Element> iterator = rootElement.elementIterator();
			while (iterator.hasNext()){
				//1.实例化对象
				Element element = iterator.next();
				Attribute attributeId = element.attribute("id");
				String beanName = attributeId.getValue();
				Attribute attributeClass = element.attribute("class");
				String beanClass = attributeClass.getValue();
				Class clazz = Class.forName(beanClass);
				Object instance = null;

				//2.维护依赖关系（判断是否有属性）
				Iterator<Element> childIterator = element.elementIterator();
				while (childIterator.hasNext()){
					Element nextElement = childIterator.next();
					if("property".equals(nextElement.getName())){
						instance = clazz.newInstance();
						String refBean = nextElement.attribute("ref").getValue();
						Object injectBean = factory.get(refBean);
						String filedName = nextElement.attribute("name").getValue();
						Field declaredField = clazz.getDeclaredField(filedName);
						declaredField.setAccessible(true);
						declaredField.set(instance, injectBean);
					}else {
						String refBean = nextElement.attribute("ref").getValue();
						Object injectBean = factory.get(refBean);
						Constructor<?> constructor = clazz.getConstructor(injectBean.getClass().getInterfaces()[0]);
						instance = constructor.newInstance(injectBean);
					}
				}
				if(instance == null){
					instance = clazz.newInstance();
				}
				factory.put(beanName, instance);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(factory);
	}

}
