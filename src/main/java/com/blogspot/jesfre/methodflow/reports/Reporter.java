/**
 * 
 */
package com.blogspot.jesfre.methodflow.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.blogspot.jesfre.methodflow.common.ClassParsingUtils;
import com.blogspot.jesfre.methodflow.common.Configuration;
import com.blogspot.jesfre.methodflow.common.MethodCallComposite;
import com.blogspot.jesfre.methodflow.serialization.SerClass;
import com.blogspot.jesfre.methodflow.serialization.SerInstruction;
import com.blogspot.jesfre.methodflow.serialization.SerMethod;
import com.blogspot.jesfre.methodflow.serialization.SerMethodIndexEntry;
import com.blogspot.jesfre.methodflow.visitor.Constants;
import com.blogspot.jesfre.methodflow.visitor.Constants.ReportColumn;
import com.blogspot.jesfre.methodflow.visitor.MethodIndexer;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 *         Nov 29, 2016
 */
public class Reporter {
	private static Log log = LogFactory.getLog(Reporter.class);
	protected static final String SINGLE_VALUE_FORMAT = "\"%s\",";
	protected static String csvHeaderString = "";
	protected static String csvLineFormat = null;
	protected static boolean generateSingleSummaryReport = false;
	protected static boolean generateSinglePmdFeedFile = false;
	protected static ReportEngine singleSummaryReportCsv = null;
	protected static ReportEngine singlePmdFeedFileTxt = null;
	protected static boolean singleSummaryFileHeaderAppend = false;
	protected ReportEngine methodRegistrationReport = null;
	protected String methodRegistrationReportHeader = null;

	protected Map<String, SerMethodIndexEntry> methodIndex = new LinkedHashMap<String, SerMethodIndexEntry>();
	protected ReportEngine flowReportHtml = null;
	protected ReportEngine flowReportTxt = null;
	protected ReportEngine visitedMethodsReportCsv = null;
	protected ReportEngine visitedMethodsReportHtml = null;
	protected ReportEngine pmdFeedFileTxt = null;
	protected String entryClassName = null;
	protected String reportLocationPath = null;
	protected int funcCounter = 0;
	protected boolean visitedMethodsHeaderAlreadyCreated = false;

	/**
	 * The array of the listed-values of a single entry-line from the entry-file
	 */
	private String[] entryValueArray = null;

	/**
	 * Visited entry list holds entries of composed values with the format: fromClass.fromMethodDeclaration-toClass.toMethodDeclaration
	 */
	protected List<String> visitedEntryList = new ArrayList<String>();

	protected static int totalWrittenLines = 0;
	protected static String headerContent = null;

