package analyzer.test;

public class SecondDependencyClass {
	
	public SecondDependencyClass(String param) {
		System.out.println(param);
	}

	public void writeWord1() {
		System.out.println("Hello");
	}
	
	public void writeWord2() {
		System.out.println("World");
	}
	
}
