package com.Stock.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

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

import com.CredentialCoder.Coder;

/**
 * Sends the daily HTML report emails. This consolidates the SMTP session setup
 * and message-building logic that every publisher class used to duplicate.
 */
public class EmailSender {

	private static final String USERNAME_ENCODED = "AES:s+Z/a55EmCfIzeb+lqd1GkgjlN/U1ueW8d+tJ+A/wIP8PBRQk405qLZksNhoD5tl";
	private static final String PASSWORD_ENCODED = "AES:YiJe10c7B36A9kpNBgb03w==";
	private static final String FROM = "shaik.jakeerhussain217@outlook.com";
	private static final String TO = "shaik.jakeerhussain217@outlook.com,shaikyounusshaik2@gmail.com";

	/**
	 * Sends an HTML email whose subject is {@code subjectPrefix} followed by the
	 * current timestamp, matching the previous per-class behaviour.
	 */
	public static void sendHtmlEmail(String subjectPrefix, String htmlBody) throws Exception {
		sendHtmlEmail(subjectPrefix, htmlBody, null);
	}

	/**
	 * Same as {@link #sendHtmlEmail(String, String)} but merges {@code extraProps}
	 * on top of the base SMTP configuration (e.g. additional SSL settings).
	 */
	public static void sendHtmlEmail(String subjectPrefix, String htmlBody, Properties extraProps) throws Exception {

		final String UserName = Coder.decode(USERNAME_ENCODED);
		final String PassWord = Coder.decode(PASSWORD_ENCODED);

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.office365.com");
		props.put("mail.smtp.port", "587");
		if (extraProps != null) {
			props.putAll(extraProps);
		}

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(UserName, PassWord);
			}
		});

		try {

			Message message = new MimeMessage(session);
			Multipart multipart = new MimeMultipart("alternative");
			message.setFrom(new InternetAddress(FROM));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO));
			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

			multipart.addBodyPart(htmlPart);
			message.setContent(multipart);

			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy-HH-mm");
			String date = dateFormat.format(new Date());
			message.setSubject(subjectPrefix + " " + date);
			message.saveChanges();
			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
