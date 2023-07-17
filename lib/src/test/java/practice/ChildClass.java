package practice;

public class ChildClass extends ParentClass{
	
	ChildClass(){
		System.out.println("Child Constructor");
	}
	
	public void m3() {
		
		System.out.println("m3");
	}
	
	 int m3(int i) {
		return 1;
		
	}
	
	public static void m2() {
		
		System.out.println("m4");
	}

	 public  Integer parent(){
			
			System.out.println("child");
			return 1;
		}
	public static void main(String[] args) {
		ChildClass cs=new ChildClass();
		cs.parent();
		
		ParentClass cd=new ParentClass();
		cd.parent();
		
		ParentClass cf=new ChildClass();
		cf.parent();
		
		

	}

}
