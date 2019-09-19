/**
 * 
 */
package com.blogspot.jesfre.methodflow.common;

/**
 * Holds the information about a visited method.
 * 
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Dec 13, 2016
 */
public class VisitedMethod {

	private String methodDeclaration = null;
	private int level = -1;
	private MethodCallComposite methodComposite = null;

	public VisitedMethod() {
	}

	public VisitedMethod(String methodDeclaration, int level, MethodCallComposite methodComposite) {
		super();
		this.methodDeclaration = methodDeclaration;
		this.level = level;
		this.methodComposite = methodComposite;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getMethodDeclaration() {
		return methodDeclaration;
	}

	public void setMethodDeclaration(String methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	public MethodCallComposite getMethodComposite() {
		return methodComposite;
	}

	public void setMethodComposite(MethodCallComposite methodComposite) {
		this.methodComposite = methodComposite;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VisitedMethod [methodDeclaration=");
		builder.append(methodDeclaration);
		builder.append(", \nlevel=");
		builder.append(level);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		result = prime * result + ((methodDeclaration == null) ? 0 : methodDeclaration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VisitedMethod other = (VisitedMethod) obj;
		if (level != other.level)
			return false;
		if (methodDeclaration == null) {
			if (other.methodDeclaration != null)
				return false;
		} else if (!methodDeclaration.equals(other.methodDeclaration))
			return false;
		return true;
	}

}
