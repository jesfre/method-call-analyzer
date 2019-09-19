/**
 * 
 */
package com.blogspot.jesfre.methodflow.serialization;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import com.blogspot.jesfre.methodflow.common.ClassParsingUtils;
import com.blogspot.jesfre.methodflow.common.Configuration;

/**
 * Set of method to serialize/deserialize the method index.
 * 
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Dec 4, 2016
 */
public final class SerializationUtils {

	/**
	 * @param ownerClass
	 * @return
	 */
	public static SerClass createSerializableClass(JavaClass ownerClass) {
		SerClass serOwnerClass = new SerClass();
		serOwnerClass.setClassName(ownerClass.getClassName());
		serOwnerClass.setPackageName(ownerClass.getPackageName());
		serOwnerClass.setSuperclassName(ownerClass.getSuperclassName());
		serOwnerClass.setFileName(ownerClass.getFileName());
		return serOwnerClass;
	}

	/**
	 * @param method
	 * @return
	 */
	public static SerMethod createSerializableMethod(Method method) {
		SerMethod serMethod = new SerMethod();
		serMethod.setName(method.getName());
		serMethod.setSignature(method.getSignature());
		return serMethod;
	}

}
