/**
 * 
 */
package com.blogspot.jesfre.methodflow.serialization;

import java.io.Serializable;

/**
 * Holds the information of serializable method and corresponding serializable class.
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 * Feb 16, 2017
 */
public class SerInstruction implements Serializable {
	
	public SerInstruction() {
	}
	
	public SerInstruction(SerMethod serMethod, SerClass serClass) {
		super();
		this.serMethod = serMethod;
		this.serClass = serClass;
	}

	private SerMethod serMethod = null;
	
	private SerClass serClass = null;

	public SerMethod getSerMethod() {
		return serMethod;
	}

	public void setSerMethod(SerMethod serMethod) {
		this.serMethod = serMethod;
	}

	public SerClass getSerClass() {
		return serClass;
	}

	public void setSerClass(SerClass serClass) {
		this.serClass = serClass;
	}

	@Override
	public String toString() {
		return "SerInstruction [serMethod=" + serMethod + ", serClass=" + serClass + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serClass == null) ? 0 : serClass.hashCode());
		result = prime * result + ((serMethod == null) ? 0 : serMethod.hashCode());
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
		SerInstruction other = (SerInstruction) obj;
		if (serClass == null) {
			if (other.serClass != null)
				return false;
		} else if (!serClass.equals(other.serClass))
			return false;
		if (serMethod == null) {
			if (other.serMethod != null)
				return false;
		} else if (!serMethod.equals(other.serMethod))
			return false;
		return true;
	}
	
}
