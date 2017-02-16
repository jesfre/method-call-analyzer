/**
 * 
 */
package com.blogspot.jesfre.methodflow.common;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.blogspot.jesfre.methodflow.visitor.Constants;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Nov 30, 2016
 */
public final class ClassParsingUtils {
	private static final String FRAMEWORK = "Framework";
	private static final String CCD = "CCD";
	private static final String FOLDER_PATH_FRAMEWORK = "\\framework\\";
	private static final String FOLDER_PATH_FRAMEWORK_EJB = "\\FrameworkEJB\\";
	private static final String FOLDER_PATH_CCD = "\\CCD\\";
	private static final String VOID = "void";
	private static final String BOOLEAN = "boolean";
	private static final String SHORT = "short";
	private static final String LONG = "long";
	private static final String INT = "int";
	private static final String FLOAT = "float";
	private static final String DOUBLE = "double";
	private static final String CHAR = "char";
	private static final String BYTE = "byte";
	private static final String OTHER = "Other";
	public static Log log = LogFactory.getLog(ClassParsingUtils.class);
	private static Pattern arraysPattern = Pattern.compile("(\\[)([^;) ]*)(;|\\)|\\s)");
	private static Pattern methodParametersPattern = Pattern.compile("(\\()([^)]*)(\\))");
	private static Pattern returnTypePattern = Pattern.compile("(\\s)([BCDFIJSZ][\\[\\]]*|V|L[\\w/]+[\\[\\]]*;)(\\s)");

	private ClassParsingUtils() {
	}

	/**
	 * Formats the values to a full method declaration (including className) or only the method declaration part
	 * [className.]methodName(methodDeclaration_parameters)methodDeclaration_returnType
	 * 
	 * @param className <b>(optional)</b>
	 * @param methodName
	 * @param methodDeclaration
	 * @return
	 */
	public static String formatMethodDeclaration(String className, String methodName, String methodDeclaration) {
		StringBuilder formatedValue = new StringBuilder();
		if (StringUtils.isNotBlank(className)) {
			formatedValue.append(className).append(Constants.STR_DOT);
			// format = Constants.METHOD_DECLARATION_FORMAT_FULL;
			// formatedValue = String.format(format, new Object[] { className, methodName, methodDeclaration });
			// } else {
			// format = Constants.METHOD_DECLARATION_FORMAT_SHORT;
			// formatedValue = String.format(format, new Object[] { methodName, methodDeclaration });
		}
		formatedValue.append(methodName).append(methodDeclaration);
		return formatedValue.toString();
	}

	/**
	 * Keeps the qualified class name
	 * 
	 * @param fullMethodDeclaration
	 * @return
	 */
	public static String getQualifiedClassnamePart(String fullMethodDeclaration) {
		String classNamePart = "";
		if (StringUtils.isNotBlank(fullMethodDeclaration)) {
			int indexOfMethodName = fullMethodDeclaration.lastIndexOf('.');
				classNamePart = fullMethodDeclaration.substring(0, indexOfMethodName);
		}
		return classNamePart;
	}

	/**
	 * Keeps the method name and method signature removing the return type
	 * 
	 * @param fullMethodDeclaration
	 * @return
	 */
	public static String getMethodDeclarationPart(String fullMethodDeclaration) {
		String methodDeclarationPart = "";
		if (StringUtils.isNotBlank(fullMethodDeclaration)) {
			int indexOfStartMethodName = fullMethodDeclaration.lastIndexOf('.');
			int indexOfEndMethodName = fullMethodDeclaration.lastIndexOf(')');
			methodDeclarationPart = fullMethodDeclaration.substring(indexOfStartMethodName + 1, indexOfEndMethodName + 1);
		}
		return methodDeclarationPart;
	}

	/**
	 * Keeps the return type only
	 * 
	 * @param fullMethodDeclaration
	 * @return
	 */
	public static String getReturnTypePart(String fullMethodDeclaration) {
		String returnTypePart = "";
		if (StringUtils.isNotBlank(fullMethodDeclaration)) {
			int indexOfEndMethodName = fullMethodDeclaration.lastIndexOf(')');
				returnTypePart = fullMethodDeclaration.substring(indexOfEndMethodName + 1);
		}
		return returnTypePart;
	}

	/**
	 * Keeps the package name part only
	 * 
	 * @param qualifiedClassName
	 * @return
	 */
	public static String getPackageNamePart(String qualifiedClassName) {
		String packagename = "";
		if (StringUtils.isNotBlank(qualifiedClassName)) {
			int lastPeriod = qualifiedClassName.lastIndexOf('.');
			if (lastPeriod >= 0) {
				packagename = qualifiedClassName.substring(0, lastPeriod);
			}
		}
		return packagename;
	}

