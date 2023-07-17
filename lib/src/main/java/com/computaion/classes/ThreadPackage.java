package com.computaion.classes;

import com.RestAssured.RestPackage.RestAssuredClass;

public class ThreadPackage {
	
	public static ThreadPackage threadPackage;
	private static ThreadLocal<RestAssuredClass>  ThreadLocalClass=new ThreadLocal<>();
	private static ThreadLocal<ShareHoldingChange>  ShareThreadLocalClass=new ThreadLocal<>();
	private static ThreadLocal<ShareHoldingPercentageChange>  SharePChangeThreadClass=new ThreadLocal<>();
	private static ThreadLocal<PromoterBuying> PromoterBuyingClass=new ThreadLocal<>();
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
	
	public void setThreadLocalShareHolding(ShareHoldingChange clsObject) {
		System.out.println("Thread started");
		ShareThreadLocalClass.set(clsObject);	
	}
	
	public void setThreadLocalSharePChange(ShareHoldingPercentageChange clsObject) {
		System.out.println("Thread started");
		SharePChangeThreadClass.set(clsObject);	
	}
	
	public void setThreadLocalPromoterBuying(PromoterBuying clsObject) {
		System.out.println("Thread started");
		PromoterBuyingClass.set(clsObject);	
	}
	
	
	public RestAssuredClass getThreadLocal() {
		return ThreadLocalClass.get();	
	}
	public ShareHoldingChange getThreadLocalShareHolding() {
		return ShareThreadLocalClass.get();	
	}
	public ShareHoldingPercentageChange getThreadLocalSharePchange() {
		return SharePChangeThreadClass.get();	
	}
	public PromoterBuying getThreadLocalPromoterBuying() {
		return PromoterBuyingClass.get();	
	}

}
