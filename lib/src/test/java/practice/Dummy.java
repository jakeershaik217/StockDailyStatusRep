package practice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

public class Dummy {
	
	@Test(priority=2,enabled=false)
	public void t1() {
		 String input = "pm.expect(Actual.Resp)";

	        // Define the regex pattern
	        String pattern = "pm.expect\\((.*?)\\)";

	        // Create the Pattern object
	        Pattern regex = Pattern.compile(pattern);

	        // Create the Matcher object
	        Matcher matcher = regex.matcher(input);

	        // Find the match and extract Actual.Resp
	        if (matcher.find()) {
	            String actualResp = matcher.group(1);
	            System.out.println("Actual.Resp: " + actualResp);
	        }
	    }
	@Test(priority=2)
	public void Sample() {
		
		String Str="Tier 1\r\n"
				+ "manufacturers in India with a presence\r\n"
				+ "in Indonesia, Vietnam, Uzbekistan and\r\n"
				+ "Japan. The Company has a diversified\r\n"
				+ "customer base, including Indian and\r\n"
				+ "global original equipment manufacturers\r\n"
				+ "and Tier-1 customers.";
		Arrays.asList(Str.split("\r\n")).forEach(k -> System.out.print(k));
		
	}

}
