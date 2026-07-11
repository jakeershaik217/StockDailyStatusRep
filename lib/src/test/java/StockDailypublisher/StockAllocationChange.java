package StockDailypublisher;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.MarketCapRangeProvider;
import com.computaion.classes.ShareHoldingPercentageChange;
import com.computaion.classes.ThreadPackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;



public class StockAllocationChange{
	
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static List<HashMap<String,Map<String,Object>>> CompnayPecentageList=new ArrayList<HashMap<String,Map<String,Object>>>();
	private List<HashMap<String,Object>> CompnayFinalData=new ArrayList<HashMap<String,Object>>();
	@Test(priority=-2)
	public static void fetchAllSocksData() {
		
		RestAssuredClass Rs=new RestAssuredClass();
		CompnayAllDataList=Rs.getAllCompaniesData();
	}
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = MarketCapRangeProvider.class,enabled=true,dependsOnMethods = {"fetchAllSocksData"} )
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps) throws JsonMappingException, JsonProcessingException {
		
		ShareHoldingPercentageChange Rs=new ShareHoldingPercentageChange();
		ThreadPackage.getInstance().setThreadLocalSharePChange(Rs);
		CompnayPecentageList.add(ThreadPackage.getInstance().getThreadLocalSharePchange().getStockPercentageChange(Maps.get("Range1"), Maps.get("Range2"), CompnayAllDataList));
	}
	
	@Test(priority=0)
	public void getAllDataAboutCompnay(){
		
		for(HashMap<String,Object> Maps:CompnayAllDataList)
			  for(HashMap<String,Map<String,Object>> MapsPercentage:CompnayPecentageList) 
				  for(String Key:MapsPercentage.keySet()) 
					if(Maps.get("CompanyName").equals(Key)) {
					HashMap<String, Object> dummyMap=new HashMap<>();	
					dummyMap.put("Company Name", Key);
					dummyMap.put("Promoters", MapsPercentage.get(Key).get("Promoters"));
					dummyMap.put("FIIs", MapsPercentage.get(Key).get("FIIs"));
					dummyMap.put("MutualFunds", MapsPercentage.get(Key).get("MutualFunds"));
					dummyMap.put("Insurance", MapsPercentage.get(Key).get("Insurance"));
					dummyMap.put("OtherDIIs", MapsPercentage.get(Key).get("OtherDIIs"));
					dummyMap.put("NonInst", MapsPercentage.get(Key).get("NonInst"));
					dummyMap.put("Others", MapsPercentage.get(Key).get("Others"));
					CompnayFinalData.add(dummyMap);
					}
		CompnayFinalData=CompnayFinalData.stream().sorted((i1,i2) -> {
			

			
			double d1= Double.parseDouble(i1.get("FIIs")+"");
			double d2= Double.parseDouble(i2.get("FIIs")+"");
			if(d1>d2)
				return -1;
			else if(d1<d2)
				return 1;
			else
				return 0;
		
		}).collect(Collectors.toList());
		//Collections.sort(CompnayFinalData,new comparatorClass());
		//Collections.sort(CompnayFinalData,new comparatorClass2());
				  
	}
	
	String CssSheet=EmailStyles.tableHeader("Compnay Name", "Promoters", "FIIs", "Mutual Funds",
			"Insurance", "Other DIIs", "Non Inst.", "Others");
	
	
	public String composeEmailBody() {
		String Body="<tbody>";
		for(HashMap<String,Object> Maps:CompnayFinalData) 
				Body=Body+"<tr><td>"+ Maps.get("Company Name")+"</td>"+
						  "<td>"+ Maps.get("Promoters")+"</td>"+
						  "<td>"+ Maps.get("FIIs")+"</td>"+
						  "<td>"+ Maps.get("MutualFunds")+"</td>"+
						  "<td>"+ Maps.get("Insurance")+"</td>"+
						  "<td>"+ Maps.get("OtherDIIs")+"</td>"+
						  "<td>"+ Maps.get("NonInst")+"</td>"+
						  "<td>"+ Maps.get("Others")+"</td></tr>";

		return CssSheet+Body+"</tbody></body></html>";
		
	}
	@Test(priority=5,dependsOnMethods = {"RunTesToFectStocks"} )
	
	public void SendEmail() throws Exception {
		EmailSender.sendHtmlEmail("Stock Allocation Change", composeEmailBody());
	}


}