/**
 * 
 */
package com.blogspot.jesfre.methodflow.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.blogspot.jesfre.methodflow.common.ClassParsingUtils;
import com.blogspot.jesfre.methodflow.common.Configuration;
import com.blogspot.jesfre.methodflow.common.MethodCallComposite;
import com.blogspot.jesfre.methodflow.serialization.SerClass;
import com.blogspot.jesfre.methodflow.serialization.SerMethod;
import com.blogspot.jesfre.methodflow.visitor.Constants;
import com.blogspot.jesfre.methodflow.visitor.Constants.ReportColumn;
import com.blogspot.jesfre.methodflow.visitor.MethodIndexer;

/**
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Nov 29, 2016
 */
public abstract class AbstractReporter {
	private static Log log = LogFactory.getLog(AbstractReporter.class);
	protected static final String SINGLE_VALUE_FORMAT = "\"%s\",";
	protected static String csvHeaderString = "";
	protected static String csvLineFormat = null;

	protected static ReportEngine globalStatisticsRerport = null;
	protected static ReportEngine individualStatisticsRerport = null;
	protected static int singleSummaryFileCounter = 0;
	protected static int singlePmdFeedFileCounter = 0;
	protected static int singleSummaryFileRecordCounter = 0;
	protected static int singlePmdFeedFileRecordCounter = 0;
	protected static boolean singleSummaryFileHeaderAppend = false;
	protected ReportEngine methodRegistrationReport = null;
	protected static int methodRegistrationReportFileCounter = 0;
	protected static int methodRegistrationReportRecordCounter = 0;
	protected String methodRegistrationReportHeader = null;

	protected ReportEngine flowReportHtml = null;
	protected ReportEngine flowReportTxt = null;
	protected ReportEngine visitedMethodsReportCsv = null;
	protected ReportEngine visitedMethodsReportHtml = null;
	protected ReportEngine pmdFeedFileTxt = null;
	protected String entryClassName = null;
	protected String reportLocationPath = null;
	protected int funcCounter = 0;
	protected boolean visitedMethodsHeaderAlreadyCreated = false;
	protected List<String> visitedMethodList = new ArrayList<String>();

	protected static int totalWrittenLines = 0;
	protected static String headerContent = null;

