package StockDailypublisher;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.MarketCapRangeProvider;
import com.computaion.classes.ThreadPackage;



public class StockPublishTest {
	
	private static final double Slab=4.98;
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static List<HashMap<String,Double>> CompnayPecentageList=new ArrayList<HashMap<String,Double>>();
	private List<HashMap<String,Object>> CompnayFinalData=new ArrayList<HashMap<String,Object>>();
	String Body="<tbody>";
	@Test(priority=-2)
	public static void fetchAllSocksData() {
		
		RestAssuredClass Rs=new RestAssuredClass();
		CompnayAllDataList=Rs.getAllCompaniesData();
		CompnayAllDataList.stream().forEach(System.out::println);
		//CompnayAllDataList.forEach(l -> l.forEach((k,v) ->System.out.println(k+" "+v)));
	}
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = MarketCapRangeProvider.class,enabled=true )
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps) {
		
		RestAssuredClass Rs=new RestAssuredClass();
		ThreadPackage.getInstance().setThreadLocal(Rs);
		CompnayPecentageList.add(ThreadPackage.getInstance().getThreadLocal().getprecentageChange(Maps.get("Range1"), Maps.get("Range2"), CompnayAllDataList, Slab));
	}
	
	@Test(priority=0)
	public void getAllDataAboutCompnay(){
		
		for(HashMap<String,Object> Maps:CompnayAllDataList)
			  for(HashMap<String,Double> MapsPercentage:CompnayPecentageList) 
				  for(String Key:MapsPercentage.keySet()) 
					if(Maps.get("CompanyName").equals(Key)) {
					HashMap<String, Object> dummyMap=new HashMap<>();	
					dummyMap.put("Company Name", Maps.get("CompanyName"));
					dummyMap.put("Company URL", Maps.get("CompanyURL"));
					dummyMap.put("Company ID", Maps.get("CompanyID"));
					dummyMap.put("Percentage Change", MapsPercentage.get(Key));
					CompnayFinalData.add(dummyMap);
					}
				  
	}
	
	String CssSheet=EmailStyles.tableHeader("CompnayName", "ChangeInPercentage", "NameBSE");
	
	public  String composeEmailBody() {
		
		List<String> SlabList=new ArrayList<>(Arrays.asList(new String[] {"4.99","4.98","5.0","9.99","10.0","19.99","20.0"}));
		CompnayFinalData=CompnayFinalData.stream().sorted((i1,i2) ->{
			
			Double d1=(i1.get("Percentage Change")+"").isEmpty()?0.00:Double.parseDouble(i1.get("Percentage Change")+"");
			Double d2=(i2.get("Percentage Change")+"").isEmpty()?0.00:Double.parseDouble(i2.get("Percentage Change")+"");
			return (d1>d2)?-1:(d1<d2)?+1:0;
			
		}).collect(Collectors.toList());
		CompnayFinalData.stream().forEach(System.out::println);
		CompnayFinalData.stream().filter(i -> SlabList.contains(i.get("Percentage Change")+"")).collect(Collectors.toList())
		.stream().forEach(i -> {Body=Body+"<tr><td bgcolor = \"#4CAF50\"><a href=\"https://www.google.com/finance/quote/"+i.get("Company ID")+":BOM?hl=en\">"+i.get("Company Name")+"</a></td><td bgcolor = \"#4CAF50\" >"+i.get("Percentage Change")+"</td><td bgcolor = \"#4CAF50\"><a href=\""+i.get("Company URL")+"\">"+i.get("Company Name")+"</a></td></tr>";});
		
		CompnayFinalData.stream().filter(i -> !SlabList.contains(i.get("Percentage Change")+"")).collect(Collectors.toList())
		.stream().forEach(
		i -> {Body=Body+"<tr><td><a href=\"https://www.google.com/finance/quote/"+i.get("Company ID")+":BOM?hl=en\">"+i.get("Company Name")+"</a></td><td>"+i.get("Percentage Change")+"</td><td><a href=\""+i.get("Company URL")+"\">"+i.get("Company Name")+"</a></td></tr>";});
		
	/*	for(HashMap<String,Object> Maps:CompnayFinalData) 
			if(SlabList.contains(Maps.get("Percentage Change")+""))
				Body=Body+"<tr><td bgcolor = \"#4CAF50\"><a href=\"https://www.google.com/finance/quote/"+Maps.get("Company ID")+":BOM?hl=en\">"+Maps.get("Company Name")+"</a></td><td bgcolor = \"#4CAF50\" >"+Maps.get("Percentage Change")+"</td></tr>";
			else
				Body=Body+"<tr><td><a href=\"https://www.google.com/finance/quote/"+Maps.get("Company ID")+":BOM?hl=en\">"+Maps.get("Company Name")+"</a></td><td>"+Maps.get("Percentage Change")+"</td></tr>";
*/
		return CssSheet+Body+"</tbody></body></html>";
		
	}
	@Test(priority=5)
	
	public void SendEmail() throws Exception {
		EmailSender.sendHtmlEmail("Daily_StockRun", composeEmailBody());
	}

}
