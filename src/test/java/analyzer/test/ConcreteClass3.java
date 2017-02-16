package analyzer.test;

public class ConcreteClass3 extends Superclass {

	public ConcreteClass3(String st) {
		
	}
	
	@Override
	public void myAbstractMethod() {
		DependencyClass dep1 = new DependencyClass();
		SecondDependencyClass dep2 = new SecondDependencyClass("test");
		
		mySuperConcreteMethod();
	}

}
