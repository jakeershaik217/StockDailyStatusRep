package StockDailypublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.computaion.classes.ThreadPackage;

public class PromoterBuyingTest {
	private static List<HashMap<String,Object>> CompnayAllDataList;
	private static List<HashMap<String,Object>> PromoterBuying=new ArrayList<HashMap<String,Object>>();
	@BeforeSuite
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
	
	@Test(priority=0)
	public void getAllDataAboutCompnay(){
		
		for(HashMap<String,Object> maps:PromoterBuying) {
			Set<Entry<String, Object>> mapsentry=maps.entrySet();
			for(Map.Entry<String, Object> map:mapsentry)
			  System.out.println(map.getKey()+"------------"+map.getValue());
			System.out.println("--------------------------------");
		
		}		  
	}
	

}
