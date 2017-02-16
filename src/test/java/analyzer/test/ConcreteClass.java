package analyzer.test;

public class ConcreteClass extends Superclass {

	public ConcreteClass(String st) {
		super();
	}
	
	@Override
	public void myAbstractMethod() {
		DependencyClass dep1 = new DependencyClass();
		SecondDependencyClass dep2 = new SecondDependencyClass("test");
		
		mySuperConcreteMethod();
	}

}
