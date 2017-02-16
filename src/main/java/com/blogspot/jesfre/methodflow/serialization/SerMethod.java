/**
 * 
 */
package com.blogspot.jesfre.methodflow.serialization;

import java.io.Serializable;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 4, 2016
 */
public class SerMethod implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name = null;
	private String signature = null;

	private boolean modifPublic = false;
	private boolean modifPrivate = false;
	private boolean modifProtected = false;
	private boolean modifAbstract = false;
	private boolean modifFinal = false;
	private boolean modifNative = false;
	private boolean modifStrinctfp = false;
	private boolean modifStatic = false;
	private boolean modifSynchronized = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
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
		SerMethod other = (SerMethod) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SerMethod [name=");
		builder.append(name);
		builder.append(", \nsignature=");
		builder.append(signature);
		builder.append("]");
		return builder.toString();
	}

	public boolean isPublic() {
		return modifPublic;
	}
	public boolean isPrivate() {
		return modifPrivate;
	}

	public boolean isProtected() {
		return modifProtected;
	}

	public boolean isAbstract() {
		return modifAbstract;
	}

	public boolean isFinal() {
		return modifFinal;
	}

	public boolean isNative() {
		return modifNative;
	}

	public boolean isStrictfp() {
		return modifStrinctfp;
	}

	public boolean isStatic() {
		return modifStatic;
	}

	public boolean isSynchronized() {
		return modifSynchronized;
	}

}
