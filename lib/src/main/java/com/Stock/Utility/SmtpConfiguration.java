package com.Stock.Utility;

public final class SmtpConfiguration {

	private final String username;
	private final String password;
	private final String recipients;

	private SmtpConfiguration(String username, String password, String recipients) {
		this.username = username;
		this.password = password;
		this.recipients = recipients;
	}

	public static SmtpConfiguration fromEnvironment() {
		return new SmtpConfiguration(
				requireEnvironmentVariable("SMTP_USERNAME"),
				requireEnvironmentVariable("SMTP_PASSWORD"),
				requireEnvironmentVariable("SMTP_RECIPIENTS"));
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getRecipients() {
		return recipients;
	}

	private static String requireEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " must be set");
		}
		return value;
	}
}
