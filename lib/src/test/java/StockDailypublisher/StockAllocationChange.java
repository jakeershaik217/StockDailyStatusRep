package StockDailypublisher;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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


import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.CredentialCoder.Coder;
import com.RestAssured.RestPackage.RestAssuredClass;
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
	
	@DataProvider(name = "paralleltest",parallel=true)
	public Object[][] getMarketCapRange(){
		
		Object[][] HashMapData=new Object[7][1];
		HashMap<String, Integer> Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 0);
		Maps.put("Range2", 50);
		HashMapData[0][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 50);
		Maps.put("Range2", 100);
	    HashMapData[1][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 100);
		Maps.put("Range2", 1000);
		HashMapData[2][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 1000);
		Maps.put("Range2", 10000);
	 	HashMapData[3][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 10000);
		Maps.put("Range2", 100000);
		HashMapData[4][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 100000);
		Maps.put("Range2", 1000000);
		HashMapData[5][0]=Maps;
		
		Maps=new HashMap<String, Integer>();
		Maps.put("Range1", 1000000);
		Maps.put("Range2", 0);
		HashMapData[6][0]=Maps;
		
		return HashMapData;
		
		
	}
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = StockAllocationChange.class,enabled=true,dependsOnMethods = {"fetchAllSocksData"} )
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
	
	String CssSheet="<style>\r\n"
			+ "* {\r\n"
			+ "  font-family: sans-serif; /* Change your font family */\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table {\r\n"
			+ "  border-collapse: collapse;\r\n"
			+ "  margin: 25px 0;\r\n"
			+ "  font-size: 0.9em;\r\n"
			+ "  min-width: 400px;\r\n"
			+ "  border-radius: 5px 5px 0 0;\r\n"
			+ "  overflow: hidden;\r\n"
			+ "  box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table thead tr {\r\n"
			+ "  background-color: #009879;\r\n"
			+ "  color: #ffffff;\r\n"
			+ "  text-align: left;\r\n"
			+ "  font-weight: bold;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table th,\r\n"
			+ ".content-table td {\r\n"
			+ "  padding: 12px 15px;\r\n"
			+ " border-left: 1px solid #a9a9a9;\r\n"
			+ " border-right: 1px solid #a9a9a9;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr {\r\n"
			+ "  border-bottom: 1px solid #dddddd;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr:nth-of-type(even) {\r\n"
			+ "  background-color: #f3f3f3;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr:last-of-type {\r\n"
			+ "  border-bottom: 2px solid #a9a9a9;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr.active-row {\r\n"
			+ "  font-weight: bold;\r\n"
			+ "  color: #a9a9a9;\r\n"
			+ "}\r\n"
			+ ""
			+ "td,tr{\r\n"
			+ " height : 2px;\r\n"
			+" border-collapse:collapse;\r\n"
			+" border:1px solid #a9a9a9;\r\n"
			+" border-right : 1px solid #a9a9a9;\r\n"
			+" border-left: 1px solid #a9a9a9;\r\n"
			+"\r\n"
			+"</style>\r\n"
			+"<html><body><table class=\'content-table\' cellspacing=\'0\'><thead>"
			+ "<tr>"
			+ "<th>Compnay Name</th>"
			+ "<th>Promoters</th>"
			+"<th>FIIs</th>"
			+ "<th>Mutual Funds</th>"
			+"<th>Insurance</th>>"
			+ "<th>Other DIIs</th>"
			+"<th>Non Inst.</th>"
			+ "<th>Others</th>";
	
	
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
	
	public void SendEmail() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, Exception {
		

		final String usernameEncode = "AES:s+Z/a55EmCfIzeb+lqd1GkgjlN/U1ueW8d+tJ+A/wIP8PBRQk405qLZksNhoD5tl";
        final String passwordEncode = "AES:YiJe10c7B36A9kpNBgb03w==";
        
        final String UserName=Coder.decode(usernameEncode);
        final String PassWord=Coder.decode(passwordEncode);

        

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.office365.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(UserName, PassWord);
            }
          });

        try {

            Message message = new MimeMessage(session);
            Multipart multipart = new MimeMultipart( "alternative" );
            message.setFrom(new InternetAddress("shaik.jakeerhussain217@outlook.com"));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse("shaik.jakeerhussain217@outlook.com,shaikyounusshaik2@gmail.com"));
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent( composeEmailBody(), "text/html; charset=utf-8" );

            multipart.addBodyPart( htmlPart );
            message.setContent( multipart );
            
            SimpleDateFormat dateFormat=new SimpleDateFormat("MM-dd-yyyy-HH-mm");
            String date=dateFormat.format(new Date());
            message.setSubject("Stock Allocation Change "+ date);
            message.saveChanges();
            Transport.send(message);


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
	
		
	}


}