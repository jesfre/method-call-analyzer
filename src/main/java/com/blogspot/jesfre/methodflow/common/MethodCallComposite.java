package com.blogspot.jesfre.methodflow.common;

import java.util.List;

import com.blogspot.jesfre.methodflow.serialization.SerClass;
import com.blogspot.jesfre.methodflow.serialization.SerMethod;

/**
 * Defines a composition of the method and it method calls
 * 
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 *         Nov 29, 2016
 */
public class MethodCallComposite {
	private String methodDeclaration = null;
	private SerMethod method = null;
	private SerClass clazz = null;
	private List<MethodCallComposite> instructionList = null;

	public MethodCallComposite() {
	}

	public MethodCallComposite(String methodDeclaration, List<MethodCallComposite> instructionList) {
		super();
		this.methodDeclaration = methodDeclaration;
		this.instructionList = instructionList;
	}

	public String getMethodDeclaration() {
		return methodDeclaration;
	}

	public void setMethodDeclaration(String methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	public SerMethod getMethod() {
		return method;
	}

	public void setMethod(SerMethod method) {
		this.method = method;
	}

	public SerClass getClazz() {
		return clazz;
	}

	public void setClazz(SerClass clazz) {
		this.clazz = clazz;
	}

	public List<MethodCallComposite> getInstructionList() {
		return instructionList;
	}

	public void setInstructionList(List<MethodCallComposite> instructionList) {
		this.instructionList = instructionList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		MethodCallComposite other = (MethodCallComposite) obj;
		if (methodDeclaration == null) {
			if (other.methodDeclaration != null)
				return false;
		} else if (!methodDeclaration.equals(other.methodDeclaration))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MethodCallComposite [methodDeclaration=");
		builder.append(methodDeclaration);
		builder.append(", \nmethod=");
		builder.append(method);
		builder.append(", \nclazz=");
		builder.append(clazz);
		builder.append(", \ninstructionList=");
		builder.append(instructionList);
		builder.append("]");
		return builder.toString();
	}

}
