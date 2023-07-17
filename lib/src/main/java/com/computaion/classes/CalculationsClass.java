package com.computaion.classes;

import java.util.*;

import org.testng.annotations.Test;

public class CalculationsClass {
	
	public volatile List<HashMap<String,Object>> CompanyListMaps=new ArrayList<>();
	@Test
	public List<HashMap<String,Object>> getCompanyDatawithMarketCapMarketCapRange(int Range1,int Range2,List<HashMap<String,Object>>companyList) {
		
		if(Range2==0) {
			for(HashMap<String,Object> Maps:companyList)
					if((Double)Maps.get("CompanyMarketCap")>= Range1)
						CompanyListMaps.add(Maps);
		}else {
		for(HashMap<String,Object> Maps:companyList)
				if((Double)Maps.get("CompanyMarketCap") >= Range1 && (Double)Maps.get("CompanyMarketCap") < Range2)
					  CompanyListMaps.add(Maps);
		
		}
		System.out.println("m4");
		return CompanyListMaps;
	}
	

}
