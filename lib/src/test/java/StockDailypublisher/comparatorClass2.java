package StockDailypublisher;

import java.util.Comparator;
import java.util.HashMap;

public class comparatorClass2 implements Comparator<HashMap<String,Object>> {

	@Override
	public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
	
		double d1= Double.parseDouble(o1.get("MutualFunds")+"");
		double d2= Double.parseDouble(o2.get("MutualFunds")+"");
		if(d1>d2)
			return -1;
		else if(d1<d2)
			return 1;
		else
			return 0;
	}
	

}
