package com.computaion.classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class ShareHoldingPercentageChange{
	private static String EndPoint="https://h.marketsmojo.com/stocks/shareholding/%s.html";
	public HashMap<String,Map<String,Object>> getStockPercentageChange(int Range1,int Range2,List<HashMap<String,Object>>companyList) throws JsonMappingException, JsonProcessingException {
		HashMap<String,Map<String,Object>> FinalDataMap=new HashMap<String,Map<String,Object>>();
		if(Range2==0) {
			for(HashMap<String,Object> Maps:companyList) 
					if( !((String)Maps.get("CompanyMarketCap")).isEmpty() && Double.parseDouble((String)Maps.get("CompanyMarketCap"))>= Range1) {
						String CompanyURL=String.format(EndPoint, Maps.get("CompanyID"));
						Response response=RestAssured.given().when().get(CompanyURL);
						String ResponseString=response.getBody().asString();
						
						
						Document doc=Jsoup.parse(ResponseString);
						String Promoters = doc.select("td table tr:nth-of-type(2) td:nth-of-type(2)").text();
						String FIIs = doc.select("td table tr:nth-of-type(4) td:nth-of-type(2)").text();
						String MutualFunds = doc.select("td table tr:nth-of-type(6) td:nth-of-type(2)").text();
						String Insurance = doc.select("td table tr:nth-of-type(8) td:nth-of-type(2)").text();
						String OtherDIIs = doc.select("td table tr:nth-of-type(10) td:nth-of-type(2)").text();
						String NonInst = doc.select("td table tr:nth-of-type(12) td:nth-of-type(2)").text();
						String Others = doc.select("td table tr:nth-of-type(14) td:nth-of-type(2)").text();
						
						Promoters=Promoters.equals("No Change")||Promoters.equals("-")||Promoters.equals("")?"0":Promoters;
						FIIs=FIIs.equals("No Change")||FIIs.equals("-")||FIIs.equals("")?"0":FIIs;
						MutualFunds=MutualFunds.equals("No Change")||MutualFunds.equals("-")||MutualFunds.equals("")?"0":MutualFunds;
						Insurance=Insurance.equals("No Change")||Insurance.equals("-")||Insurance.equals("")?"0":Insurance;
						OtherDIIs=OtherDIIs.equals("No Change")||OtherDIIs.equals("-")||OtherDIIs.equals("")?"0":OtherDIIs;
						NonInst=NonInst.equals("No Change")||NonInst.equals("-")||NonInst.equals("")?"0":NonInst;
						Others=Others.equals("No Change")||Others.equals("-")||Others.equals("")?"0":Others;

						HashMap<String,Object> DataMap=new HashMap<>();
						DataMap.put("Promoters", Promoters.replace("%", ""));
						DataMap.put("FIIs", FIIs.replace("%", ""));
						DataMap.put("MutualFunds", MutualFunds.replace("%", ""));
						DataMap.put("Insurance", Insurance.replace("%", ""));
						DataMap.put("OtherDIIs", OtherDIIs.replace("%", ""));
						DataMap.put("NonInst", NonInst.replace("%", ""));
						DataMap.put("Others", Others.replace("%", ""));
						
						FinalDataMap.put((String)Maps.get("CompanyName"), DataMap);
					}
		}else {
		for(HashMap<String,Object> Maps:companyList) 
				if(!((String)Maps.get("CompanyMarketCap")).isEmpty() && Double.parseDouble((String)Maps.get("CompanyMarketCap")) >= Range1 && Double.parseDouble((String)Maps.get("CompanyMarketCap")) < Range2){
					String CompanyURL=String.format(EndPoint, Maps.get("CompanyID"));
					Response response=RestAssured.given().when().get(CompanyURL);
					String ResponseString=response.getBody().asString();

					Document doc=Jsoup.parse(ResponseString);
					String Promoters = doc.select("td table tr:nth-of-type(2) td:nth-of-type(2)").text();
					String FIIs = doc.select("td table tr:nth-of-type(4) td:nth-of-type(2)").text();
					String MutualFunds = doc.select("td table tr:nth-of-type(6) td:nth-of-type(2)").text();
					String Insurance = doc.select("td table tr:nth-of-type(8) td:nth-of-type(2)").text();
					String OtherDIIs = doc.select("td table tr:nth-of-type(10) td:nth-of-type(2)").text();
					String NonInst = doc.select("td table tr:nth-of-type(12) td:nth-of-type(2)").text();
					String Others = doc.select("td table tr:nth-of-type(14) td:nth-of-type(2)").text();
					
					Promoters=Promoters.equals("No Change")||Promoters.equals("-")||Promoters.equals("")?"0":Promoters;
					FIIs=FIIs.equals("No Change")||FIIs.equals("-")||FIIs.equals("")?"0":FIIs;
					MutualFunds=MutualFunds.equals("No Change")||MutualFunds.equals("-")||MutualFunds.equals("")?"0":MutualFunds;
					Insurance=Insurance.equals("No Change")||Insurance.equals("-")||Insurance.equals("")?"0":Insurance;
					OtherDIIs=OtherDIIs.equals("No Change")||OtherDIIs.equals("-")||OtherDIIs.equals("")?"0":OtherDIIs;
					NonInst=NonInst.equals("No Change")||NonInst.equals("-")||NonInst.equals("")?"0":NonInst;
					Others=Others.equals("No Change")||Others.equals("-")||Others.equals("")?"0":Others;
					
					
					HashMap<String,Object> DataMap=new HashMap<>();
					DataMap.put("Promoters", Promoters.replace("%", ""));
					DataMap.put("FIIs", FIIs.replace("%", ""));
					DataMap.put("MutualFunds", MutualFunds.replace("%", ""));
					DataMap.put("Insurance", Insurance.replace("%", ""));
					DataMap.put("OtherDIIs", OtherDIIs.replace("%", ""));
					DataMap.put("NonInst", NonInst.replace("%", ""));
					DataMap.put("Others", Others.replace("%", ""));
					
					FinalDataMap.put((String)Maps.get("CompanyName"), DataMap);
				}
		}
		return FinalDataMap;
		
	}

}
