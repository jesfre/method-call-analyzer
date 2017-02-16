package com.blogspot.jesfre.methodflow.visitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.common.ClassFilter;
import com.blogspot.jesfre.methodflow.common.ClassLoaderUtils;
import com.blogspot.jesfre.methodflow.common.MethodCallComposite;
import com.blogspot.jesfre.methodflow.reports.ReportEngine;

/**
 * TODO 
 */
public class FlowCallGraph {
	private static final String MAIN_METHOD = "main";
	private static final String MAIN_SIGNATURE = "([Ljava/lang/String;)V";

	private static final String reportLocationPath = "C:/ProjectILIES/java-callgraph-master";
	
	private static final List<String> projectSrcFolders =  new ArrayList<String>(Arrays.asList(new String[]{
			"C:/ProjectILIES/ILIES_BATCH/CCD/src",
			"C:/ProjectILIES/ILIES_BATCH/CCD/bin",
			"C:/ProjectILIES/ILIES_BATCH/FrameworkEJB/ejbModule",
			"C:/ProjectILIES/ILIES_BATCH/SharedApp/src",
			"C:/ProjectILIES/ILIES_BATCH/SharedApp/bin",
			"C:/ProjectILIES/ILIES_BATCH/CorrespondanceBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/CorrespondanceBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/CorrespondanceBO/src",
			"C:/ProjectILIES/ILIES_BATCH/CorrespondanceBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/ConversionBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/ConversionBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/ConversionBO/src",
			"C:/ProjectILIES/ILIES_BATCH/ConversionBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/EligibilityBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/EligibilityBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/EligibilityBO/src",
			"C:/ProjectILIES/ILIES_BATCH/EligibilityBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/FrontofficeBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/FrontofficeBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/FrontofficeBO/src",
			"C:/ProjectILIES/ILIES_BATCH/FrontofficeBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/InterfacesBO/src",
			"C:/ProjectILIES/ILIES_BATCH/InterfacesBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/SelfServiceBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/SelfServiceBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/SelfServiceBO/src",
			"C:/ProjectILIES/ILIES_BATCH/SelfServiceBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/SupportFunctionBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/SupportFunctionBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/SupportFunctionBO/src",
			"C:/ProjectILIES/ILIES_BATCH/SupportFunctionBO/bin",
			"C:/ProjectILIES/ILIES_BATCH/WVSBatch/src",
			"C:/ProjectILIES/ILIES_BATCH/WVSBatch/bin",
			"C:/ProjectILIES/ILIES_BATCH/WVSBO/src",
			"C:/ProjectILIES/ILIES_BATCH/WVSBO/bin"
	}));
	
	private List<String> includedPackages = new ArrayList<String>(Arrays.asList(new String[]{"gov.illinois"}));
	
	private List<String> excludedPackages = new ArrayList<String>(Arrays.asList(new String[]{
	// "gov.illinois.fw",
	// "gov.illinois.framework",
	// "gov.illinois.ies.business.entities"
	// "gov.illinois.fw.batch.entities"
	}));
	
	private List<String> excludedClasses = new ArrayList<String>(Arrays.asList(new String[]{
	}));
	
	private static Map<String, JavaClass> classLoader = new LinkedHashMap<String, JavaClass>();

	/**
	 * Map of visited methods map.<br/>
	 * <ul>
	 * <li>key: Method declaration</li>
	 * <li>value: visited methods map where
	 * <ul>
	 * <li>key: Method declaration</li>
	 * <li>value: InvokeInstruction</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * key:
	 */
	private Map<String, Map<String, InvokeInstruction>> visitedMethods = new LinkedHashMap<String, Map<String, InvokeInstruction>>();
	private Set<String> visitedClasses = new LinkedHashSet<String>();

	private MethodCallComposite entryMethodComposition = new MethodCallComposite();
	private Set<MethodCallComposite> methodIndex = new LinkedHashSet<MethodCallComposite>();

	private ReportEngine flowReport = null;
	private ReportEngine flowReportTxt = null;
	private ReportEngine visitedMethodsReportTxt = null;
	private ReportEngine visitedMethodsReportHtml = null;

