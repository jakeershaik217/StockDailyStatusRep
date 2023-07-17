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
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.CredentialCoder.Coder;
import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.StaticVariableCollection;
import com.computaion.classes.ThreadPackage;



public class ResultsDay {
	
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static HashMap<String,String> DatesMap;
	String Body="<tbody>";
	@Test
	@Parameters({"FromDate","ToDate"})
	public static void getResultsFortheDay(String FromDate,String ToDate) {
		
		DatesMap=RestAssuredClass.setDate(FromDate, ToDate);
		RestAssuredClass restAssured=new RestAssuredClass();
		CompnayAllDataList=restAssured.ResultsForTheData(DatesMap.get("FromDate"), DatesMap.get("ToDate"));
		CompnayAllDataList.forEach(k -> StaticVariableCollection.Results.add(k));
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
			+ "<th>Name BSE</th>"
			+"</tr></thead>";
	
	public  String composeEmailBody() {
		
		
		CompnayAllDataList.stream().forEach(i -> {Body=Body+"<tr><td bgcolor = \"#4CAF50\"><a href=\"https://www.google.com/finance/quote/"+i.get("Company ID")+":BOM?hl=en\">"+i.get("Company Name")+"</a></td><td bgcolor = \"#4CAF50\"><a href=\""+i.get("Company URL")+"\">"+i.get("Company Name")+"</a></td></tr>";});
		
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
            message.setSubject("Results From:"+ DatesMap.get("FromDate")+" To:"+DatesMap.get("ToDate"));
            message.saveChanges();
            Transport.send(message);


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
	}

}
