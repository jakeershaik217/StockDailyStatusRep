package StockDailypublisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.CredentialCoder.Coder;
import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.ExcelUtility;
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

	@DataProvider(name = "paralleltest", parallel = true)
	public Object[][] getMarketCapRange() {

		Object[][] HashMapData = new Object[7][1];
		HashMap<String, Integer> Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 0);
		Maps.put("Range2", 50);
		HashMapData[0][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 50);
		Maps.put("Range2", 100);
		HashMapData[1][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 100);
		Maps.put("Range2", 1000);
		HashMapData[2][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 1000);
		Maps.put("Range2", 10000);
		HashMapData[3][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 10000);
		Maps.put("Range2", 100000);
		HashMapData[4][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 100000);
		Maps.put("Range2", 1000000);
		HashMapData[5][0] = Maps;

		Maps = new HashMap<String, Integer>();
		Maps.put("Range1", 1000000);
		Maps.put("Range2", 0);
		HashMapData[6][0] = Maps;

		return HashMapData;

	}

	@Test(priority = -1, dataProvider = "paralleltest", dataProviderClass = LatestUpdatesFromStocks.class, enabled = true, invocationCount = 1)
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
	
	

	String CssSheet = "<style>\r\n" + "* {\r\n" + "  font-family: sans-serif; /* Change your font family */\r\n"
			+ "}\r\n" + "\r\n" + ".content-table {\r\n" + "  border-collapse: collapse;\r\n" + "  margin: 25px 0;\r\n"
			+ "  font-size: 0.9em;\r\n" + "  min-width: 400px;\r\n" + "  border-radius: 5px 5px 0 0;\r\n"
			+ "  overflow: hidden;\r\n" + "  box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\r\n" + "}\r\n" + "\r\n"
			+ ".content-table thead tr {\r\n" + "  background-color: #009879;\r\n" + "  color: #ffffff;\r\n"
			+ "  text-align: left;\r\n" + "  font-weight: bold;\r\n" + "}\r\n" + "\r\n" + ".content-table th,\r\n"
			+ ".content-table td {\r\n" + "  padding: 12px 15px;\r\n" + " border-left: 1px solid #a9a9a9;\r\n"
			+ " border-right: 1px solid #a9a9a9;\r\n" + "}\r\n" + "\r\n" + ".content-table tbody tr {\r\n"
			+ "  border-bottom: 1px solid #dddddd;\r\n" + "}\r\n" + "\r\n"
			+ ".content-table tbody tr:nth-of-type(even) {\r\n" + "  background-color: #f3f3f3;\r\n" + "}\r\n" + "\r\n"
			+ ".content-table tbody tr:last-of-type {\r\n" + "  border-bottom: 2px solid #a9a9a9;\r\n" + "}\r\n"
			+ "\r\n" + ".content-table tbody tr.active-row {\r\n" + "  font-weight: bold;\r\n" + "  color: #a9a9a9;\r\n"
			+ "}\r\n" + "" + "td,tr{\r\n" + " height : 2px;\r\n" + " border-collapse:collapse;\r\n"
			+ " border:1px solid #a9a9a9;\r\n" + " border-right : 1px solid #a9a9a9;\r\n"
			+ " border-left: 1px solid #a9a9a9;\r\n" + "\r\n" + "</style>\r\n"
			+ "<html><body><table class=\'content-table\' cellspacing=\'0\'><thead>" + "<tr>" + "<th>CompnayName</th>"
			+ "<th>Board Meeting Outcome</th>" + "<th>Dividend</th>" +  "<th>BuyBack</th>"+  "<th>Bounus</th>"+"<th>Date</th>"+"</tr></thead>";

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

	public void SendEmail() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			BadPaddingException, Exception {
		final String usernameEncode = "AES:s+Z/a55EmCfIzeb+lqd1GkgjlN/U1ueW8d+tJ+A/wIP8PBRQk405qLZksNhoD5tl";
                final String passwordEncode = "AES:YiJe10c7B36A9kpNBgb03w==";

                final String UserName = Coder.decode(usernameEncode);
                final String PassWord = Coder.decode(passwordEncode);

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.office365.com");
                props.put("mail.smtp.port", "587");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.debug", "true");
		
		

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(UserName, PassWord);
			}
		});

		try {

			Message message = new MimeMessage(session);
			Multipart multipart = new MimeMultipart("alternative");
			message.setFrom(new InternetAddress("shaik.jakeerhussain217@outlook.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("shaik.jakeerhussain217@outlook.com,shaikyounusshaik2@gmail.com"));
			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(EmailBody, "text/html; charset=utf-8");

			multipart.addBodyPart(htmlPart);
			message.setContent(multipart);

			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy-HH-mm");
			String date = dateFormat.format(new Date());
			message.setSubject("Stock News -  BuyBack,Divident,Bounus " + date);
			message.saveChanges();
			Transport.send(message);
			

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}