	static{
		String reportHtmlHeader = "com/blogspot/jesfre/methodflow/resources/reportheader.html";
		try {
			InputStream headerContentIs = Reporter.class.getClassLoader().getResourceAsStream(reportHtmlHeader);
			headerContent = IOUtils.toString(headerContentIs);
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public Reporter(String reportLocationPath, String entryClassName) {
		this.reportLocationPath = reportLocationPath;
		this.entryClassName = entryClassName;
	}

	private void initSingleSummaryReport() {
		if (generateSingleSummaryReport) {
			if (singleSummaryReportCsv == null) {
				String reportFileName = String.format(Constants.VISITED_METHODS_REPORT_CSV, "All_Classes");
				singleSummaryReportCsv = new ReportEngine(reportFileName, reportLocationPath, Configuration.maxRecordsPerFile);
			}
		}
	}

	private void initSinglePmdFeedFile() {
		if (generateSinglePmdFeedFile) {
			if (singlePmdFeedFileTxt == null) {
				String reportFileName = String.format(Constants.PMD_FEED_FILE_TXT, "All_Classes");
				singlePmdFeedFileTxt = new ReportEngine(reportFileName, reportLocationPath, Configuration.maxRecordsPerFile);
			}
		}
	}
	
	
	private void initMethodRegistrationReport() {
		if (methodRegistrationReport == null) {
			String reportFileName = "methodRegistry.csv";
			methodRegistrationReport = new ReportEngine(reportFileName, reportLocationPath, Configuration.maxRecordsPerFile);
		}
	}

	private void initFlowReportHtml() {
		if (flowReportHtml == null) {
			String reportFileName = String.format(Constants.FLOW_REPORT_HTML, entryClassName);
			flowReportHtml = new ReportEngine(reportFileName, reportLocationPath, 0);
			flowReportHtml.appendContent(headerContent, 0);
		}
	}

	private void initFlowReportTxt() {
		if (flowReportTxt == null) {
			String reportFileName = String.format(Constants.FLOW_REPORT_TXT, entryClassName);
			flowReportTxt = new ReportEngine(reportFileName, reportLocationPath, 0);
			flowReportTxt.disableConsoleLogging();
		}
	}

	private void initVisitedMethodsReportCsv() {
		if (visitedMethodsReportCsv == null) {
			String reportFileName = String.format(Constants.VISITED_METHODS_REPORT_CSV, entryClassName);
			visitedMethodsReportCsv = new ReportEngine(reportFileName, reportLocationPath, 0);
		}
	}

	private void initVisitedMethodsReportHtml() {
		if (visitedMethodsReportHtml == null) {
			String reportFileName = String.format(Constants.VISITED_METHODS_REPORT_HTML, entryClassName);
			visitedMethodsReportHtml = new ReportEngine(reportFileName, reportLocationPath, 0);
			visitedMethodsReportHtml.appendContent(headerContent, 0);
		}
	}

	private void initPmdFeedFileTxt() {
		if (pmdFeedFileTxt == null) {
			String reportFileName = String.format(Constants.PMD_FEED_FILE_TXT, entryClassName);
			pmdFeedFileTxt = new ReportEngine(reportFileName, reportLocationPath, 0);
		}
	}

	public void generateFlow(MethodCallComposite composite) throws Exception {
		initFlowReportHtml();
		initFlowReportTxt();
		generateFlow(composite, 0);
	}

	private void generateFlow(MethodCallComposite composite, int level) throws IOException {
		String methodDeclaration = composite.getMethodDeclaration();
		List<MethodCallComposite> instructionList = composite.getInstructionList();

		initFlowReportHtml();
		flowReportHtml.writeReport(false);

		initFlowReportTxt();
		flowReportTxt.writeReport(false);

		StringBuffer spaces = new StringBuffer();
		for (int i = 0; i < level; i++) {
			// tabs.append(" | ");
			spaces.append(" | ");
		}
		String btnPlus = "&nbsp;&nbsp;";
		String tabs = " ";
		int methodHash = Math.abs((methodDeclaration + (funcCounter++)).hashCode());
		if (instructionList.size() > 0) {
			btnPlus = "<span id='btn" + methodHash + "' name='expander' style='font-weight:bold;' onclick=\"javascript:toggle('" + methodHash + "')\">+&nbsp;</span>";
			tabs = "+";
		}
		flowReportTxt.appendLine(spaces + tabs + methodDeclaration);
		flowReportHtml.appendLine("<div style='margin-left:" + level * 20 + "px;'>" + btnPlus + methodDeclaration + "</div>");
		flowReportHtml.appendLine("<span id='content" + methodHash + "' name='container' style='display:none'>");

		for (MethodCallComposite instruction : instructionList) {
			String instructionDeclaration = instruction.getMethodDeclaration();
			// if (!printedMethods.contains(instructionDeclaration)) {
			// printedMethods.add(instructionDeclaration);
			generateFlow(instruction, level + 1);
			// } else {
			// flowReportHtml.appendLine("<div style='margin-left:" + level * 20 + "px;'>" + "&nbsp;&nbsp;" + methodDeclaration +
			// " [Printed]</div>");
			// flowReportTxt.appendLine(spaces + " " + methodDeclaration + " [Printed]");
			// }
		}
		flowReportHtml.appendLine("</span>");
	}

	public void writeReports(String outputColumns, SerClass entryClasss, SerMethod entryMethod, Collection<String> allVisitedClasses, Collection<MethodCallComposite> allVisitedMethods) throws IOException {
		flowReportHtml.writeReport(false);
		flowReportTxt.writeReport(false);

		List<String> sortedVisitedClasses = new ArrayList<String>(allVisitedClasses);
		Collections.sort(sortedVisitedClasses);
		Map<String, Set<String>> calledMethodsByClassMap = new LinkedHashMap<String, Set<String>>();
		for (MethodCallComposite methodComposite : allVisitedMethods) {
			String methodDeclaration = methodComposite.getMethodDeclaration();
			String classNamePart = ClassParsingUtils.getQualifiedClassnamePart(methodDeclaration);
			String methodNamePart = ClassParsingUtils.getMethodDeclarationPart(methodDeclaration);
			if (!calledMethodsByClassMap.containsKey(classNamePart)) {
				calledMethodsByClassMap.put(classNamePart, new LinkedHashSet<String>());
			}
			calledMethodsByClassMap.get(classNamePart).add(methodNamePart);
		}

		String[] columnOrder = StringUtils.split(outputColumns, ',');
		if (!visitedMethodsHeaderAlreadyCreated) {
			String headerStr = Reporter.buildCvsHeader(columnOrder);
			visitedMethodsReportCsv.appendLine(headerStr);
			visitedMethodsHeaderAlreadyCreated = true;
		}

		String classContext = ClassParsingUtils.getClassContext(entryClasss.getFileName());
		String file = entryClasss.getFileName();
		file = MethodIndexer.getAsJavaFile(file);
		String entryMethodDeclaration = ClassParsingUtils.getMethodDeclarationPart(entryMethod.getName() + entryMethod.getSignature());
		String methodType = Constants.CONSTRUCTOR_NAME.equals(entryMethod.getName()) ? Constants.METHOD_TYPE_CONSTRUCTOR : Constants.METHOD_TYPE_METHOD;
		String methodName = entryMethod.getName();
		String entryMethodReturnType = ClassParsingUtils.getReturnTypePart(entryMethodDeclaration);
		List<String> accessorList = MethodIndexer.getModifiers(entryMethod);
		StringBuilder fullEntryMethodDeclaration = new StringBuilder()
				.append(StringUtils.join(accessorList, Constants.STR_SPACE))
				.append(Constants.STR_SPACE).append(entryMethodReturnType)
				.append(Constants.STR_SPACE).append(entryMethodDeclaration);

		int funcCounter = 0;
		List<String> visitedMethodList = new ArrayList<String>();
		for (String visitedClass : sortedVisitedClasses) {
			if (calledMethodsByClassMap.containsKey(visitedClass)) {
				Set<String> visitedMethodNameList = calledMethodsByClassMap.get(visitedClass);

				String btnPlus = "&nbsp;&nbsp;";
				int classHash = Math.abs((visitedClass + (funcCounter++)).hashCode());
				if (visitedMethodNameList.size() > 0) {
					btnPlus = "<span id='btn" + classHash + "' name='expander' style='font-weight:bold;' onclick=\"javascript:toggle('" + classHash + "')\">+&nbsp;</span>";
				}
				visitedMethodsReportHtml.appendLine(btnPlus + visitedClass);
				visitedMethodsReportHtml.appendLine("<ul id='content" + classHash + "' name='container' style='display:none'>");

				for (String visitedMethod : visitedMethodNameList) {
					try {
						String fullMethodDecl = visitedClass + "." + visitedMethod;
						if (!visitedMethodList.contains(fullMethodDecl)) {
							visitedMethodList.add(fullMethodDecl);

							SerClass toClass = null;
							String toFile = "";
							SerMethod toMethod = null;
							String toMethodName = "";
							String toClassContext = "";
							List<String> toAccessorList = null;
							SerMethodIndexEntry methodInstruction = methodIndex.get(fullMethodDecl);
							if (methodInstruction != null) {
								toClass = methodInstruction.getOwnerClass();
								toClassContext = ClassParsingUtils.getClassContext(toClass.getFileName());
								toFile = MethodIndexer.getAsJavaFile(toClass.getFileName());

								toMethod = methodInstruction.getMethod();
								toMethodName = toMethod.getName();
								toAccessorList = MethodIndexer.getModifiers(toMethod);
							} else {
								toAccessorList = new ArrayList<String>();
							}

							String methodDeclarationPart = ClassParsingUtils.getMethodDeclarationPart(visitedMethod);
							String returnTypePart = ClassParsingUtils.getReturnTypePart(visitedMethod);
							String toMethodReturnType = ClassParsingUtils.getReturnTypePart(visitedMethod);
							StringBuilder toMethodDeclarationFull = new StringBuilder()
									.append(StringUtils.join(toAccessorList, Constants.STR_SPACE))
									.append(Constants.STR_SPACE).append(toMethodReturnType)
									.append(Constants.STR_SPACE).append(methodDeclarationPart);

							flowReportTxt.appendLine(visitedClass + "," + returnTypePart + " " + methodDeclarationPart);
							visitedMethodsReportHtml.appendLine("<li>" + visitedMethod + "</li>");

							StringBuilder line = new StringBuilder();
							// for (String column : columnOrder) {
							// if (Constants.COL_FROM_CLASS_TYPE.equals(column)) {
							// line.append(classContext).append(Constants.STR_COMMA);
							// }
							// . . .
							// if (Constants.COL_TO_FILE.equals(column)) {
							// line.append(toFile).append(Constants.STR_COMMA);
							// }
							// }
							// Removing last comma
							String lineString = line.toString();
							visitedMethodsReportCsv.appendLine(lineString.substring(0, lineString.length() - 1));
							totalWrittenLines++;
						}
					} catch (Exception e) {
						System.out.println("Exception while creating line for visited method: " + visitedMethod);
					}
				}
				visitedMethodsReportHtml.appendLine("</ul><br/>");
			} else {
				// System.err.println(visitedClass + " is not mapped in visited methods.");
			}

		}
		// System.out.println("Total writen lines: " + totalWrittenLines);
		visitedMethodsReportCsv.writeReport(false);
		visitedMethodsReportHtml.writeReport(false);
	}

	/**
	 * Generates the corresponding reports for the analyzed class/method.
	 * 
	 * @param entryMethodCompositionList the list of entry object that holds the entire call flow after the analysis
	 * @throws Exception
	 */
	public void generateVisitedMethodsReport(List<MethodCallComposite> entryMethodCompositionList) throws Exception {
		initVisitedMethodsReportCsv();
		initVisitedMethodsReportHtml();
		initSingleSummaryReport();

		csvHeaderString = Reporter.buildCvsHeader(Configuration.outputColumns);
		visitedMethodsReportCsv.appendLine(csvHeaderString);
		visitedMethodsReportCsv.writeReport(false);
		if (!singleSummaryFileHeaderAppend) {
			singleSummaryReportCsv.appendHeader(csvHeaderString);
			singleSummaryReportCsv.writeReport(false);
			singleSummaryFileHeaderAppend = true;
		}

		for (MethodCallComposite entryMethodComposition : entryMethodCompositionList) {
			// printMethodCallCompositionEntries(entryMethodComposition, 0);

			csvLineFormat = getCsvLineFormat(entryMethodComposition);

			List<MethodCallComposite> methodCallList = entryMethodComposition.getInstructionList();
			for (MethodCallComposite call : methodCallList) {
				this.generateVisitedMethodsReportLine(visitedMethodsReportCsv, Configuration.outputColumns, entryMethodComposition, call, 0);
			}
		}
	}

	public void generateVisitedMethodsReportLine(ReportEngine reportEngine, String[] columnOrder, MethodCallComposite fromMethodComposition, MethodCallComposite toMethodComposition, int level) throws IOException {
		SerClass fromClass = fromMethodComposition.getClazz();
		SerMethod fromMethod = fromMethodComposition.getMethod();

		List<MethodCallComposite> methodCallList = fromMethodComposition.getInstructionList();
		for (MethodCallComposite call : methodCallList) {
			String callFullMethodDeclaration = call.getMethodDeclaration();
			SerClass callClazz = call.getClazz();
			SerMethod callMethod = call.getMethod();
			if (callMethod == null) {
				System.out.println(callMethod);
			}

			String visitedEntry = fromClass.getClassName() + "." + fromMethod.getName() + fromMethod.getSignature()
					+ "-" + callFullMethodDeclaration;
			if (visitedEntryList.contains(visitedEntry)) {
				// Do not print the same call twice
				continue;
			}

			String lineString = this.getFormatedLine(fromClass, fromMethod, callClazz, callMethod);
			reportEngine.appendLine(lineString);

			if (generateSingleSummaryReport) {
				singleSummaryReportCsv.appendLine(lineString);
			}

			if (Configuration.maxDepth > 0 && level >= Configuration.maxDepth) {
				return;
			}
			visitedEntryList.add(visitedEntry);

			List<MethodCallComposite> callMethodCallList = call.getInstructionList();
			for (MethodCallComposite innerCall : callMethodCallList) {
				this.generateVisitedMethodsReportLine(reportEngine, columnOrder, call, innerCall, level + 1);
			}
			reportEngine.writeReport(false);
			if (generateSingleSummaryReport) {
				singleSummaryReportCsv.writeReport(false);
			}
		}
	}

	private static Object[] getPrintableValues(Map<ReportColumn, String> valueMap) {
		List<Object> columnValues = new ArrayList<Object>();
		for (int i = 0; i < Configuration.outputColumns.length; i++) {
			String column = Configuration.outputColumns[i];
			if (column.startsWith(Constants.ENTRY_VAL_PREFIX)) {
				/*
				 * Skip since these values are already added as part of the template
				 */
				continue;
			}

			ReportColumn colEnum = ReportColumn.valueOf(column);
			String value = valueMap.get(colEnum);
			if (StringUtils.isNotBlank(value)) {
				columnValues.add(value);
			}
		}
		return columnValues.toArray();
	}

	private String getCsvLineFormat(MethodCallComposite entryMethodComposition) {
		String newCsvLineFormat = null;
		StringBuilder csvLineFormatSb = new StringBuilder();
		for (int i = 0; i < Configuration.outputColumns.length; i++) {
			String curCol = Configuration.outputColumns[i];
			if (curCol.startsWith(Constants.ENTRY_VAL_PREFIX)) {
				String[] entryValDeclaration = StringUtils.split(curCol, ':');
				String entryValNum = entryValDeclaration[0];
				String displayName = entryValDeclaration[1];
				int ix = Integer.valueOf(entryValNum.substring(Constants.ENTRY_VAL_PREFIX.length()));
				if(ix > 0) {
					// Removes 1 since the entry values are 1 based index 
					ix -= 1;
					/*
					 * Only append a value when it actually is defined in the list of values of the entry-line,
					 * otherwise display empty
					 */
					if (entryValueArray.length > ix) {
						String displayValue = entryValueArray[ix];
						csvLineFormatSb.append("\"").append(displayValue).append("\",");
					} else {
						csvLineFormatSb.append("\"\",");
					}
				}
			} else {
				csvLineFormatSb.append(SINGLE_VALUE_FORMAT);
			}
		}
		// Removing last comma
		newCsvLineFormat = csvLineFormatSb.substring(0, csvLineFormatSb.length() - 1);

		String anyString = "%s";
		Map<ReportColumn, String> mapOfValues = new HashMap<ReportColumn, String>();
		mapOfValues.put(ReportColumn.FROM_MODULE, "TODO");
		mapOfValues.put(ReportColumn.FROM_CLASS, anyString);
		mapOfValues.put(ReportColumn.FROM_FILE, anyString);
		mapOfValues.put(ReportColumn.FROM_CLASS_TYPE, anyString);
		mapOfValues.put(ReportColumn.FROM_METHOD_NAME, anyString);
		mapOfValues.put(ReportColumn.FROM_METHOD_TYPE, anyString);
		mapOfValues.put(ReportColumn.FROM_METHOD_DECLARATION, anyString);
		mapOfValues.put(ReportColumn.TO_MODULE, anyString);
		mapOfValues.put(ReportColumn.TO_CLASS, anyString);
		mapOfValues.put(ReportColumn.TO_FILE, anyString);
		mapOfValues.put(ReportColumn.TO_CLASS_TYPE, anyString);
		mapOfValues.put(ReportColumn.TO_METHOD_NAME, anyString);
		mapOfValues.put(ReportColumn.TO_METHOD_DECLARATION, anyString);

		Object[] columnValues = Reporter.getPrintableValues(mapOfValues);
		newCsvLineFormat = String.format(newCsvLineFormat, columnValues);
		return newCsvLineFormat;
	}

	/**
	 * @param toClass
	 * @param toMethod
	 * @return
	 */
	private String getFormatedLine(SerClass fromClass, SerMethod fromMethod, SerClass toClass, SerMethod toMethod) {
		String fromMethodName = fromMethod.getName();
		String fromMethodDeclaration = fromMethodName + fromMethod.getSignature();
		String fromClassName = fromClass.getClassName();
		String fromFile = fromClass.getFileName();
		fromFile = MethodIndexer.getAsJavaFile(fromFile);
		String fromClassContext = ClassParsingUtils.getClassContext(fromFile);

		String fromMethodShortDeclaration = ClassParsingUtils.getMethodDeclarationPart(fromMethodDeclaration);
		String fromMethodType = ClassParsingUtils.getMethodCtxType(fromMethodName);
		String fromMethodReturnType = ClassParsingUtils.getReturnTypePart(fromMethodDeclaration);
		List<String> fromAccessorList = MethodIndexer.getModifiers(fromMethod);
		StringBuilder printableMethodDeclaration = new StringBuilder()
				.append(StringUtils.join(fromAccessorList, Constants.STR_SPACE))
				.append(Constants.STR_SPACE).append(fromMethodReturnType)
				.append(Constants.STR_SPACE).append(fromMethodShortDeclaration);
		String printableFromMethodDeclarationStr = ClassParsingUtils.makeDeclaringReadable(printableMethodDeclaration.toString());

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
		String printableToMethodDeclarationStr = ClassParsingUtils.makeDeclaringReadable(toMethodDeclarationFullSb.toString());

		Map<ReportColumn, String> mapOfValues = new HashMap<ReportColumn, String>();
		mapOfValues.put(ReportColumn.FROM_MODULE, "TODO");
		mapOfValues.put(ReportColumn.FROM_CLASS, fromClassName);
		mapOfValues.put(ReportColumn.FROM_FILE, fromFile);
		mapOfValues.put(ReportColumn.FROM_CLASS_TYPE, fromClassContext);
		mapOfValues.put(ReportColumn.FROM_METHOD_NAME, fromMethodName);
		mapOfValues.put(ReportColumn.FROM_METHOD_TYPE, fromMethodType);
		mapOfValues.put(ReportColumn.FROM_METHOD_DECLARATION, printableFromMethodDeclarationStr);
		mapOfValues.put(ReportColumn.TO_MODULE, "TODO");
		mapOfValues.put(ReportColumn.TO_CLASS, toClassName);
		mapOfValues.put(ReportColumn.TO_FILE, toFile);
		mapOfValues.put(ReportColumn.TO_CLASS_TYPE, toContext);
		mapOfValues.put(ReportColumn.TO_METHOD_NAME, toMethodName);
		mapOfValues.put(ReportColumn.TO_METHOD_DECLARATION, printableToMethodDeclarationStr);
		Object[] columnValues = Reporter.getPrintableValues(mapOfValues);

		String lineString = String.format(csvLineFormat, columnValues);
		return lineString;
	}

	public void generateMethodRegistryReport(String[] columnOrder) throws IOException {
		long startingTime = System.currentTimeMillis();
		log.info("Starting reporting at " + new Date(startingTime));

		initMethodRegistrationReport();
		methodRegistrationReportHeader = Reporter.buildCvsHeader(columnOrder);
		methodRegistrationReport.appendHeader(methodRegistrationReportHeader);

		Map<String, JavaClass> classLoader = MethodIndexer.getClassLoader();
		for (final Entry<String, JavaClass> clEntry : classLoader.entrySet()) {
			generateMethodRegistryReportLine(clEntry);
		}

		long endingTime = System.currentTimeMillis();
		log.info("Ending reporting at " + new Date(endingTime));
		long totalMillis = endingTime - startingTime;
		long millis = totalMillis % 1000;
		long totalSecs = totalMillis / 1000;
		long secs = totalSecs % 60;
		long mins = totalSecs / 60;
		log.info(String.format("Total reporting time: %d:%d.%d", mins, secs, millis));
	}

	/**
	 * @param classLoaderEntry
	 * @throws IOException
	 */
	private void generateMethodRegistryReportLine(Entry<String, JavaClass> classLoaderEntry) throws IOException {
		MethodIndexer methodIndexer = MethodIndexer.getInstance();
		Map<String, SerMethodIndexEntry> classMethodIndex = methodIndexer.indexClass(classLoaderEntry, Configuration.filter, Configuration.maxDepth);
		for (SerMethodIndexEntry indexEntry : classMethodIndex.values()) {
			if (indexEntry == null) {
				continue;
			}
			SerClass clazz = indexEntry.getOwnerClass();
			String file = clazz.getFileName();
			file = MethodIndexer.getAsJavaFile(file);
			String methodDeclaration = indexEntry.getMethodDeclaration();
			SerMethod method = indexEntry.getMethod();

			MethodCallComposite tempComposite = new MethodCallComposite();
			tempComposite.setMethodDeclaration(methodDeclaration);
			tempComposite.setClazz(clazz);
			tempComposite.setMethod(method);
			csvLineFormat = getCsvLineFormat(tempComposite);

			Collection<SerInstruction> instructionList = indexEntry.getInstructionList();
			if (instructionList.size() > 0) {
				// Has TO columns instructions
				for (SerInstruction instEntry : instructionList) {
					SerClass toClass = instEntry.getSerClass();
					SerMethod toMethod = instEntry.getSerMethod();

					String lineString = this.getFormatedLine(clazz, method, toClass, toMethod);
					methodRegistrationReport.appendLine(lineString);
					methodRegistrationReport.writeReport(false);
				}
			} else {
				// Does not has TO columns instructions
				Map<ReportColumn, String> mapOfValues = new HashMap<ReportColumn, String>();
				mapOfValues.put(ReportColumn.TO_MODULE, Constants.STR_HYPHEN);
				mapOfValues.put(ReportColumn.TO_CLASS, Constants.STR_HYPHEN);
				mapOfValues.put(ReportColumn.TO_FILE, Constants.STR_HYPHEN);
				mapOfValues.put(ReportColumn.TO_CLASS_TYPE, Constants.STR_HYPHEN);
				mapOfValues.put(ReportColumn.TO_METHOD_NAME, Constants.STR_HYPHEN);
				mapOfValues.put(ReportColumn.TO_METHOD_DECLARATION, Constants.STR_HYPHEN);
				Object[] columnValues = Reporter.getPrintableValues(mapOfValues);

				String lineString = String.format(csvLineFormat, columnValues);
				methodRegistrationReport.appendLine(lineString);
				methodRegistrationReport.writeReport(false);
			}
		}
		// if (methodRegistrationReportRecordCounter % 1000 == 0) {
		// System.out.println("File " + methodRegistrationReportFileCounter + "> " + methodRegistrationReportRecordCounter + " lines printed.");
		// }
		// Clear before load a new class into the index
		methodIndexer.getMethodIndex().clear();
	}

	public void generatePmdFeedFile(Collection<String> visitedClasses) throws IOException {
		initPmdFeedFileTxt();
		initSinglePmdFeedFile();

		List<String> sortedVisitedClasses = new ArrayList<String>(visitedClasses);
		Collections.sort(sortedVisitedClasses);
		
		String fileContent = StringUtils.join(sortedVisitedClasses, '\n');
		pmdFeedFileTxt.appendContent(fileContent, sortedVisitedClasses.size());
		pmdFeedFileTxt.writeReport(false);
		if (generateSinglePmdFeedFile) {
			singlePmdFeedFileTxt.appendContent(fileContent, sortedVisitedClasses.size());
			singlePmdFeedFileTxt.writeReport(false);
		}
	}

	public static String buildCvsHeader(String[] outputColumns) {
		StringBuffer header = new StringBuffer();
		for (String outCol : outputColumns) {
			if (outCol.startsWith(Constants.ENTRY_VAL_PREFIX)) {
				String[] entryValDeclaration = StringUtils.split(outCol, ':');
				String entryValNum = entryValDeclaration[0];
				String displayName = entryValDeclaration[1];
				header.append(displayName);
			} else {
				header.append(outCol);
			}
			header.append(',');
		}
		return header.toString();
	}

	public void flowReportHtmlSwitch(boolean on) {
		initFlowReportHtml();
		if (on) {
			flowReportHtml.on();
		} else {
			flowReportHtml.off();
		}
	}

	public void flowReportTxtSwitch(boolean on) {
		initFlowReportTxt();
		if (on) {
			flowReportTxt.on();
		} else {
			flowReportTxt.off();
		}
	}

	public void visitedMethodsReportHtmlSwitch(boolean on) {
		initVisitedMethodsReportHtml();
		if (on) {
			visitedMethodsReportHtml.on();
		} else {
			visitedMethodsReportHtml.off();
		}
	}

	public void visitedMethodsReportCvsSwitch(boolean on) {
		initVisitedMethodsReportCsv();
		if (on) {
			visitedMethodsReportCsv.on();
		} else {
			visitedMethodsReportCsv.off();
		}
	}

	public void pmdFeedFileSwitch(boolean on) {
		initPmdFeedFileTxt();
		if (on) {
			pmdFeedFileTxt.on();
		} else {
			pmdFeedFileTxt.off();
		}
	}

	public void setMethodIndex(Map<String, SerMethodIndexEntry> methodIndex) {
		this.methodIndex = methodIndex;
	}

	public static void setGenerateSingleSummaryReport(boolean generateSingleSummaryReport) {
		Reporter.generateSingleSummaryReport = generateSingleSummaryReport;
	}

	public static void setGenerateSinglePmdFeedFile(boolean generateSinglePmdFeedFile) {
		Reporter.generateSinglePmdFeedFile = generateSinglePmdFeedFile;
	}

	public static void printMethodCallCompositionEntries(MethodCallComposite entryMethodComposition, int level) {
		log.debug("Method entry: " + entryMethodComposition.getMethodDeclaration());
		for (MethodCallComposite newCall : entryMethodComposition.getInstructionList()) {
			printMethodCallCompositionEntries(newCall, level + 1);
		}
	}

	public void setEntryValueArray(String[] entryValueArray) {
		this.entryValueArray = entryValueArray;
	}

}
