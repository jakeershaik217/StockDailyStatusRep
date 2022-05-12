package StockDailypublisher;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.CredentialCoder.Coder;

public class emailTesting {
	
@Test
	
	public void SendEmail() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, Exception {
		
		Email email = new SimpleEmail();
		email.setHostName("smtp.googlemail.com");
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(Coder.decode("AES:s+Z/a55EmCfIzeb+lqd1Gm9NeLK/9oLTG21lMHCzFS7t8I86gdhTEOzwq7/z3WAk"), Coder.decode("AES:WgTPkK8AnpvT9thJUuQgKQ==")));
		email.setSSLOnConnect(true);
		email.setFrom("shaik.jakeerhussain217@gmail.com");
		email.setSubject("Best Performing Stock");
		email.setContent("Jakeer","text/html");
		email.addTo("shaik.jakeerhussain217@outlook.com");
		email.send();
		
	}


}
