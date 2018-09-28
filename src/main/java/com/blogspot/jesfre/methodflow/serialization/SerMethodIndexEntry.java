/**
 * 
 */
package com.blogspot.jesfre.methodflow.serialization;

import java.io.Serializable;
import java.util.Collection;

import com.blogspot.jesfre.methodflow.common.MethodCallComposite;

/**
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 * Dec 4, 2016
 */
public class SerMethodIndexEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	private String methodDeclaration = null;
	private SerMethod method = null;
	private SerClass ownerClass = null;
	private Collection<SerInstruction> instructionList = null;
	private transient MethodCallComposite methodComposite = null;

	public SerMethodIndexEntry() {
	}

	public SerMethodIndexEntry(String methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	public SerMethodIndexEntry(String methodDeclaration, SerMethod method, SerClass ownerClass, Collection<SerInstruction> instructionList) {
		super();
		this.methodDeclaration = methodDeclaration;
		this.method = method;
		this.ownerClass = ownerClass;
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

	public SerClass getOwnerClass() {
		return ownerClass;
	}

	public void setOwnerClass(SerClass ownerClass) {
		this.ownerClass = ownerClass;
	}


	public Collection<SerInstruction> getInstructionList() {
		return instructionList;
	}

	public void setInstructionList(Collection<SerInstruction> instructionList) {
		this.instructionList = instructionList;
	}

	public MethodCallComposite getMethodComposite() {
		return methodComposite;
	}

	public void setMethodComposite(MethodCallComposite methodComposite) {
		this.methodComposite = methodComposite;
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
		SerMethodIndexEntry other = (SerMethodIndexEntry) obj;
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
		builder.append("SerMethodIndexEntry [methodDeclaration=");
		builder.append(methodDeclaration);
		builder.append(", \nmethod=");
		builder.append(method);
		builder.append(", \nownerClass=");
		builder.append(ownerClass);
		builder.append("]");
		return builder.toString();
	}

}
