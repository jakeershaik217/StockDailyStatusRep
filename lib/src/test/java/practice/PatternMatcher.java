package practice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatcher {
    public static void main(String[] args) {
        String inputString = "Name is Jakeer @Mapper(target = \"JakeerHussain\", source = \"HussainShaik\")   Name is Hussain @Mapper(target = \"JakeerHussain\")";

        Pattern pattern = Pattern.compile("@Mapper\\(target = \"([a-zA-Z]+)\",source = \"([a-zA-Z]+)\"\\)");
        Matcher matcher = pattern.matcher(inputString);

        while (matcher.find()) {
            String mapper = matcher.group(0);
            System.out.println(mapper);
        }

        if (!matcher.hitEnd()) {
            System.out.println("Invalid input string.");
        }
    }
}