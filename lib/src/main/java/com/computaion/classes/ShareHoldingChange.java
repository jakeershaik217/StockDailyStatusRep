package com.computaion.classes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class ShareHoldingChange {
	
	private static String EndPoint="https://h.marketsmojo.com/stocks/shareholding/%s.html";
	public HashMap<String,Map<String,Double>> getJsonDataFromAStock(int Range1,int Range2,List<HashMap<String,Object>>companyList) throws JsonMappingException, JsonProcessingException {
		HashMap<String,Map<String,Double>> FinalDataMap=new HashMap<String,Map<String,Double>>();
		if(Range2==0) {
			for(HashMap<String,Object> Maps:companyList)
					if(!((String)Maps.get("CompanyMarketCap")).isEmpty() && Double.parseDouble((String)Maps.get("CompanyMarketCap"))>= Range1) {
						String CompanyURL=String.format(EndPoint, Maps.get("CompanyID"));
						Response response=RestAssured.given().when().get(CompanyURL);
						String ResponseString=response.getBody().asString();
						String JsonValue="";
						
						Document doc=Jsoup.parse(ResponseString);
						Element sampleDiv = doc.getElementById("chartdata");
						if(sampleDiv!=null) {
						Elements elements=sampleDiv.getElementsByTag("input");
						for(Element ele:elements) {
							JsonValue=ele.attr("Value");
						}
						ObjectMapper objectMapper = new ObjectMapper();
						FinalDataMap.put((String)Maps.get("CompanyName"),objectMapper.readValue(JsonValue, Map.class));
						}
					}
		}else {
		for(HashMap<String,Object> Maps:companyList)
				if(!((String)Maps.get("CompanyMarketCap")).isEmpty() && Double.parseDouble((String)Maps.get("CompanyMarketCap")) >= Range1 && Double.parseDouble((String)Maps.get("CompanyMarketCap")) < Range2){
					String CompanyURL=String.format(EndPoint, Maps.get("CompanyID"));
					Response response=RestAssured.given().when().get(CompanyURL);
					String ResponseString=response.getBody().asString();
					String JsonValue="";
					Document doc=Jsoup.parse(ResponseString);
					Element sampleDiv = doc.getElementById("chartdata");
					if(sampleDiv!=null) {
					Elements elements=sampleDiv.getElementsByTag("input");
					for(Element ele:elements) {
						JsonValue=ele.attr("Value");
					}
					ObjectMapper objectMapper = new ObjectMapper();
					FinalDataMap.put((String)Maps.get("CompanyName"),objectMapper.readValue(JsonValue, Map.class));
					}
				}
		}
		return FinalDataMap;
		
	}
	
	public void fetchTableData() {
		Response response=RestAssured.given().when().get("https://h.marketsmojo.com/stocks/shareholding/542651.html");
		String ResponseString=response.getBody().asString();
		String JsonValue="";
		Document doc=Jsoup.parse(ResponseString);
		String tables = doc.select("td table tr:nth-of-type(2) td:nth-of-type(2)").text();
		System.out.println(tables);
		
	}
	
	public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
		ShareHoldingChange ss=new ShareHoldingChange();
		ss.fetchTableData();
		ShareHoldingChange sd=new ShareHoldingChange();
		
	}

}
