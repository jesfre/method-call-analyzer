/**
 * 
 */
package com.blogspot.jesfre.methodflow.serialization;

import java.io.Serializable;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 4, 2016
 */
public class SerClass implements Serializable {
	private static final long serialVersionUID = 1L;

	private String className = null;
	private String packageName = null;
	private String superclassName = null;
	private String fileName = null;
	private String module = null;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getSuperclassName() {
		return superclassName;
	}

	public void setSuperclassName(String superclassName) {
		this.superclassName = superclassName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
		result = prime * result + ((superclassName == null) ? 0 : superclassName.hashCode());
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
		SerClass other = (SerClass) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (packageName == null) {
			if (other.packageName != null)
				return false;
		} else if (!packageName.equals(other.packageName))
			return false;
		if (superclassName == null) {
			if (other.superclassName != null)
				return false;
		} else if (!superclassName.equals(other.superclassName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SerClass [className=");
		builder.append(className);
		builder.append(", \npackageName=");
		builder.append(packageName);
		builder.append(", \nsuperclassName=");
		builder.append(superclassName);
		builder.append(", \nfileName=");
		builder.append(fileName);
		builder.append(", \nmodule=");
		builder.append(module);
		builder.append("]");
		return builder.toString();
	}

}
