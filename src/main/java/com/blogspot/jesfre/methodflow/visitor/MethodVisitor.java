/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.blogspot.jesfre.methodflow.visitor;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.EmptyVisitor;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.common.ClassFilter;
import com.blogspot.jesfre.methodflow.common.ClassLoaderUtils;

/**
 * The simplest of method visitors, prints any invoked method
 * signature for all method invocations.
 * 
 * Class copied with modifications from CJKM: http://www.spinellis.gr/sw/ckjm/
 */
public class MethodVisitor extends EmptyVisitor {

    private JavaClass visitedClass;
    private MethodGen mg;
    private ConstantPoolGen cp;
    private String format;
    private List<String> includedPackages = new ArrayList<String>();
    private List<String> excludedPackages = new ArrayList<String>();
    private List<String> excludeClasses = new ArrayList<String>();
	private Map<String, InvokeInstruction> callMap = new LinkedHashMap<String, InvokeInstruction>();

    public MethodVisitor(MethodGen m, JavaClass jc) {
        visitedClass = jc;
        mg = m;
        cp = mg.getConstantPool();
        format = "M:" + visitedClass.getClassName() + ":" + mg.getName() 
				+ " " + "(%s)%s:%s:%s";
    }
    
    public void start() {
        if (mg.isAbstract() || mg.isNative())
            return;
        for (InstructionHandle ih = mg.getInstructionList().getStart(); 
                ih != null; ih = ih.getNext()) {
            Instruction i = ih.getInstruction();
            if (!visitInstruction(i))
                i.accept(this);
        }
    }

    private boolean visitInstruction(Instruction i) {
        short opcode = i.getOpcode();

        return ((InstructionConstants.INSTRUCTIONS[opcode] != null)
                && !(i instanceof ConstantPushInstruction) 
                && !(i instanceof ReturnInstruction));
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
    	addStackEntry(i, "M");
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
    	addStackEntry(i, "I");
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
    	addStackEntry(i, "O");
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
    	addStackEntry(i, "S");
    }

	private String getPackage(String qClassName) {
		int indexOfPeriod = qClassName.lastIndexOf('.');
		if(indexOfPeriod >= 0) {
			String packageName = qClassName.substring(0, indexOfPeriod);
			return packageName;
		}
		System.err.println("Cannot getPackage for " + qClassName);
		return null;
	}
	
	private void addStackEntry(InvokeInstruction i, String instuctionType) {
		String className = i.getReferenceType(cp).toString();

    	if(!excludeClasses.contains(className)) {
			String packageName = getPackage(className);
			boolean isAllowedPackage = false;
			if (StringUtils.isNotBlank(packageName)) {
				isAllowedPackage = ClassLoaderUtils.isAllowedPackage(packageName, includedPackages, excludedPackages);
			}
			if (isAllowedPackage) {
				String methodDeclarationDesc = i.getReferenceType(cp) + ":" + i.getMethodName(cp) + ":" + i.getSignature(cp);
				if (!callMap.containsKey(methodDeclarationDesc)) {
					callMap.put(methodDeclarationDesc, i);
				}
				// System.out.println(String.format(format, instuctionType, i.getReferenceType(cp), i.getMethodName(cp), i.getSignature(cp)));
	    	} else {
				// System.out.println("Excluded - " + String.format(format,instuctionType,i.getReferenceType(cp),i.getMethodName(cp),
				// i.getSignature(cp)));
	    	}
    	}
	}
	
	public void addIncludedPackage(String includedPackage) {
		this.includedPackages.add(includedPackage);
	}

	public void addExcludedPackage(String excludePackage) {
		this.excludedPackages.add(excludePackage);
	}

	public void addExcludedClass(String excludeClass) {
		this.excludeClasses.add(excludeClass);
	}

	public Map<String, InvokeInstruction> getCallMap() {
		return callMap;
	}

}
