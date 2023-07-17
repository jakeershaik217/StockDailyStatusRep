package StockDailypublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.StaticVariableCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;


public class ResultDayDataPublisher {
	
	private static List<HashMap<String,Object>> ResultsCollection=new ArrayList<>();
	@BeforeTest
	public static void getAllCompaniesResultPosting() {
		ResultsDay.getResultsFortheDay("20221121", "20221127");
		ResultsCollection.addAll(StaticVariableCollection.Results);
		ResultsCollection.forEach(System.out::println);
	}
	@Test
	public static void getNewsDataLink() throws  IOException {
		RestAssuredClass restAssured=new RestAssuredClass();
		restAssured.NewsLetters(ResultsCollection);
		
		
		
	}

}
