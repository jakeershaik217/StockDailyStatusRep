package StockDailypublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.ExcelUtility;
import com.Stock.Utility.MarketCapRangeProvider;
import com.Stock.Utility.StaticVariableCollection;
import com.computaion.classes.ThreadPackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class LatestUpdatesFromStocks {

	private static List<HashMap<String, Object>> CompnayAllDataList;
	private static int NumberOfColumns = 6;
	private static String SheetName = "CompanyData";
	//private static String ExcelDataBasePath=System.getProperty("user.dir")+"\\src\\test\\resources\\Database.xlsx";
	private static String ExcelDataBasePath="src/test/resources/Database.xlsx";
	private static List<HashMap<String, HashMap<String, String>>> CompnayDataList = new ArrayList<HashMap<String, HashMap<String, String>>>();
	private static List<HashMap<String, HashMap<String, String>>> CompnayDataListClone = new ArrayList<HashMap<String, HashMap<String, String>>>();
	private static List<HashMap<String, String>> CompnayDataCustm = new ArrayList<>();
	private static HashMap<String, String> CompnayandURL = new HashMap<>();
	ExcelUtility excelUtility;
	String Body = "<tbody>";
	String EmailBody="";

	
	@BeforeTest
	public void cleanExcelDatabase() throws IOException {
		excelUtility=new ExcelUtility(ExcelDataBasePath,SheetName);
		String Flag=excelUtility.getDateFromExcelDatabase();
		if(!Flag.equals(RestAssuredClass.getTodaysDate("dd/MM/yyyy")) && !Flag.isEmpty() )
        	excelUtility.cleanExcelDatabase();
	
	}
	@Test(priority = -2)
	public static void fetchAllSocksData() {

		RestAssuredClass Rs = new RestAssuredClass();
		CompnayAllDataList = Rs.getAllCompaniesData();
		CompnayAllDataList.stream().forEach(System.out::println);
	}

	@Test(priority = -1, dataProvider = "paralleltest", dataProviderClass = MarketCapRangeProvider.class, enabled = true, invocationCount = 1)
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps)
			throws JsonMappingException, JsonProcessingException {

		RestAssuredClass Rs = new RestAssuredClass();
		ThreadPackage.getInstance().setThreadLocal(Rs);
		CompnayDataList.add(ThreadPackage.getInstance().getThreadLocal().getNewsDetails(Maps.get("Range1"),
				Maps.get("Range2"), CompnayAllDataList));
	}

	@Test(priority = 0)
	public static void runs() {
		CompnayDataListClone.addAll(CompnayDataList);
		for (HashMap<String, HashMap<String, String>> maps : CompnayDataList)
			if (maps.keySet().isEmpty())
				CompnayDataListClone.remove(maps);
		for (HashMap<String, HashMap<String, String>> CompanyDataMap : CompnayDataListClone)
			for (String KeyName : CompanyDataMap.keySet()) {
				Set<String> CompanyParamtersKeys = CompanyDataMap.get(KeyName).keySet();
				Set<String> RefParameters = new HashSet<>(Arrays.asList(StaticVariableCollection.KeywordsReferrence));
				RefParameters.removeAll(CompanyParamtersKeys);
				for (String Param : RefParameters)
					CompanyDataMap.get(KeyName).put(Param, "NO");

			}

		for (HashMap<String, HashMap<String, String>> CompanyDataMap : CompnayDataListClone)
			for (String KeyName : CompanyDataMap.keySet()) {
				HashMap<String, String> tempmap = new HashMap<>();
				tempmap.put("CompanyName", KeyName);
				tempmap.put("Date", ""+RestAssuredClass.getTodaysDate("dd/MM/yyyy"));
				for (String Key : CompanyDataMap.get(KeyName).keySet())
					tempmap.put(Key, CompanyDataMap.get(KeyName).get(Key));
				CompnayDataCustm.add(tempmap);
			}
		

		
	}

	@Test(priority=1)
	public  void DataInExcelDB() throws IOException {
		
		List<HashMap<String,String>> ExcelDataMap=excelUtility.readExcel();
		List<HashMap<String,String>> CompnayDataCustmClone=new ArrayList<>(CompnayDataCustm);
		if(ExcelDataMap.size()>=CompnayDataCustmClone.size()) {
			for(HashMap<String,String>  ele:CompnayDataCustmClone)
				if(ExcelDataMap.contains(ele))
					CompnayDataCustm.remove(ele);
		}else if(ExcelDataMap.size()<CompnayDataCustmClone.size()) {
			CompnayDataCustm.removeAll(ExcelDataMap);
		}
		if(!CompnayDataCustm.isEmpty())
		excelUtility.setValuestoExcel(NumberOfColumns, CompnayDataCustm);
		
		

	}
	
	@Test(priority=3)
	public static void CompanyURL() throws IOException {
		for(HashMap<String, String> Data:CompnayDataCustm)
		     for(HashMap<String, Object> maps:CompnayAllDataList)
		    	 if(maps.get("CompanyName").equals(Data.get("CompanyName")))
			   CompnayandURL.put(Data.get("CompanyName"), (String)maps.get("CompanyURL"));
	}
	
	

	String CssSheet = EmailStyles.tableHeader("CompnayName", "Board Meeting Outcome", "Dividend",
			"BuyBack", "Bounus", "Date");

	@Test(priority=4)
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

	@Test(priority = 5,dependsOnMethods = {"composeEmailBody"})

	public void SendEmail() throws Exception {
		Properties extraProps = new Properties();
		extraProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		extraProps.put("mail.smtp.ssl.trust", "smtp.office365.com");
		extraProps.put("mail.debug", "true");
		extraProps.put("mail.smtp.socketFactory.port", "587");
		EmailSender.sendHtmlEmail("Stock News -  BuyBack,Divident,Bounus", EmailBody, extraProps);
	}

}
