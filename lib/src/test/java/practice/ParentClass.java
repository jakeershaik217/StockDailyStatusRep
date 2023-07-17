package practice;

public class ParentClass {
	
	ParentClass(){
		
		System.out.println("ParentClass Constructor");
		
	}
	
	public final void m1() {
		
		
		System.out.println("m1");
	}
	
	public static void m2() {
		
		System.out.println("m2");
	}
	
   public  Number parent() {
		
		System.out.println("parent");
		return 1;
	}

}
