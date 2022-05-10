package com.RestAssured.RestPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.jayway.jsonpath.JsonPath;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class RestAssuredClass {
	public final static String BaseURI="https://api.bseindia.com/";
	public final static String AllCompaniesURL="BseIndiaAPI/api/ListofScripData/w?Group=&Scripcode=&industry=&segment=Equity&status=Active";
	public final static String PromoterBuyingURL="BseIndiaAPI/api/InsiderTrade15/w?fromdt=&scripcode=%s&todt=";
	private static String EndPoint1="BseIndiaAPI/api/getScripHeaderData/w?Debtflag=&scripcode=%s&seriesid=";
	private volatile HashMap<String,Object> CompnayData;
    public volatile List<HashMap<String,Object>> CompnayDataList=new ArrayList<HashMap<String,Object>>();
    private Response response;
    private static String CompanyIDPath="$..SCRIP_CD";
    private static String CompanyNamePath="$..Scrip_Name";
    private static String CompanyURLPath="$..NSURL";
    private static String CompanyScriptIDPath="$..scrip_id";
    private static String CompanyMarketCapPath="$..Mktcap";
    private static String CompanyPercentageChange="$.CurrRate.PcChg";
    public List<HashMap<String,Object>> getAllCompaniesData() {
    	response =RestAssured.given().when().baseUri(BaseURI).get(AllCompaniesURL);
    	String JsonBody= response.getBody().asString();
    	List<String> CompanyID=JsonPath.read(JsonBody,CompanyIDPath);
    	List<String> CompanyName=JsonPath.read(JsonBody,CompanyNamePath);
    	List<String> CompanyURL=JsonPath.read(JsonBody,CompanyURLPath);
    	List<String> CompanyScriptID=JsonPath.read(JsonBody,CompanyScriptIDPath);
    	List<String> CompanyMarketCap=JsonPath.read(JsonBody,CompanyMarketCapPath);
    	System.out.println(CompanyID.size());
    	for(int i=0;i<CompanyID.size();i++) {
    		CompnayData=new HashMap<>();
    		String MarketCap=CompanyMarketCap.get(i)==""?"0":CompanyMarketCap.get(i);
    		CompnayData.put("CompanyID", CompanyID.get(i));
    		CompnayData.put("CompanyName", CompanyName.get(i));
    		CompnayData.put("CompanyURL", CompanyURL.get(i));
    		CompnayData.put("CompanyScriptID", CompanyScriptID.get(i));
    		CompnayData.put("CompanyMarketCap", MarketCap);
    		CompnayDataList.add(CompnayData);
    	}
    	return CompnayDataList;
    }
    
    public HashMap<String,Double> getprecentageChange(int Range1,int Range2,List<HashMap<String,Object>>companyList,double Slab){
    	HashMap<String, Double> Company=new HashMap<>();
    	if(Range2==0) {
			for(HashMap<String,Object> Maps:companyList)
					if(Double.parseDouble((String)Maps.get("CompanyMarketCap"))>= Range1) {
						String CompanyURL=String.format(EndPoint1, Maps.get("CompanyID"));
						response =RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
						String Percentage=JsonPath.read(response.getBody().asString(), CompanyPercentageChange);
						double percentageChange=Double.parseDouble(Percentage);
						if(percentageChange>=Slab)
							Company.put((String)Maps.get("CompanyName"), percentageChange);
					}
		}else {
		for(HashMap<String,Object> Maps:companyList)
				if(Double.parseDouble((String)Maps.get("CompanyMarketCap")) >= Range1 && Double.parseDouble((String)Maps.get("CompanyMarketCap")) < Range2){
					String CompanyURL=String.format(EndPoint1, Maps.get("CompanyID"));
					response =RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Percentage=JsonPath.read(response.getBody().asString(), CompanyPercentageChange);
					if(Percentage!=null && !Percentage.equals("") && !Percentage.contains("-")) {
					double percentageChange=Double.parseDouble(Percentage);
					if(percentageChange>=Slab)
						Company.put((String)Maps.get("CompanyName"), percentageChange);
					}
				}
		}
    	return Company;
    }
    
    
   
    public List<HashMap<String,Object>> getPromoterBuyingData(int Range1,int Range2,List<HashMap<String,Object>> companyList){
    	
      String CurrentDate=new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      String CommonPath="$.Table[?(@.Fld_PersonCatgName=~/Promoter.*/i && @.Fld_DateIntimation==\""+CurrentDate+"T00:00:00\" && @.Fld_TransactionType==\"Acquisition\")]";
      String CompanyName=CommonPath+".Companyname";
      String AmountPurchased=CommonPath+".Fld_SecurityValue";
      String NumberOfShares=CommonPath+".Fld_SecurityNo";
      String CompanyCode=CommonPath+".Fld_ScripCode";
      
      List<HashMap<String,Object>> list=new ArrayList<HashMap<String,Object>>();
      HashMap<String, Object> Company;
  	if(Range2==0) {
			for(HashMap<String,Object> Maps:companyList)
					if(Double.parseDouble((String)Maps.get("CompanyMarketCap"))>= Range1) {
						String CompanyURL=String.format(PromoterBuyingURL, Maps.get("CompanyID"));
						response =RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
						List<String> CompanyNames=JsonPath.read(response.getBody().asString(), CompanyName);
						List<Double> Amount=JsonPath.read(response.getBody().asString(), AmountPurchased);
						List<Integer> NumberOfshares=JsonPath.read(response.getBody().asString(), NumberOfShares);
						List<Integer> Companycode=JsonPath.read(response.getBody().asString(), CompanyCode);
						for(int i=0;i<Companycode.size();i++) {
							Company=new HashMap<>();
							Company.put("CompanyName", CompanyNames.get(i));
							Company.put("AmountPurchased", Amount.get(i));
							Company.put("NumberOfShares", NumberOfshares.get(i));
							Company.put("CompanyCode", Companycode.get(i));
							System.out.println(Company);
							list.add(Company);
							
						}
						
					}
		}else {
		for(HashMap<String,Object> Maps:companyList)
				if(Double.parseDouble((String)Maps.get("CompanyMarketCap")) >= Range1 && Double.parseDouble((String)Maps.get("CompanyMarketCap")) < Range2){
					String CompanyURL=String.format(PromoterBuyingURL, Maps.get("CompanyID"));
					response =RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					List<String> CompanyNames=JsonPath.read(response.getBody().asString(), CompanyName);
					List<Double> Amount=JsonPath.read(response.getBody().asString(), AmountPurchased);
					List<Integer> NumberOfshares=JsonPath.read(response.getBody().asString(), NumberOfShares);
					List<Integer> Companycode=JsonPath.read(response.getBody().asString(), CompanyCode);
					for(int i=0;i<Companycode.size();i++) {
						Company=new HashMap<>();
						Company.put("CompanyName", CompanyNames.get(i));
						Company.put("AmountPurchased", Amount.get(i));
						Company.put("NumberOfShares", NumberOfshares.get(i));
						Company.put("CompanyCode", Companycode.get(i));
						System.out.println(Company);
						list.add(Company);
						
					}
					
				}
		}
  	return list;
      
   
    }
    
	
}
