package StockDailypublisher;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.CredentialCoder.Coder;
import com.RestAssured.RestPackage.RestAssuredClass;
import com.computaion.classes.ThreadPackage;

public class PromoterBuyingTest {
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static List<HashMap<String,Object>> PromoterBuying=new ArrayList<HashMap<String,Object>>();
	@Test(priority=-2)
	public static void fetchAllSocksData() {
		
		RestAssuredClass Rs=new RestAssuredClass();
		CompnayAllDataList=Rs.getAllCompaniesData();
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
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = StockPublishTest.class,enabled=true)
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps) {
		
		RestAssuredClass Rs=new RestAssuredClass();
		ThreadPackage.getInstance().setThreadLocal(Rs);
		PromoterBuying.addAll(ThreadPackage.getInstance().getThreadLocal().getPromoterBuyingData(Maps.get("Range1"), Maps.get("Range2"), CompnayAllDataList));
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
			+ "<th>NumberOfShares</th>"
			+"<th>AmountPurchased</th>";
	
	
	public String getAllDataAboutCompnay(){
		
		/*for(HashMap<String,Object> maps:PromoterBuying) {
		Set<Entry<String, Object>> mapsentry=maps.entrySet();
		for(Map.Entry<String, Object> map:mapsentry)
		  System.out.println(map.getKey()+"------------"+map.getValue());
		System.out.println("--------------------------------");
	
	}	*/
		
	String Body="<tbody>";
	for(HashMap<String,Object> Maps:PromoterBuying) {
			Body=Body+"<tr><td><a href=\""+Maps.get("CompanyURL")+"\">"+ Maps.get("CompanyName")+"</a></td>"+
					  "<td>"+ Maps.get("NumberOfShares")+"</td>"+
					  "<td>"+ Maps.get("AmountPurchased")+"</td></tr>"  ;}
					
	return CssSheet+Body+"</tbody></body></html>";
		
	}
	
	@Test(priority=5,enabled=true,dependsOnMethods = {"RunTesToFectStocks"})
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
	            htmlPart.setContent(getAllDataAboutCompnay(), "text/html; charset=utf-8" );

	            multipart.addBodyPart( htmlPart );
	            message.setContent( multipart );
	            
	            SimpleDateFormat dateFormat=new SimpleDateFormat("MM-dd-yyyy-HH-mm");
	            String date=dateFormat.format(new Date());
	            message.setSubject("Promoter Buying "+ date);
	            message.saveChanges();
	            Transport.send(message);


	        } catch (MessagingException e) {
	            throw new RuntimeException(e);
	        }
		
			
		}
	

}
