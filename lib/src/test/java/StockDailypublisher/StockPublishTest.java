package StockDailypublisher;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
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
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = StockPublishTest.class,enabled=true )
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
			+ "<th>CompnayName</th>"
			+ "<th>ChangeInPercentage</th>"
			+ "<th>NameBSE</th>"
			+"</tr></thead>";
	
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
            message.setRecipients(Message. RecipientType.TO,
                InternetAddress.parse("shaik.jakeerhussain217@outlook.com,shaikyounusshaik2@gmail.com"));
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent( composeEmailBody(), "text/html; charset=utf-8" );

            multipart.addBodyPart( htmlPart );
            message.setContent( multipart );
            
            SimpleDateFormat dateFormat=new SimpleDateFormat("MM-dd-yyyy-HH-mm");
            String date=dateFormat.format(new Date());
            message.setSubject("Daily_StockRun "+ date);
            message.saveChanges();
            Transport.send(message);


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
	}

}
