package analyzer.test;

public class MainClass {

	public static void main(String[] args) {
		ConcreteClass c1 = new ConcreteClass("test");
		ConcreteClass2 c2 = new ConcreteClass2("test");
		ConcreteClass3 c3 = new ConcreteClass3("test");
		
		c1.mySuperConcreteMethod();
		c1.myAbstractMethod();
	}
	
}