	/**
	 * Keeps the short class name only.
	 * 
	 * @param qualifiedClassName
	 * @return
	 */
	public static String getClassNamePart(String qualifiedClassName) {
		String classname = "";
		if (StringUtils.isNotBlank(qualifiedClassName)) {
			int lastPeriod = qualifiedClassName.lastIndexOf('.');
			if (lastPeriod >= 0) {
				classname = qualifiedClassName.substring(lastPeriod + 1);
			}
		}
		return classname;
	}

	public static String getMethodCtxType(String methodName) {
		String methodType = Constants.CONSTRUCTOR_NAME.equals(methodName) ? Constants.METHOD_TYPE_CONSTRUCTOR : Constants.METHOD_TYPE_METHOD;
		return methodType;
	}

	/**
	 * Takes a method declaration as given by BCEL and formats it into a readable way
	 * 
	 * @param unreadableDeclaringType
	 * @return
	 */
	public static String makeDeclaringReadable(String unreadableDeclaringType) {
		// Change $ character to avoid regex errors
		String newMethodDeclarationPart = " " + unreadableDeclaringType.replaceAll("\\$", "@");
		try {
			// Decode method parameters
			Matcher parametersMatcher = methodParametersPattern.matcher(newMethodDeclarationPart);
			if (parametersMatcher.find()) {
				String methodParametersGroup = parametersMatcher.group(2);
				StringBuilder newParameters = new StringBuilder();
				for (int i = 0; i < methodParametersGroup.length(); i++) {
					char nextChar = methodParametersGroup.charAt(i);
					String decodedType = null;
					if (nextChar == 'L') {
						// Code was L
						int ixOfNextComma = methodParametersGroup.indexOf(Constants.STR_SEMICOLON, i);
						decodedType = methodParametersGroup.substring(i + 1, ixOfNextComma);
						i = ixOfNextComma;
					} else {
						decodedType = decodeMemberType(nextChar);
					}
					if (decodedType == null) {
						newParameters.append(nextChar);
						continue;
					}
					newParameters.append(decodedType).append(Constants.STR_SEMICOLON);
				}
				newMethodDeclarationPart = parametersMatcher.replaceAll("$1SET_METHOD_PARAMETERS$3");
				newMethodDeclarationPart = newMethodDeclarationPart.replaceFirst("SET_METHOD_PARAMETERS", newParameters.toString());
			}
			
			// Decode object arrays
			Matcher arraysMatcher = arraysPattern.matcher(newMethodDeclarationPart);
			if (arraysMatcher.find()) {
				newMethodDeclarationPart = arraysMatcher.replaceAll("$2\\[\\]$3");
			}

			// Decode return type
			Matcher rtnTypeMatcher = returnTypePattern.matcher(newMethodDeclarationPart);
			if (rtnTypeMatcher.find()) {
				String rtnTypeGroup = rtnTypeMatcher.group(2);
				if (rtnTypeGroup != null) {
					int stFull = rtnTypeMatcher.start();
					int enFull = rtnTypeMatcher.end();
					String fullGroups = newMethodDeclarationPart.substring(stFull, enFull);
					String decodedType = null;
					char nextChar = rtnTypeGroup.charAt(0); // Get the actual type char
					if (nextChar == 'L') {
						// Get the classname only
						decodedType = rtnTypeGroup.substring(1, rtnTypeGroup.length() - 1);
					} else {
						decodedType = decodeMemberType(nextChar);
						decodedType = StringUtils.replaceOnce(fullGroups, "" + nextChar, decodedType);
					}
					newMethodDeclarationPart = StringUtils.replaceOnce(newMethodDeclarationPart, rtnTypeGroup, decodedType);
				}
			}
			newMethodDeclarationPart = newMethodDeclarationPart.replaceAll("/", ".");
			newMethodDeclarationPart = newMethodDeclarationPart.replaceFirst(";\\)", ")");
			newMethodDeclarationPart = newMethodDeclarationPart.replaceAll(";", ", ");
			newMethodDeclarationPart = newMethodDeclarationPart.replaceAll("[\\s]{2,}", " ");
			newMethodDeclarationPart = newMethodDeclarationPart.replaceAll("@", "\\$");
		} catch (Exception e) {
			log.error(e.getMessage());
			log.error("An error occurred while making readable: " + unreadableDeclaringType);
			newMethodDeclarationPart = unreadableDeclaringType;
		}
		return newMethodDeclarationPart.trim();
	}

	public static String decodeMemberType(char code) {
		switch(code) {
		case 'B': return BYTE;
		case 'C': return CHAR;
		case 'D': return DOUBLE;
		case 'F': return FLOAT;
		case 'I': return INT;
		case 'J': return LONG;
		case 'S': return SHORT;
		case 'Z': return BOOLEAN;
		case 'V':
			return VOID;
		}
		return null;
	}