	static{
		String reportHtmlHeader = "com/blogspot/jesfre/methodflow/resources/reportheader.html";
		try {
			InputStream headerContentIs = AbstractReporter.class.getClassLoader().getResourceAsStream(reportHtmlHeader);
			headerContent = IOUtils.toString(headerContentIs);
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public AbstractReporter(String reportLocationPath, String entryClassName) {
		this.reportLocationPath = reportLocationPath;
		this.entryClassName = entryClassName;
	}
	
	protected static Object[] getPrintableValues(Map<ReportColumn, String> valueMap) {
		Object[] columnValues = new Object[Configuration.outputColumns.length];
		int lastInsertion = 0;
		for (int i = 0; i < Configuration.outputColumns.length; i++) {
			String column = Configuration.outputColumns[i];
			ReportColumn colEnum = ReportColumn.valueOf(column);
			String value = valueMap.get(colEnum);
			if (StringUtils.isNotBlank(value)) {
				columnValues[lastInsertion++] = value;
			}
		}
		return columnValues;
	}

	protected String getCsvLineFormat(MethodCallComposite entryMethodComposition) {
		String newCsvLineFormat = null;
		StringBuilder csvLineFormatSb = new StringBuilder();
		for (int i = 0; i < Configuration.outputColumns.length; i++) {
			csvLineFormatSb.append(SINGLE_VALUE_FORMAT);
		}
		// Removing last comma
		newCsvLineFormat = csvLineFormatSb.substring(0, csvLineFormatSb.length() - 1);

		String fullMethodDeclaration = entryMethodComposition.getMethodDeclaration();
		SerClass clazz = entryMethodComposition.getClazz();
		SerMethod method = entryMethodComposition.getMethod();

		String className = clazz.getClassName();
		String file = clazz.getFileName();
		file = MethodIndexer.getAsJavaFile(file);
		String classContext = ClassParsingUtils.getClassContext(file);

		String methodShortDeclaration = ClassParsingUtils.getMethodDeclarationPart(fullMethodDeclaration);
		String methodName = method.getName();
		String methodType = ClassParsingUtils.getMethodCtxType(methodName);
		String methodReturnType = ClassParsingUtils.getReturnTypePart(fullMethodDeclaration);
		List<String> accessorList = MethodIndexer.getModifiers(method);
		StringBuilder printableMethodDeclaration = new StringBuilder()
				.append(StringUtils.join(accessorList, Constants.STR_SPACE))
				.append(Constants.STR_SPACE).append(methodReturnType)
				.append(Constants.STR_SPACE).append(methodShortDeclaration);
		String printableMethodDeclarationStr = ClassParsingUtils.makeDeclaringReadable(printableMethodDeclaration.toString());

		String anyString = "%s";
		Map<ReportColumn, String> mapOfValues = new HashMap<ReportColumn, String>();
		mapOfValues.put(ReportColumn.FROM_MODULE, "TODO");
		mapOfValues.put(ReportColumn.FROM_CLASS, className);
		mapOfValues.put(ReportColumn.FROM_FILE, file);
		mapOfValues.put(ReportColumn.FROM_CLASS_TYPE, classContext);
		mapOfValues.put(ReportColumn.FROM_METHOD_NAME, methodName);
		mapOfValues.put(ReportColumn.FROM_METHOD_TYPE, methodType);
		mapOfValues.put(ReportColumn.FROM_METHOD_DECLARATION, printableMethodDeclarationStr);
		mapOfValues.put(ReportColumn.TO_MODULE, anyString);
		mapOfValues.put(ReportColumn.TO_CLASS, anyString);
		mapOfValues.put(ReportColumn.TO_FILE, anyString);
		mapOfValues.put(ReportColumn.TO_CLASS_TYPE, anyString);
		mapOfValues.put(ReportColumn.TO_METHOD_NAME, anyString);
		mapOfValues.put(ReportColumn.TO_METHOD_DECLARATION, anyString);

		Object[] columnValues = AbstractReporter.getPrintableValues(mapOfValues);
		newCsvLineFormat = String.format(newCsvLineFormat, columnValues);
		return newCsvLineFormat;
	}

	/**
	 * @param toClass
	 * @param toMethod
	 * @return
	 */
	protected String getFormatedLine(SerClass toClass, SerMethod toMethod) {
		String toClassName = toClass.getClassName();
		String toFile = toClass.getFileName();
		toFile = MethodIndexer.getAsJavaFile(toFile);
		String toContext = ClassParsingUtils.getClassContext(toFile);

		String toMethodName = toMethod.getName();
		String toMethodDeclaration = toMethodName + toMethod.getSignature();
		List<String> toMethodAccessorList = MethodIndexer.getModifiers(toMethod);
		String toMethodReturnType = ClassParsingUtils.getReturnTypePart(toMethodDeclaration);
		String toMethodDeclarationPart = ClassParsingUtils.getMethodDeclarationPart(toMethodDeclaration);
		StringBuilder toMethodDeclarationFullSb = new StringBuilder()
				.append(StringUtils.join(toMethodAccessorList, Constants.STR_SPACE))
				.append(Constants.STR_SPACE).append(toMethodReturnType)
				.append(Constants.STR_SPACE).append(toMethodDeclarationPart);
		String toMethodDeclarationFull = ClassParsingUtils.makeDeclaringReadable(toMethodDeclarationFullSb.toString());

		Map<ReportColumn, String> mapOfValues = new HashMap<ReportColumn, String>();
		mapOfValues.put(ReportColumn.TO_MODULE, "TODO");
		mapOfValues.put(ReportColumn.TO_CLASS, toClassName);
		mapOfValues.put(ReportColumn.TO_FILE, toFile);
		mapOfValues.put(ReportColumn.TO_CLASS_TYPE, toContext);
		mapOfValues.put(ReportColumn.TO_METHOD_NAME, toMethodName);
		mapOfValues.put(ReportColumn.TO_METHOD_DECLARATION, toMethodDeclarationFull);
		Object[] columnValues = AbstractReporter.getPrintableValues(mapOfValues);

		String lineString = String.format(csvLineFormat, columnValues);
		return lineString;
	}

	public static String buildCvsHeader(String[] outputColumns) {
		String header = StringUtils.join(outputColumns, ',');
		return header.toString();
	}

	public static void printMethodCallCompositionEntries(MethodCallComposite entryMethodComposition, int level) {
		log.debug("Method entry: " + entryMethodComposition.getMethodDeclaration());
		for (MethodCallComposite newCall : entryMethodComposition.getInstructionList()) {
			printMethodCallCompositionEntries(newCall, level + 1);
		}
	}

	/**
	 * 
	 * @return the new file path
	 */
	public abstract String generateReport(Object... reportParameters);
}