	public static void main(String[] args) throws Exception {
    	FlowCallGraph flowGraph = new FlowCallGraph();
        try {
        	flowGraph.generateGraph(args);
        } catch (IOException e) {
            System.err.println("Error while processing jar: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("End.");
    }

	/**
	 * @param args
	 * @throws Exception
	 */
    public void generateGraph(String[] args) throws Exception {
    	String entryClassName = args[0];
		InputStream headerContentIs = FlowCallGraph.class.getClassLoader().getResourceAsStream("com/blogspot/jesfre/methodflow/resources/reportheader.html");
		String headerContent = IOUtils.toString(headerContentIs);

		flowReport = new ReportEngine(String.format(Constants.FLOW_REPORT_HTML, entryClassName), reportLocationPath, 0);
		flowReport.disableConsoleLogging();
		flowReport.appendContent(headerContent, 0);

		flowReportTxt = new ReportEngine(String.format(Constants.FLOW_REPORT_TXT, entryClassName), reportLocationPath, 0);
		flowReportTxt.enableConsoleLogging();

		visitedMethodsReportTxt = new ReportEngine(String.format(Constants.VISITED_METHODS_REPORT_CSV, entryClassName), reportLocationPath, 0);

		visitedMethodsReportHtml = new ReportEngine(String.format(Constants.VISITED_METHODS_REPORT_HTML, entryClassName), reportLocationPath, 0);
		visitedMethodsReportHtml.appendContent(headerContent, 0);

    	System.out.println("Loading classes...");
		int loadedClassesCnt = 0;
    	for(String srcFolderPath : projectSrcFolders) {
			System.out.println("Loading classes in " + srcFolderPath);
    		File srcFolder = new File(srcFolderPath);
    		if(srcFolder.exists()) {
				Collection<File> files = FileUtils.listFiles(srcFolder, new String[] { "class" }, true);
				for (File classFile : files) {
					String qClassName = null;
					InputStream is = null;
					try {
						String classLocation = classFile.getAbsolutePath();
						qClassName = ClassLoaderUtils.getQualifiedClassname(classLocation, srcFolder.getAbsolutePath());
						boolean isAllowedClass = ClassLoaderUtils.isAllowedPackage(qClassName, includedPackages, excludedPackages);
						 if (!isAllowedClass) {
							 continue;
						 }

						// System.out.println("Loading " + qClassName);
						is = new FileInputStream(classFile);
						ClassParser cp = new ClassParser(is, classLocation);
						JavaClass javaClass = cp.parse();
						if (!javaClass.getClassName().equals(qClassName)) {
							System.err.println(qClassName + " and " + javaClass.getClassName() + " do not match.");
						}
						classLoader.put(qClassName, javaClass);
						loadedClassesCnt++;
						// System.out.println("Loaded " + qClassName);
					} catch (Throwable t) {
						System.err.println("Exception processing " + qClassName);
						t.printStackTrace();
					} finally {
						if (is != null) {
							is.close();
						}
					}
				}
    		} else {
    			System.out.println(srcFolderPath + " does not exist.");
    		}
    	}
		System.out.println("Loaded " + loadedClassesCnt + " classes.");
    	if(classLoader.containsKey(entryClassName)) {
    		JavaClass entryClass = classLoader.get(entryClassName);
    		for(Method method: entryClass.getMethods()) {
    			String mname = method.getName();
    			String msignature = method.getSignature();
    			if(MAIN_METHOD.equals(mname) && MAIN_SIGNATURE.equals(msignature)) {
					Map<String, InvokeInstruction> instructions = getMethodCalls(entryClass, method, 0);
    			}
    		}
    	} else {
    		System.err.println("Class does not exist.");
    	}

		this.generateReports(entryClassName);
    }

	private Map<String, InvokeInstruction> getMethodCalls(JavaClass entryClass, Method method, int level) throws Exception {
		ClassVisitor cvisitor = new ClassVisitor(entryClass);
		StringBuilder spaces = new StringBuilder();
		String methodDeclaration = entryClass.getClassName() + "." + method.getName() + method.getSignature();

		Map<String, InvokeInstruction> invInstructions = null;
		if (visitedMethods.containsKey(methodDeclaration)) {
			// invInstructions = visitedMethods.get(methodDeclaration);
			return new HashMap<String, InvokeInstruction>();
		} else {
			MethodVisitor mvisitor = cvisitor.visitMethod(method, includedPackages, excludedPackages, excludedClasses);
			invInstructions = mvisitor.getCallMap();
			visitedMethods.put(methodDeclaration, invInstructions);
		}

		// Skip those constructors without instructions
		if ("<init>".equals(method.getName()) && invInstructions.size() == 0) {
			return new HashMap<String, InvokeInstruction>();
		}
		for (int i = 0; i < level; i++) {
			// tabs.append(" | ");
			spaces.append("   ");
		}
		String btnPlus = "&nbsp;&nbsp;";
		String tabs = " ";
		int methodHash = Math.abs(methodDeclaration.hashCode());
		if (invInstructions.size() > 0) {
			btnPlus = "<span id='btn" + methodHash + "' name='expander' style='font-weight:bold;' onclick=\"javascript:toggle('" + methodHash + "')\">+&nbsp;</span>";
			tabs = "+";
		}
		flowReportTxt.appendLine(StringUtils.leftPad(tabs, level, " | ") + methodDeclaration);
		flowReport.appendLine("<div style='margin-left:" + level * 20 + "px;'>" + btnPlus + methodDeclaration + "</div>");
		flowReport.appendLine("<span id='content" + methodHash + "' name='container' style='display:none'>");
    	ConstantPoolGen cp = cvisitor.getConstants();
		for (Map.Entry<String, InvokeInstruction> e : invInstructions.entrySet()) {
			InvokeInstruction inst = e.getValue();
			String qClassName = inst.getReferenceType(cp).toString();
			String calledMethodName = inst.getMethodName(cp);
			String signature = inst.getSignature(cp);

			visitedClasses.add(qClassName);

			if (classLoader.containsKey(qClassName)) {
				JavaClass clazz = classLoader.get(qClassName);
				Map.Entry<JavaClass, Method> calledMethod = getCalledMethod(clazz, calledMethodName, signature);
				if (calledMethod != null) {
					getMethodCalls(calledMethod.getKey(), calledMethod.getValue(), level + 1);
				} else {
					System.err.println(spaces + "!!! Cannot find method " + qClassName + "." + calledMethodName + signature);
				}
			} else {
				System.err.println("Class " + qClassName + " not found in classloader.");
			}
		}
		flowReport.appendLine("</span>");
		return invInstructions;
	}

	private Map.Entry<JavaClass, Method> getCalledMethod(JavaClass clazz, String calledMethodName, String signature) {
		for (Method refMethod : clazz.getMethods()) {
			String mname = refMethod.getName();
			String msignature = refMethod.getSignature();
			if (mname.equals(calledMethodName) && msignature.equals(signature)) {
				Map.Entry<JavaClass, Method> e = new AbstractMap.SimpleEntry(clazz, refMethod);
				return e;
			}
		}
		String superClassName = clazz.getSuperclassName();
		JavaClass superClazz = classLoader.get(superClassName);
		if (superClazz != null) {
			return getCalledMethod(superClazz, calledMethodName, signature);
		}
		return null;
	}

	private void generateReports(String entryClassName) throws IOException {
		flowReport.writeReport(false);
		flowReportTxt.writeReport(false);

		System.out.println("\n\n-------------------------------\nVisited methods...");
		List<String> sortedVisitedClasses = new ArrayList<String>(visitedClasses);
		Collections.sort(sortedVisitedClasses);
		Map<String, Set<String>> calledMethodsByClassMap = new LinkedHashMap<String, Set<String>>();
		for (Entry<String, Map<String, InvokeInstruction>> invocationMap : visitedMethods.entrySet()) {
			String methodDeclaration = invocationMap.getKey();
			int indexOfMethodName = methodDeclaration.lastIndexOf('.', methodDeclaration.indexOf('('));
			String classNamePart = methodDeclaration.substring(0, indexOfMethodName);
			String methodNamePart = methodDeclaration.substring(indexOfMethodName + 1);
			if (!calledMethodsByClassMap.containsKey(classNamePart)) {
				calledMethodsByClassMap.put(classNamePart, new LinkedHashSet<String>());
			}
			calledMethodsByClassMap.get(classNamePart).add(methodNamePart);
		}

		List<String> visitedMethodList = new ArrayList<String>();
		for (String visitedClass : sortedVisitedClasses) {
			if (calledMethodsByClassMap.containsKey(visitedClass)) {
				Set<String> visitedMethodNameList = calledMethodsByClassMap.get(visitedClass);

				String btnPlus = "&nbsp;&nbsp;";
				int classHash = Math.abs(visitedClass.hashCode());
				if (visitedMethodNameList.size() > 0) {
					btnPlus = "<span id='btn" + classHash + "' name='expander' style='font-weight:bold;' onclick=\"javascript:toggle('" + classHash + "')\">+&nbsp;</span>";
				}
				visitedMethodsReportHtml.appendLine(btnPlus + "<b>" + visitedClass + "</b>");
				visitedMethodsReportHtml.appendLine("<ul id='content" + classHash + "' name='container' style='display:none'>");

				for (String visitedMethod : visitedMethodNameList) {
					String fullMethodDecl = visitedClass + "." + visitedMethod;
					if (!visitedMethodList.contains(fullMethodDecl)) {
						visitedMethodList.add(fullMethodDecl);
						visitedMethodsReportTxt.appendLine(visitedClass + "." + visitedMethod);
						visitedMethodsReportHtml.appendLine("<li>" + visitedMethod + "</li>");
					}
				}
				visitedMethodsReportHtml.appendLine("</ul><br/>");
			} else {
				// System.err.println(visitedClass + " is not mapped in visited methods.");
			}
		}
		visitedMethodsReportTxt.writeReport(false);
		visitedMethodsReportHtml.writeReport(false);
	}
}
