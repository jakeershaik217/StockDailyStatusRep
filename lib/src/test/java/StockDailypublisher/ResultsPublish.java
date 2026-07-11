package StockDailypublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.ExcelUtility;
import com.Stock.Utility.MarketCapRangeProvider;
import com.computaion.classes.ThreadPackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ResultsPublish {

	private static List<HashMap<String, Object>> CompnayAllDataList;
	private static String SheetName = "CompanyData";
	private static String ExcelDataBasePath=System.getProperty("user.dir")+"\\src\\test\\resources\\Results.xlsx";
	private static List<HashMap<String, String>> CompnayDataListClone = new ArrayList<HashMap<String,  String>>();
	private static List<HashMap<String, String>> CompnayDataList = new ArrayList<HashMap<String, String>>();
	private static List<HashMap<String, String>> CompnayDataCustm = new ArrayList<>();
	private static HashMap<String, String> CompnayandURL = new HashMap<>();
	private static List<String> Compnaylist = new ArrayList<>();
	ExcelUtility excelUtility;
	String Body = "<tbody>";
	String EmailBody="";
	private static HashMap<String,String> DatesMap;

	
	@BeforeTest
	public void cleanExcelDatabase() throws IOException {
		excelUtility=new ExcelUtility(ExcelDataBasePath,SheetName);
		String Flag=excelUtility.getDateFromExcelDatabase();
		if(!Flag.equals(RestAssuredClass.getTodaysDate("dd/MM/yyyy")) && !Flag.isEmpty() )
        	excelUtility.cleanExcelDatabase();
	
	}
	@Parameters({"FromDate","ToDate"})
	@Test(priority = -2)
	public static void fetchAllSocksData(String FromDate,String ToDate) {

		DatesMap=RestAssuredClass.setDate(FromDate, ToDate);
		RestAssuredClass restAssured=new RestAssuredClass();
		CompnayAllDataList=restAssured.ResultsForTheData(DatesMap.get("FromDate"), DatesMap.get("ToDate"));
		//CompnayAllDataList.forEach(k -> StaticVariableCollection.Results.add(k));
	}

	@Test(priority = -1, dataProvider = "paralleltest", dataProviderClass = MarketCapRangeProvider.class, enabled = true, invocationCount = 1)
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps)
			throws JsonMappingException, JsonProcessingException {

		RestAssuredClass Rs = new RestAssuredClass();
		ThreadPackage.getInstance().setThreadLocal(Rs);
		CompnayDataList.add(ThreadPackage.getInstance().getThreadLocal().getResultsDetails(Maps.get("Range1"),
				Maps.get("Range2"), CompnayAllDataList));
		
	}

	
	@Test(priority = 0)
	public static void runs() {
		CompnayDataListClone.addAll(CompnayDataList);
		for (HashMap<String, String> maps : CompnayDataList)
			if (maps.keySet().isEmpty())
				CompnayDataListClone.remove(maps);
		
		System.out.println(CompnayDataListClone);
		for(HashMap<String, String> maps : CompnayDataList)
			for(String key : maps.keySet())
			    Compnaylist.add(key);

		
	}
	
	@Test(priority=1)
	public  void DataInExcelDB() throws IOException {
		
				
		List<String> ExcelDataMap=excelUtility.readExcelasList();
		List<String> CompnayDataCustmClone=new ArrayList<>(Compnaylist);
		if(ExcelDataMap.size()>=CompnayDataCustmClone.size()) {
			for(String  ele:CompnayDataCustmClone)
				if(ExcelDataMap.contains(ele))
					Compnaylist.remove(ele);
		}else if(ExcelDataMap.size()<CompnayDataCustmClone.size()) {
			Compnaylist.removeAll(ExcelDataMap);
		}
		if(!Compnaylist.isEmpty())
		excelUtility.setValuestoExcelasList( Compnaylist);
		
		

	}
	
	@Test(priority=3,enabled=false)
	public static void CompanyURL() throws IOException {
		for(HashMap<String, String> Data:CompnayDataCustm)
		     for(HashMap<String, Object> maps:CompnayAllDataList)
		    	 if(maps.get("CompanyName").equals(Data.get("CompanyName")))
			   CompnayandURL.put(Data.get("CompanyName"), (String)maps.get("CompanyURL"));
	}
	
	

	String CssSheet = EmailStyles.tableHeader("CompnayName", "Board Meeting Outcome", "Dividend",
			"BuyBack", "Bounus", "Date");

	@Test(priority=4,enabled=false)
	public void composeEmailBody() {

		if(CompnayDataCustm.isEmpty())
			throw new SkipException("No Data to publish");
		else {
		CompnayDataCustm.stream().forEach(i -> {
					Body = Body + "<tr><td bgcolor = \"#4CAF50\"><a href=\""+CompnayandURL.get(i.get("CompanyName")) +"\">" + i.get("CompanyName")
							+ "</a></td><td bgcolor = \"#4CAF50\" >" + i.get("Board Meeting Outcome")
							+ "</td><td bgcolor = \"#4CAF50\">" + i.get("Dividend") 
							+ "</td><td bgcolor = \"#4CAF50\">" + i.get("Buyback") 
							+ "</td><td bgcolor = \"#4CAF50\">" + i.get("Bounus") 
							+ "</td><td bgcolor = \"#4CAF50\">" + i.get("Date") 
							+ "</td></tr>";
				});

		/*
		 * for(HashMap<String,Object> Maps:CompnayFinalData)
		 * if(SlabList.contains(Maps.get("Percentage Change")+"")) Body=
		 * Body+"<tr><td bgcolor = \"#4CAF50\"><a href=\"https://www.google.com/finance/quote/"
		 * +Maps.get("Company ID")+":BOM?hl=en\">"+Maps.get("Company Name"
		 * )+"</a></td><td bgcolor = \"#4CAF50\" >"+Maps.get("Percentage Change")+
		 * "</td></tr>"; else
		 * Body=Body+"<tr><td><a href=\"https://www.google.com/finance/quote/"+Maps.
		 * get("Company ID")+":BOM?hl=en\">"+Maps.get("Company Name")+"</a></td><td>"+
		 * Maps.get("Percentage Change")+"</td></tr>";
		 */
		EmailBody= CssSheet + Body + "</tbody></body></html>";
		}
	}

	@Test(priority = 5,dependsOnMethods = {"composeEmailBody"},enabled=false)

	public void SendEmail() throws Exception {
		EmailSender.sendHtmlEmail("Stock News -  BuyBack,Divident,Bounus", EmailBody);
	}

}
