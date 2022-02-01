package com.computaion.classes;

import com.RestAssured.RestPackage.RestAssuredClass;

public class ThreadPackage {
	
	public static ThreadPackage threadPackage;
	private static ThreadLocal<RestAssuredClass>  ThreadLocalClass=new ThreadLocal<>();
	private ThreadPackage() {
		
		
	}
	
	public static ThreadPackage getInstance() {
		
		if(threadPackage==null)
			return threadPackage=new ThreadPackage();
		else
			return threadPackage;
		
	}
	
	public void setThreadLocal(RestAssuredClass clsObject) {
		System.out.println("Thread started");
		ThreadLocalClass.set(clsObject);	
	}
	
	public RestAssuredClass getThreadLocal() {
		return ThreadLocalClass.get();	
	}

}
