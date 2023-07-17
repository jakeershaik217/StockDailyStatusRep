package practice;

import java.util.stream.Stream;

public class JsonPayloadCreator {
	
	public static void main(String[] args) {
		String Name = "00013435\n"
				+ "00123487";
		String JsonPayloadPrefix="{ \"listContracts\" : [";
		String JsonPayloadSufix=" ] }";
		String[] collection=Name.split("\\n");
		for(int i=0;i<collection.length;i++) {
			
			if(i!=(collection.length-1))
			    JsonPayloadPrefix = JsonPayloadPrefix+ "{ \"contractId\" : \""+collection[i]+"\" },";
			else
				JsonPayloadPrefix = JsonPayloadPrefix+ "{ \"contractId\" : \""+collection[i]+"\" }"+JsonPayloadSufix;	
		}
		
		System.out.println(JsonPayloadPrefix);
	}

}
