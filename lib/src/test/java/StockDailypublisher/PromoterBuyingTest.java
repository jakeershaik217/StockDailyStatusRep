package StockDailypublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.Test;

import com.RestAssured.RestPackage.RestAssuredClass;
import com.Stock.Utility.EmailSender;
import com.Stock.Utility.EmailStyles;
import com.Stock.Utility.MarketCapRangeProvider;
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
	
	@Test(priority=-1,dataProvider = "paralleltest",dataProviderClass = MarketCapRangeProvider.class,enabled=true)
	public static void RunTesToFectStocks(HashMap<String, Integer> Maps) {
		
		RestAssuredClass Rs=new RestAssuredClass();
		ThreadPackage.getInstance().setThreadLocal(Rs);
		PromoterBuying.addAll(ThreadPackage.getInstance().getThreadLocal().getPromoterBuyingData(Maps.get("Range1"), Maps.get("Range2"), CompnayAllDataList));
	}
	
	String CssSheet=EmailStyles.tableHeader("Compnay Name", "NumberOfShares", "AmountPurchased");
	
	
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
		public void SendEmail() throws Exception {
			EmailSender.sendHtmlEmail("Promoter Buying", getAllDataAboutCompnay());
		}
	

}