	/**
	 * TODO Compare with {@link #getClassContext(String)}
	 * 
	 * @param projectFolders
	 * @param classFilePath
	 * @return
	 */
	public static String getModuleName(List<String> projectFolders, String classFilePath) {
		classFilePath = classFilePath.replace(Constants.STR_SLASH, Constants.STR_SPACE);
		classFilePath = classFilePath.replace(Constants.STR_BACK_SLASH, Constants.STR_SPACE);
		for (String projectFolder : projectFolders) {
			// projectFolder = StringUtils.removeEnd(projectFolder, "bin");
			// projectFolder = StringUtils.removeEnd(projectFolder, "src");
			projectFolder = projectFolder.replace(Constants.STR_SLASH, Constants.STR_SPACE);
			projectFolder = projectFolder.replace(Constants.STR_BACK_SLASH, Constants.STR_SPACE);
			projectFolder = projectFolder.trim();
			if (classFilePath.startsWith(projectFolder)) {
				int lastIxOfSlash = projectFolder.lastIndexOf(Constants.STR_SPACE);
				String module = projectFolder.substring(lastIxOfSlash + 1);
				if (ArrayUtils.contains(Constants.GROUPING_MODULES, module)) {
					return module;
				}
			}
		}
		return Constants.DEFAULT_GROUPING_MODULE;
	}

	/**
	 * TODO Compare with {@link #getModuleName(List, String)}
	 * 
	 * @param filePath
	 * @return
	 */
	public static String getClassContext(String filePath) {
		if (StringUtils.contains(filePath, FOLDER_PATH_CCD)) {
			return CCD;
		}
		if (StringUtils.contains(filePath, FOLDER_PATH_FRAMEWORK_EJB) || StringUtils.contains(filePath, FOLDER_PATH_FRAMEWORK)) {
			return FRAMEWORK;
		}
		return OTHER;
	}

	public static void main(String[] args) {
		String s = null;
		s = makeDeclaringReadable("V simulateException(Ljava/lang/String;ILgov/illinois/fw/batch/AbstractBatchExceptionTypes;II)");
		System.out.println("1|" + s);
		s = makeDeclaringReadable("V simulateException(Ljava/lang/String;ILgov/illinois/fw/batch/AbstractBatch$ExceptionTypes;II)");
		System.out.println("2|" + s);
		s = makeDeclaringReadable("V <init>(Lgov/illinois/fw/persistence/helperclasses/HistoryCollectionPersister;Lgov/illinois/fw/persistence/helperclasses/HistoryCollectionPersister$UniqueCollection;)");
		System.out.println("3|" + s);
		s = makeDeclaringReadable("V buildCollections(Ljava/util/Collection;Ljava/sql/Connection;Lgov/illinois/fw/persistence/helperclasses/HistoryCollectionPersister$UniqueCollection;Ljava/lang/String;)");
		System.out.println("4|" + s);
		s = makeDeclaringReadable("V <init>(Lgov/illinois/fw/persistence/helperclasses/Type2HistoryPersister$SortByDate;)");
		System.out.println("5|" + s);
		s = makeDeclaringReadable(" [Lgov/illinois/ies/business/entities/correspondence/vo/VCoRequestAuthRepsCountVO; getVCoRequestRecords(Ljava/util/Map;)");
		System.out.println("|" + s);
		s = makeDeclaringReadable(" Ljava/util/Map; getRestartAttributes(Ljava/sql/Connection;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)");
		System.out.println("|" + s);
		s = makeDeclaringReadable(" V <init>()");
		System.out.println("|" + s);
		s = makeDeclaringReadable("installService(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;[Ljava/lang/Object;)");
		System.out.println("|" + s);
		s = makeDeclaringReadable("V convert4To3([B[BI)");
		System.out.println("|" + s);
		s = makeDeclaringReadable("I getAdjustedMaximumRunNoInt(Ljava/sql/Connection;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)");
		System.out.println("|" + s);
		s = makeDeclaringReadable("Lgov/illinois/ies/business/entities/correspondence/CoRequestRecipientsCargo; getRequestFeedFile(JJJLjava/util/List;)");
		System.out.println("|" + s);
		s = makeDeclaringReadable(" [B getBytes(Ljava/lang/String;)");
		System.out.println("|" + s);
		s = makeDeclaringReadable(" V setEnvTypeInd(C)");
		System.out.println(s);
		s = makeDeclaringReadable(" V setRestartAttributes(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/sql/Connection;Ljava/util/Map;JLjava/lang/String;)");
		System.out.println(s);
		s = makeDeclaringReadable("insertRunControlRecord(Ljava/sql/Connection;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Lgov/illinois/framework/management/error/FwErrorLogContextBean;)");
		System.out.println(s);
		s = makeDeclaringReadable(" V setErrorLogContextBean(Lgov/illinois/framework/management/error/FwErrorLogContextBean;Ljava/lang/String;Ljava/sql/Timestamp;JILjava/lang/String;Ljava/lang/String;)");
		System.out.println(s);
	}
}
