package practice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TableExtractor {

	public static void main(String[] args) throws IOException {
		Document doc = Jsoup.connect("https://www.screener.in/company/VBL/consolidated/").get();
		Elements tds=null;
		String Data="";
		HashMap<String,String> datamap=new HashMap<>();
	    for (Element table : doc.select("section#quarters>div>table>thead")) {
	        for (Element row : table.select("tr")) {
	            tds = row.select("th");
	        }
	    }
	    for(int i=0;i<tds.size();i++) {
	    	datamap.put(tds.get(i).text(), "");
	    	
	    }
	    
	    System.out.print(Data.trim());
	    System.out.println("\n");
		
		List<List<Element>> CollectionData=new ArrayList<>();
		int count=1;
	    for (Element table : doc.select("section#quarters>div>table>tbody")) {
	        for (Element row : table.select("tr")) {
	        	Elements td = row.select("td");
	        	CollectionData.add(td);
	        }
	    }
	   
    for(int i=0;i<CollectionData.size();i++) {
    	
    	for(int j=0;j<CollectionData.get(i).size();j++) {
    		if(CollectionData.get(i).get(0).text().contains("Sales")) {
    			int size=datamap.keySet().size();
    			for(int datac=0;datac<size;datac++) {
    				
    			}
    			
    		}
    		Data="";
    		Data=Data+"  "+CollectionData.get(i).get(j).text()+"  ";
    		System.out.print(Data);
    	}
    	System.out.println("\n");
    	
    }
	    
	    

	}

}
