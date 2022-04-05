package practice;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class className {

	public static void main(String[] args) {
		
		finduplicatecharInString();
		findduplicateCharinString();
		findduplicateStringsinStringCollection();
		addNumberinAString();
		printMultiplesofnumber();
		System.out.println(checkgivenStringisalancedString("{(jakeer)}"));
		findthenumberOFoccurancesOfANumberinAnArray(new int[] {2,4,6,8,8,7,2,1,6});
		printTreewithStar(11);
	}
	
	public static void finduplicatecharInString() {
		
		int[] charArray=new int[255];
		String string="JakeerShaikHussain";
		for(char c:string.toCharArray())
			charArray[c]++;
		
		for(int i=0;i<charArray.length;i++)
			if(charArray[i]>1)
				System.out.println((char)i+" : "+charArray[i]);
			
	}
	
	public static void findduplicateCharinString() {
		
		
		Map<Character, Integer> maps=new HashMap<>();
		String string="JakeerShaikHussain";
		for(char c:string.toCharArray())
			if(maps.containsKey(c))
				maps.put(c, maps.get(c)+1);
			else
				maps.put(c, 1);
		
		 for(char c:maps.keySet()) 
			 if(maps.get(c)>1)
		       System.out.println(c+" : "+maps.get(c));
	}

	
public static void findduplicateStringsinStringCollection() {
		
		
		Map<String, Integer> maps=new HashMap<>();
		String[] string= {"Jakeer","Hussain","Shaik","Jakeer","Hussain"};
		for(String c:string)
			if(maps.containsKey(c))
				maps.put(c, maps.get(c)+1);
			else
				maps.put(c, 1);
		
		 for(String c:maps.keySet()) 
			 if(maps.get(c)>1)
		       System.out.println(c+" : "+maps.get(c));
	}

public static void addNumberinAString() {
	
	
	String string="Jakeer12hussain1a";
	String Number="0";
	int sum=0;
	for(char c:string.toCharArray()) {
		if(Character.isDigit(c))
			Number=Number+c;
		else {
			sum+=Integer.parseInt(Number);
	        Number="0";
		}
	  
	}
	sum+=Integer.parseInt(Number);
	System.out.println(sum);	
}

public static void printMultiplesofnumber() {
	
	int[] number= {1,2,5,7,9,8,3,15,4};
	for(int i=0;i<number.length;i++) {
		int j=i+1;
		if(j<number.length) {
		for(int k=j+2;k<number.length;k++) {
			
			int numbermul=number[i]*number[j]*number[k];
			if(numbermul%5==0) {
				System.out.println("Pair of 3 is equal "+number[i]+","+number[j]+","+number[k]);
			}
		}
		
		}
	}
	
	
}


public static boolean checkgivenStringisalancedString(String name) {
	
	Stack<Character> stack=new Stack<>();
    if(name.isEmpty())
    	return true;
	for(char c:name.toCharArray()) {
		switch(c) {
		
		
		case '{' : stack.push(c);
                    break;
		case '[' :stack.push(c);
                  break;
		case '(' :stack.push(c);
		          break;
		case ')' : if(stack.isEmpty() || stack.pop()!='(')
			       return false;
		           break;
		case ']' : if(stack.isEmpty() || stack.pop()!='[')
		       return false;
	           break;
		case '}' : if(stack.isEmpty() || stack.pop()!='{')
		       return false;
	           break;
		
	}
		
		
		
		
	}
	
	return stack.isEmpty();
	
	
}

public static void findthenumberOFoccurancesOfANumberinAnArray(int... x) {
	
	
	Map<Integer,Integer> maps=new HashMap<>();
	for(int i:x)
		if(maps.containsKey(i))
			maps.put(i, maps.get(i)+1);
		else
			maps.put(i, 1);
	
	for(int i:maps.keySet())
		System.out.println(i+" : "+maps.get(i));
	
}

public static void printTreewithStar(int numberOfLines) {
	
	
	for(int i=1;i<=numberOfLines;i++) {
		String Space="";
		for(int j=1;j<=i;j++) {
			for(int k=numberOfLines;k>=i;k--)
				Space=Space+" ";
			if(j==1)
				System.out.print(Space+"*");
			else
				System.out.print(" *");
			
		}
		System.out.print("\n");
	}
	
	for(int i=1;i<=numberOfLines;i++) {
		String Space="";
			for(int j=0;j<numberOfLines;j++)
				Space=Space+" ";
		System.out.print(Space+"*");
		System.out.print("\n");
		
		
	}
            
	
	
	
}
}
