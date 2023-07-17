package com.RestAssured.RestPackage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


import com.Stock.Utility.StaticVariableCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class RestAssuredClass {
	public final static String BaseURI = "https://api.bseindia.com/";
	public final static String AllCompaniesURL = "BseIndiaAPI/api/ListofScripData/w?Group=&Scripcode=&industry=&segment=Equity&status=Active";
	public final static String SpecificCompanyURL = "BseIndiaAPI/api/ListofScripData/w?Group=&Scripcode=%s&industry=&segment=Equity&status=Active";
	public final static String CompaniesResultsURL = "BseIndiaAPI/api/Corpforthresults/w?fromdate=%s&scripcode=&todate=%s";
	public final static String PromoterBuyingURL = "BseIndiaAPI/api/InsiderTrade15/w?fromdt=&scripcode=%s&todt=";
	public final static String NewsLinkURL = "BseIndiaAPI/api/TabResults/w?scripcode=%s&tabtype=NEWS";
	private static String EndPoint1 = "BseIndiaAPI/api/getScripHeaderData/w?Debtflag=&scripcode=%s&seriesid=";
	private volatile HashMap<String, Object> CompnayData;
	public volatile List<HashMap<String, Object>> CompnayDataList = new ArrayList<HashMap<String, Object>>();
	private Response response;
	private static String CompanyIDPath = "$..SCRIP_CD";
	private static String CompanyNamePath = "$..Scrip_Name";
	private static String CompanyURLPath = "$..NSURL";
	private static String CompanyScriptIDPath = "$..scrip_id";
	private static String CompanyMarketCapPath = "$..Mktcap";
	private static String CompanyPercentageChange = "$.CurrRate.PcChg";

	public List<HashMap<String, Object>> getAllCompaniesData() {
		response = RestAssured.given().when().baseUri(BaseURI).get(AllCompaniesURL);
		String JsonBody = response.getBody().asString();
		List<String> CompanyID = JsonPath.read(JsonBody, CompanyIDPath);
		List<String> CompanyName = JsonPath.read(JsonBody, CompanyNamePath);
		List<String> CompanyURL = JsonPath.read(JsonBody, CompanyURLPath);
		List<String> CompanyScriptID = JsonPath.read(JsonBody, CompanyScriptIDPath);
		List<String> CompanyMarketCap = JsonPath.read(JsonBody, CompanyMarketCapPath);
		System.out.println(CompanyID.size());
		for (int i = 0; i < CompanyID.size(); i++) {
			CompnayData = new HashMap<>();
			String MarketCap = CompanyMarketCap.get(i) == "" ? "0" : CompanyMarketCap.get(i);
			CompnayData.put("CompanyID", CompanyID.get(i));
			CompnayData.put("CompanyName", CompanyName.get(i));
			CompnayData.put("CompanyURL", CompanyURL.get(i));
			CompnayData.put("CompanyScriptID", CompanyScriptID.get(i));
			CompnayData.put("CompanyURLPath", CompanyURL.get(i));
			CompnayData.put("CompanyMarketCap", MarketCap);
			CompnayDataList.add(CompnayData);
		}
		return CompnayDataList;
	}

	public HashMap<String, Double> getprecentageChange(int Range1, int Range2,
			List<HashMap<String, Object>> companyList, double Slab) {
		HashMap<String, Double> Company = new HashMap<>();
		if (Range2 == 0) {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1) {
					String CompanyURL = String.format(EndPoint1, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Percentage = JsonPath.read(response.getBody().asString(), CompanyPercentageChange);
					double percentageChange = Double.parseDouble(Percentage);
					if (percentageChange >= Slab)
						Company.put((String) Maps.get("CompanyName"), percentageChange);
				}
		} else {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) < Range2) {
					String CompanyURL = String.format(EndPoint1, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Percentage = JsonPath.read(response.getBody().asString(), CompanyPercentageChange);
					if (Percentage != null && !Percentage.equals("") && !Percentage.contains("-")) {
						double percentageChange = Double.parseDouble(Percentage);
						if (percentageChange >= Slab)
							Company.put((String) Maps.get("CompanyName"), percentageChange);
					}
				}
		}
		return Company;
	}
	
	public HashMap<String, String> getResultsDetails(int Range1, int Range2,
			List<HashMap<String, Object>> companyList) throws JsonMappingException, JsonProcessingException {
		HashMap<String, String> Company = new HashMap<>();
		if (Range2 == 0) {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1) {
					String CompanyURL = String.format(NewsLinkURL, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Jsonbody = response.getBody().asString();
				    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("\\\"", "\"").replace("}\"", "}]");
					
					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
			        for(JSONObjectCalls jsonObjectCalls:root) {
			        	
			        	//if(jsonObjectCalls.getNewsdt().equals("08/12/2022"))
			        	if(jsonObjectCalls.getNewsdt().equals(getTodaysDate("dd/MM/yyyy")))
			        		for(String Keyword:StaticVariableCollection.KeywordsForSearch) {
			        			
			        			if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			        				
			        				Company.put((String) Maps.get("CompanyName"),"YES");
			        				break;
			        			}
			        				
			        			
			        			
			        		}
			        			
			        	
			        }
				}
		} else {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) < Range2) {
					String CompanyURL = String.format(NewsLinkURL, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Jsonbody = response.getBody().asString();
				    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("\\\"", "\"").replace("}\"", "}]");
					
					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
			        for(JSONObjectCalls jsonObjectCalls:root) {
			        	
			        	//if(jsonObjectCalls.getNewsdt().equals("08/12/2022"))
			        	if(jsonObjectCalls.getNewsdt().equals(getTodaysDate("dd/MM/yyyy")))
			        		for(String Keyword:StaticVariableCollection.KeywordsForSearch) {
			        			
			        			
                                 if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			        				
			        				Company.put((String) Maps.get("CompanyName"),"YES");
			        				break;
			        			}
			        				
			        		}
			        			
			        	
			        }
				}
		}
			return Company;
	}
	
	public HashMap<String, HashMap<String,String>> getNewsDetails(int Range1, int Range2,
			List<HashMap<String, Object>> companyList) throws JsonMappingException, JsonProcessingException {
		HashMap<String, HashMap<String,String>> Company = new HashMap<>();
		if (Range2 == 0) {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1) {
					String CompanyURL = String.format(NewsLinkURL, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Jsonbody = response.getBody().asString();
				    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("\\\"", "\"").replace("}\"", "}]");
					
					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
			        for(JSONObjectCalls jsonObjectCalls:root) {
			        	
			        	//if(jsonObjectCalls.getNewsdt().equals("14/07/2023"))
			        	if(jsonObjectCalls.getNewsdt().equals(getTodaysDate("dd/MM/yyyy")))
			        		for(String Keyword:StaticVariableCollection.KeywordsForSearch) {
			        			HashMap<String,String> MainDataMap=new HashMap<>();
			        			switch(Keyword) {
			        			case "Board Meeting Outcome":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			        				                         MainDataMap.put("Board Meeting Outcome","YES");
			        				                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
			        			                                }
			        			                             break;
			        			case "Dividend":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Dividend","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                     break;
			        			case "Buyback":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Buyback","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			case "Buy back":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Buyback","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			case "Bounus":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Bounus","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			                             
			        			}
			        			
			        			
			        			
			        		}
			        			
			        	
			        }
				}
		} else {
			for (HashMap<String, Object> Maps : companyList)
				if (!(Maps.get("CompanyMarketCap").toString()).equals("")
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) < Range2) {
					String CompanyURL = String.format(NewsLinkURL, Maps.get("CompanyID"));
					if(Maps.get("CompanyName").toString().contains("AARTI"))
						System.out.println();
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					String Jsonbody = response.getBody().asString();
				    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("\\\"", "\"").replace("}\"", "}]");
					
					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
			        for(JSONObjectCalls jsonObjectCalls:root) {
			        	
			        	//if(jsonObjectCalls.getNewsdt().equals("14/07/2023"))
			        	if(jsonObjectCalls.getNewsdt().equals(getTodaysDate("dd/MM/yyyy")))
			        		for(String Keyword:StaticVariableCollection.KeywordsForSearch) {
			        			HashMap<String,String> MainDataMap=new HashMap<>();
			        			switch(Keyword) {
			        			case "Board Meeting Outcome":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			        				                         MainDataMap.put("Board Meeting Outcome","YES");
			        				                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
			        			                                }
			        			                             break;
			        			case "Dividend":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Dividend","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                     break;
			        			case "Buyback":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Buyback","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			case "Buy back":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Buyback","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			case "Bounus":if(jsonObjectCalls.getNewsSubj().contains(Keyword)) {
			                         MainDataMap.put("Bounus","YES");
			                         Company.put((String) Maps.get("CompanyName"),MainDataMap);
		                                                        }
		                                                      break;
			        			                             
			        			}
			        			
			        			
			        			
			        		}
			        			
			        	
			        }
				}
		}
			return Company;
	}

	public List<HashMap<String, Object>> getPromoterBuyingData(int Range1, int Range2,
			List<HashMap<String, Object>> companyList) {

		String CurrentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String CommonPath = "$.Table[?(@.Fld_PersonCatgName=~/Promoter.*/i && @.Fld_DateIntimation==\"" + CurrentDate
				+ "T00:00:00\" && @.Fld_TransactionType==\"Acquisition\")]";
		String CompanyName = CommonPath + ".Companyname";
		String AmountPurchased = CommonPath + ".Fld_SecurityValue";
		String NumberOfShares = CommonPath + ".Fld_SecurityNo";
		String CompanyCode = CommonPath + ".Fld_ScripCode";
		String promoterBuyingURL = "disclosures-insider-trading-2015/";

		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> Company;
		if (Range2 == 0) {
			for (HashMap<String, Object> Maps : companyList)
				if (!((String) Maps.get("CompanyMarketCap")).isEmpty()
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1) {
					String CompanyURL = String.format(PromoterBuyingURL, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					List<String> CompanyNames = JsonPath.read(response.getBody().asString(), CompanyName);
					List<Double> Amount = JsonPath.read(response.getBody().asString(), AmountPurchased);
					List<Integer> NumberOfshares = JsonPath.read(response.getBody().asString(), NumberOfShares);
					List<Integer> Companycode = JsonPath.read(response.getBody().asString(), CompanyCode);
					for (int i = 0; i < Companycode.size(); i++) {
						Company = new HashMap<>();
						Company.put("CompanyName", CompanyNames.get(i));
						Company.put("AmountPurchased", Amount.get(i));
						Company.put("NumberOfShares", NumberOfshares.get(i));
						Company.put("CompanyCode", Companycode.get(i));
						Company.put("CompanyURL", Maps.get("CompanyURL") + promoterBuyingURL);
						System.out.println(Company);
						list.add(Company);

					}

				}
		} else {
			for (HashMap<String, Object> Maps : companyList)
				if (!((String) Maps.get("CompanyMarketCap")).isEmpty()
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) >= Range1
						&& Double.parseDouble((String) Maps.get("CompanyMarketCap")) < Range2) {
					String CompanyURL = String.format(PromoterBuyingURL, Maps.get("CompanyID"));
					response = RestAssured.given().when().baseUri(BaseURI).get(CompanyURL);
					List<String> CompanyNames = JsonPath.read(response.getBody().asString(), CompanyName);
					List<Double> Amount = JsonPath.read(response.getBody().asString(), AmountPurchased);
					List<Integer> NumberOfshares = JsonPath.read(response.getBody().asString(), NumberOfShares);
					List<Integer> Companycode = JsonPath.read(response.getBody().asString(), CompanyCode);
					for (int i = 0; i < Companycode.size(); i++) {
						Company = new HashMap<>();
						Company.put("CompanyName", CompanyNames.get(i));
						Company.put("AmountPurchased", Amount.get(i));
						Company.put("NumberOfShares", NumberOfshares.get(i));
						Company.put("CompanyCode", Companycode.get(i));
						Company.put("CompanyURL", Maps.get("CompanyURL") + promoterBuyingURL);
						System.out.println(Company);
						list.add(Company);

					}

				}
		}
		return list;

	}

	public static String getTodaysDate() {

		DateFormat dateformatter = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date();
		return dateformatter.format(date);

	}
	
	public static String getTodaysDate(String Formatt) {

		DateFormat dateformatter = new SimpleDateFormat(Formatt);
		Date date = new Date();
		return dateformatter.format(date);

	}

	public static HashMap<String, String> setDate(String FromDate, String ToDate) {

		if (FromDate.isEmpty()) {
			FromDate = getTodaysDate();
			if (ToDate.isEmpty()) {

				ToDate = getTodaysDate();

			}
		} else {

			if (ToDate.isEmpty()) {

				ToDate = getTodaysDate();

			}

		}
		HashMap<String, String> maps = new HashMap<>();
		maps.put("FromDate", FromDate);
		maps.put("ToDate", ToDate);
		return maps;
	}

	public List<HashMap<String, Object>> ResultsForTheData(String FromDate, String ToDate) {

		List<HashMap<String, Object>> listData = new ArrayList<>();
		String CompanyIDPath = "$..scrip_Code";
		String CompanyNamePath = "$..Long_Name";
		String CompanyURLPath = "$..URL";
		String CompanyMarketCapPath = "$..Mktcap";

		String finalURL = String.format(CompaniesResultsURL, FromDate, ToDate);
		
		response = RestAssured.given().when().baseUri(BaseURI).get(finalURL);
		String Jsonbody = response.getBody().asString();
		List<String> CompanyID = JsonPath.read(Jsonbody, CompanyIDPath);
		List<String> CompanyName = JsonPath.read(Jsonbody, CompanyNamePath);
		List<String> CompanyURL = JsonPath.read(Jsonbody, CompanyURLPath);
		for (int i = 0; i < CompanyID.size(); i++) {
			HashMap<String, Object> DataMap = new HashMap<>();
			String finalURLCompany = String.format(SpecificCompanyURL, CompanyID.get(i).toString());
			response = RestAssured.given().when().baseUri(BaseURI).get(finalURLCompany);
			Jsonbody = response.getBody().asString();
			List<String> CompanyMarketCap = JsonPath.read(Jsonbody, CompanyMarketCapPath);
			DataMap.put("CompanyID", CompanyID.get(i));
			DataMap.put("CompanyName", CompanyName.get(i));
			DataMap.put("CompanyURL", CompanyURL.get(i));
			String MarketCap=CompanyMarketCap.size()==0?"0":CompanyMarketCap.get(0);
			DataMap.put("CompanyMarketCap", MarketCap);
			listData.add(DataMap);
		}

		return listData;
	}

	public void NewsLetters(List<HashMap<String, Object>> ResultsCollection)
			throws  IOException {

		List<HashMap<String, Object>> NewslettersMap = new ArrayList<>();
		for(int i=0;i<ResultsCollection.size();i++) {
		String finalURL = String.format(NewsLinkURL, ResultsCollection.get(i).get("Company ID"));
		
		response = RestAssured.given().when().baseUri(BaseURI).get(finalURL);
		String Jsonbody = response.getBody().asString();
	    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("}\"", "}]");
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
        for(JSONObjectCalls jsonObjectCalls:root) {
        	
        	if(jsonObjectCalls.getNewsSubj().contains("Results"))
        		System.out.println(ResultsCollection.get(i).get("Company Name"));
        	
        }

		// }

	}
	}
	
	
	public  void Check(String URL)
			throws  IOException {

		String finalURL = String.format(NewsLinkURL, URL);
		
		response = RestAssured.given().when().baseUri(BaseURI).get(finalURL);
		String Jsonbody = response.getBody().asString();
	    Jsonbody=Jsonbody.replaceAll(".\"", "\"").replaceFirst("\"", "").replace("}\"", "}]");
	    Jsonbody=Jsonbody.replace("\\\"", "\"");
	    System.out.println(Jsonbody);
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        JSONObjectCalls[] root = mapper.readValue(Jsonbody, JSONObjectCalls[].class);
        for(JSONObjectCalls jsonObjectCalls:root) {
        	
        	System.out.println(jsonObjectCalls.getNewsSubj()+" "+jsonObjectCalls.getNewsid()+" "+jsonObjectCalls.getNewsdt());

	}
	}
	
	public static void main(String[] args) throws IOException {
		RestAssuredClass r=new RestAssuredClass();
		r.Check("504273");
		System.out.println(getTodaysDate("dd/MM/yyyy"));
	}
}
