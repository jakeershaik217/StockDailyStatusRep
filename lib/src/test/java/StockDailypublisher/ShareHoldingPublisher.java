package StockDailypublisher;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.MarketCapRangeProvider;
import com.computaion.classes.ShareHoldingChange;
import com.computaion.classes.ThreadPackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;




public class ShareHoldingPublisher {
	
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static List<HashMap<String,Map<String,Double>>> CompnayPecentageList=new ArrayList<HashMap<String,Map<String,Double>>>();
	public List<HashMap<String,Object>> CompnayFinalData=new ArrayList<HashMap<String,Object>>();
	@Test(priority=-2)
	public static void fetchAllSocksData() {
		
		RestAssuredClass Rs=new RestAssuredClass();
		CompnayAllDataList=Rs.getAllCompaniesData();
	}
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = MarketCapRangeProvider.class,enabled=true,dependsOnMethods = {"fetchAllSocksData"} )
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps) throws JsonMappingException, JsonProcessingException {
		
		ShareHoldingChange Rs=new ShareHoldingChange();
		ThreadPackage.getInstance().setThreadLocalShareHolding(Rs);
		CompnayPecentageList.add(ThreadPackage.getInstance().getThreadLocalShareHolding().getJsonDataFromAStock(Maps.get("Range1"), Maps.get("Range2"), CompnayAllDataList));
	}
	
	@Test(priority=0)
	public void getAllDataAboutCompnay(){
		
		for(HashMap<String,Object> Maps:CompnayAllDataList)
			  for(HashMap<String,Map<String,Double>> MapsPercentage:CompnayPecentageList) 
				  for(String Key:MapsPercentage.keySet()) 
					if(Maps.get("CompanyName").equals(Key)) {
					HashMap<String, Object> dummyMap=new HashMap<>();	
					dummyMap.put("Company Name", Key);
					dummyMap.put("Promoters", MapsPercentage.get(Key).get("Promoters"));
					dummyMap.put("FIIs",  MapsPercentage.get(Key).get("FIIs"));
					dummyMap.put("Mutual Funds",  MapsPercentage.get(Key).get("Mutual Funds"));
					dummyMap.put("Insurance",  MapsPercentage.get(Key).get("Insurance"));
					dummyMap.put("Other DIIs",  MapsPercentage.get(Key).get("Other DIIs"));
					dummyMap.put("Non Inst.",  MapsPercentage.get(Key).get("Non Inst."));
					dummyMap.put("Others",  MapsPercentage.get(Key).get("Others"));
					CompnayFinalData.add(dummyMap);
					}
		
		Collections.sort(CompnayFinalData,new comparatorClass());		  
	}
	
	String CssSheet=EmailStyles.tableHeader("Compnay Name", "Promoters", "FIIs", "Mutual Funds",
			"Insurance", "Other DIIs", "Non Inst.", "Others");
	
	
	public String composeEmailBody() {
		String Body="<tbody>";
		for(HashMap<String,Object> Maps:CompnayFinalData) 
				Body=Body+"<tr><td>"+ Maps.get("Company Name")+"</td>"+
						  "<td>"+ Maps.get("Promoters")+"</td>"+
						  "<td>"+ Maps.get("FIIs")+"</td>"+
						  "<td>"+ Maps.get("Mutual Funds")+"</td>"+
						  "<td>"+ Maps.get("Insurance")+"</td>"+
						  "<td>"+ Maps.get("Other DIIs")+"</td>"+
						  "<td>"+ Maps.get("Non Inst.")+"</td>"+
						  "<td>"+ Maps.get("Others")+"</td></tr>";

		return CssSheet+Body+"</tbody></body></html>";
		
	}
	@Test(priority=5,enabled=true,dependsOnMethods = {"RunTesToFectStocks"})
	
	public void SendEmail() throws Exception {
		EmailSender.sendHtmlEmail("Stock Holding", composeEmailBody());
	}

}