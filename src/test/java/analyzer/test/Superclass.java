package analyzer.test;

public abstract class Superclass {

	private static final String PUBLIC_CONSTANT_FIELD = "PUB_CONST";
	
	private static final String PRIVATE_CONSTANT_FIELD = "PRIV_CONST";
	
	private DependencyClass dependency1 = new DependencyClass();
	
	public SecondDependencyClass dependency2 = null;
	
	public void mySuperConcreteMethod() {
		dependency2 = new SecondDependencyClass("test");
		
		dependency1.generateNumber("test");
	}
	
	public abstract void myAbstractMethod(); 
}
