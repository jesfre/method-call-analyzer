package analyzer.test;

public class ConcreteClass2 extends Superclass {

	public ConcreteClass2(String st) {
		
	}
	
	@Override
	public void myAbstractMethod() {
		DependencyClass dep1 = new DependencyClass();
		SecondDependencyClass dep2 = new SecondDependencyClass("test");
		
		mySuperConcreteMethod();
	}

}
