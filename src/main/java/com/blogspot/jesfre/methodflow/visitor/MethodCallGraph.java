package com.blogspot.jesfre.methodflow.visitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.blogspot.jesfre.methodflow.common.ClassLoaderUtils;
import com.blogspot.jesfre.methodflow.common.ClassParsingUtils;
import com.blogspot.jesfre.methodflow.common.Configuration;
import com.blogspot.jesfre.methodflow.common.MethodCallComposite;
import com.blogspot.jesfre.methodflow.common.VisitedMethod;
import com.blogspot.jesfre.methodflow.reports.ReportEngine;
import com.blogspot.jesfre.methodflow.reports.Reporter;
import com.blogspot.jesfre.methodflow.serialization.SerClass;
import com.blogspot.jesfre.methodflow.serialization.SerInstruction;
import com.blogspot.jesfre.methodflow.serialization.SerMethod;
import com.blogspot.jesfre.methodflow.serialization.SerMethodIndexEntry;
import com.blogspot.jesfre.methodflow.serialization.SerializationUtils;

/**
 * Generates a graph of the method calls from a method entry point.
 * 
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 *         Dec 2, 2016
 */
public class MethodCallGraph {
	public static Log log = LogFactory.getLog(MethodCallGraph.class);

	private static Map<String, JavaClass> classLoader = new LinkedHashMap<String, JavaClass>();

	private static Map<String, VisitedMethod> visitedCommonMethods = new LinkedHashMap<String, VisitedMethod>();

	private static Set<String> allCollectionWithNoDao = new HashSet<String>();

	private static MethodIndexer methodIndexer = MethodIndexer.getInstance();

	private static Map<String, Integer> methodsMultipleCalls = new HashMap<String, Integer>();

	private static Map<String, List<String>> statisticReportList = new LinkedHashMap<String, List<String>>();

	private static int remainingClassesToProcess = 0;

	private static String reportPath = null;

	private static int globalMaxDepthReached = 0;

	private static boolean classLoaderFedDrivenRun = false;
	
	private static ReportEngine globalStatisticReport = null;

	private static boolean detectInfiniteCycles = true;

	private static Map<String, List<String>> infiniteCycleMap = new LinkedHashMap<String, List<String>>();

	private static Pattern METHOD_SPLIT_PATTERN = Pattern.compile("\\.[a-zA-Z0-9_$]*\\(");

	private Map<String, VisitedMethod> visitedMethods = null;

	private Set<String> allVisitedClasses = new HashSet<String>();

	private Set<String> visitedClasses = null;

	private LinkedList<String> processingMethodsQueue = null;

	private int maxDepthReached = 0;

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			Configuration.workingLocationPath = args[0];
		}
		if (StringUtils.isBlank(Configuration.workingLocationPath)) {
			log.error("Working directory is needed.");
			System.exit(-1);
		}

		String configurationFileName = null;
		if (args.length > 1) {
			configurationFileName = args[1];
		} else {
			configurationFileName = "config.properties";
		}

		long heapSize = Runtime.getRuntime().totalMemory();
		log.info("Heap size (MB): " + heapSize / 1024 / 1024);

        try {
			File configFile = new File(Configuration.workingLocationPath + File.separator + configurationFileName);
			if (configFile.exists() == false) {
				log.error("No cofig file found with name " + configurationFileName);
				System.exit(-1);
			}

			Configuration.loadConfigurations(configFile);

			if (Configuration.maxDepth <= 0) {
				log.warn("Max depth is set to UNLIMITED.");
			}

			if (Configuration.numberOfClassesToTest > 0) {
				log.info("--------------------------");
				log.info("Running in test mode!");
				log.info("--------------------------");
			}

			String entryFilePath = Configuration.properties.getProperty("entryFile.path");
			File entryFile = new File(entryFilePath);
			if (!entryFile.exists()) {
				entryFile = new File(Configuration.workingLocationPath + File.separator + entryFilePath);
				if (!entryFile.exists()) {
					throw new FileNotFoundException("Provided file [" + entryFilePath + "] does not exist.");
				}
			}

			String entryFileBaseName = FilenameUtils.getBaseName(entryFilePath);
			reportPath = new StringBuilder()
					.append(Configuration.workingLocationPath)
					.append(File.separator).append(Constants.REPORTS_FOLDER)
					.append(File.separator).append(entryFileBaseName)
					.append(File.separator).toString();

			Reporter.setGenerateSingleSummaryReport(Configuration.rpSingleSummaryCsvSw);
			Reporter.setGenerateSinglePmdFeedFile(Configuration.rpSinglePmdTxtSw);

			classLoader = methodIndexer.loadClasses(Configuration.projectClassFolders, Configuration.filter);

			// Indexes only declared methods
			methodIndexer.indexAll(Configuration.filter, Configuration.maxDepth);

			List<String> entryFileLines = FileUtils.readLines(entryFile);
			List<String> processingClassList = new ArrayList<String>();
			for (String entryClassName : entryFileLines) {
				if (StringUtils.isBlank(entryClassName) || entryClassName.trim().startsWith("#")) {
					continue;
				}
				processingClassList.add(entryClassName);
			}
			if (processingClassList.size() == 0) {
				classLoaderFedDrivenRun = true;
				processingClassList = new ArrayList<String>(classLoader.keySet());
			}

			int processedFiles = 0;
			remainingClassesToProcess = processingClassList.size();
			long startingTime = System.currentTimeMillis();
			log.info("Starting graphs at " + new Date(startingTime));

			ExecutorService pool = Executors.newFixedThreadPool(Configuration.graphMaxThreads);
			for (String entryLine : processingClassList) {
				if (StringUtils.isBlank(entryLine) || entryLine.trim().startsWith("#")) {
					// Comment or empty line
					remainingClassesToProcess--;
					continue;
				}
				
				String entryClassName = null;
				String entryMethodDeclaration = null;
				String[] entryValues = StringUtils.splitPreserveAllTokens(entryLine, '|');
				int valLength = entryValues.length;
				if (valLength > 0) {
					entryClassName = entryValues[0].trim();
				}
				if (valLength > 1) {
					entryMethodDeclaration = entryValues[1].trim();
				}

				if (StringUtils.isNotBlank(entryMethodDeclaration)) {
					boolean allowedMethodName = entryMethodDeclaration.matches("^[a-zA-Z0-9_$\\.]*$");
					if (!allowedMethodName) {
						log.error("Not allowed method name: " + entryClassName + "." + entryMethodDeclaration);
						remainingClassesToProcess--;
						continue;
					}
				}

				// Test - EJB no qualified class names - START
				// String prevEntryClassName = entryClassName;
				// for (String qclassname : classLoader.keySet()) {
				// if (qclassname.endsWith("." + entryClassName)) {
				// entryClassName = qclassname;
				// break;
				// }
				// }
				// if (entryClassName.equals(prevEntryClassName)) {
				// // Class not found in classloader
				// continue;
				// }
				// Test - EJB no qualified class names - END

				final GenerateGraphContext ctx = new GenerateGraphContext();
				ctx.setMethodIndex(MethodIndexer.getMethodIndex());
				ctx.setEntryClassName(entryClassName);
				ctx.setEntryMethodDeclaration(entryMethodDeclaration);
				ctx.setEntryValueArray(entryValues);

				boolean threadable = true;
				if (threadable) {
					Runnable r = new Runnable() {
						public void run() {
							try {
								new MethodCallGraph().generateGraph(ctx);
							} catch (Throwable e) {
								log.error("Error while generating graph for " + ctx.getEntryClassName());
								e.printStackTrace();
							}
						}
					};
					pool.execute(r);
				} else {
					try {
						new MethodCallGraph().generateGraph(ctx);
					} catch (Throwable e) {
						log.error("Error while generating graph for " + ctx.getEntryClassName());
						e.printStackTrace();
					}
				}
				processedFiles++;
				if (Configuration.numberOfClassesToTest > 0 && processedFiles > Configuration.numberOfClassesToTest) {
					break;
				}
			}
			pool.shutdown();
			try {
				pool.awaitTermination(Configuration.graphTimeoutMins, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endingTime = System.currentTimeMillis();
			log.info("Ending graphs at " + new Date(endingTime));
			long totalMillis = endingTime - startingTime;
			long millis = totalMillis % 1000;
			long totalSecs = totalMillis / 1000;
			long secs = totalSecs % 60;
			long mins = totalSecs / 60;
			log.info(String.format("Total time: %d:%d.%d", mins, secs, millis));
			log.info("Processed files: " + processedFiles);
		} catch (Exception e) {
            e.printStackTrace();
        }

		if(Configuration.rpGlobalStatisticsSw) {
			generateStatisticReport();
		}

		log.info("End.");
	}

	/**
	 * @throws Throwable
	 */
	public void generateGraph(final GenerateGraphContext generateGraphCtx) throws Throwable {
		Map<String, SerMethodIndexEntry> methodIndex = generateGraphCtx.getMethodIndex();
		String entryClassName = generateGraphCtx.getEntryClassName();
		String entryMethodDeclaration = generateGraphCtx.getEntryMethodDeclaration();
		String[] allEntryValues = generateGraphCtx.getEntryValueArray();

		if (classLoader.containsKey(entryClassName)) {
			boolean isEjb = false;
			boolean isServlet = false;
			boolean isCustomTag = false;
			boolean isMainClass = false;
			boolean isMainMethod = false;
			boolean isDirectMethodEntry = !StringUtils.isBlank(entryMethodDeclaration);
			JavaClass entryClass = classLoader.get(entryClassName);
			if (entryClass == null) {
				log.error(entryClassName + " found in class loader but it's NULL.");
				return;
			}

			List<String> registeredMethodList = new ArrayList<String>();
			if (!isDirectMethodEntry) {
				Method[] registeredMethods = entryClass.getMethods();
				for (Method m : registeredMethods) {
					registeredMethodList.add(m.getName() + m.getSignature());
					if (Constants.MAIN_METHOD_NAME.equals(m.getName()) && Constants.MAIN_METHOD_SIGNATURE.equals(m.getSignature())) {
						isMainClass = true;
					}
				}

				isEjb = ClassLoaderUtils.isEjbClass(entryClass, classLoader);
				String originalName = entryClassName;
				if (isEjb) {
					// Is an EJB
					if (entryClassName.endsWith("Bean")) {
						// Is EJBBean, we need to register the methods from EJB.
						entryClassName = StringUtils.removeEnd(entryClassName, "Bean");
						entryClass = classLoader.get(entryClassName);
						if (entryClass == null) {
							log.error("Class [" + originalName + "] is an EJB but it does not exist in the class loader.");
							return;
						}
						registeredMethodList.clear();
						// Process registered methods from interface only
						registeredMethods = entryClass.getMethods();
						for (Method m : registeredMethods) {
							registeredMethodList.add(m.getName() + m.getSignature());
						}
					}
					entryClass = classLoader.get(entryClassName + "Bean");
					if (entryClass == null) {
						log.error("Class does not exist in classloader: " + entryClassName + "Bean");
						return;
					}
					isMainClass = false;
				} else if (Configuration.filter.isAllowServlets() && ClassLoaderUtils.isServletClass(entryClass, classLoader)) {
					isServlet = true;
					isMainClass = false;
				} else if (Configuration.filter.isAllowCustomtags()
						&& (StringUtils.contains(entryClassName, ".customtags.") || entryClassName.endsWith("Tag"))) {
					isCustomTag = true;
					isMainClass = false;
				}

				if (isMainClass) {
					if (classLoaderFedDrivenRun && !entryClassName.startsWith("gov.illinois.ies.business.batch")
							&& !entryClassName.startsWith("gov.illinois.fw.batch")
							&& !entryClassName.startsWith("gov.illinois.framework.batch")) {
						// Not a class under batch package
						remainingClassesToProcess--;
						return;
					}
				}
			}

			String fullEntryVal = entryClassName + (StringUtils.isNotBlank(entryMethodDeclaration) ? "." + entryMethodDeclaration : "");
			log.info(remainingClassesToProcess-- + "> Generating graph for " + fullEntryVal);
			if (!classLoader.containsKey(entryClassName)) {
				log.error("Entry class [" + entryClassName + "] does not exist in the class loader.");
				return;
			}

			statisticReportList.put(entryClassName, new ArrayList<String>());
			Reporter reporter = new Reporter(reportPath, fullEntryVal);
			reporter.flowReportTxtSwitch(Configuration.rpFlowTxtSw);
			reporter.flowReportHtmlSwitch(Configuration.rpFlowHtmlSw);
			reporter.visitedMethodsReportCvsSwitch(Configuration.rpSummaryCsvSw);
			reporter.visitedMethodsReportHtmlSwitch(Configuration.rpSummaryHtmlSw);
			reporter.pmdFeedFileSwitch(Configuration.rpPmdTxtSw);
			reporter.setMethodIndex(methodIndex);
			reporter.setEntryValueArray(allEntryValues);

			List<MethodCallComposite> entryMethodCompositionList = new ArrayList<MethodCallComposite>();
			Method[] entryMethods = entryClass.getMethods();
			for (Method method : entryMethods) {
				String mname = method.getName();
				String msignature = method.getSignature();
				String methodDeclaration = entryClassName + "." + mname + msignature;
				
				boolean processThisMethod = false;
				if (isDirectMethodEntry) {
					boolean areSame = compareMethodDeclarations(entryMethodDeclaration, methodDeclaration);
					if (areSame) {
						processThisMethod = true;
						areSame = false;
					}
				} else {
					if (isMainClass) {
						// Process main() method only
						if (Constants.MAIN_METHOD_NAME.equals(mname) && Constants.MAIN_METHOD_SIGNATURE.equals(msignature)) {
							processThisMethod = true;
							isMainMethod = true;
						}
					} else {
						// Is Servlet or any other class
						if (registeredMethodList.contains(mname + msignature)) {
							processThisMethod = true;
						}
					}
				}
				visitedMethods = new LinkedHashMap<String, VisitedMethod>();
				visitedClasses = new LinkedHashSet<String>();
				processingMethodsQueue = new LinkedList<String>();
				if (processThisMethod) {
					log.trace("Processing method ... " + methodDeclaration);
					// log.debug(Configuration.remainingClassesToProcess + "> Generating graph for " + entryClassName + "." + mname);

					List<MethodCallComposite> instructions = null;
					try {
						instructions = this.getMethodCalls(entryClass, method, 0);
					} catch (Throwable t) {
						log.fatal(t.getMessage() + " | Error getting method calls of " + methodDeclaration);
						throw t;
					}

					SerClass serEntryClass = SerializationUtils.createSerializableClass(entryClass);
					SerMethod serEntryMethod = SerializationUtils.createSerializableMethod(method);

					MethodCallComposite entryMethodComposition = new MethodCallComposite(methodDeclaration, instructions);
					entryMethodComposition.setMethod(serEntryMethod);
					entryMethodComposition.setClazz(serEntryClass);
					entryMethodCompositionList.add(entryMethodComposition);

					if (isMainMethod) {
						// reporter.generateFlow(entryMethodComposition);
						log.info("Writing report for " + entryClassName + mname);
						reporter.generateVisitedMethodsReport(entryMethodCompositionList);

						if (Configuration.rpPmdTxtSw) {
							// Generate the PMD feed file
							reporter.generatePmdFeedFile(visitedClasses);
						}

						if (isMainMethod) {
							// Terminate the loop, the main() method have been reached
							break;
						}
					}
					allVisitedClasses.addAll(visitedClasses);
					visitedClasses.clear();
					visitedMethods.clear();
				}
			}
			if (!isMainMethod) {
				log.info("Writing report for " + fullEntryVal);
				reporter.generateVisitedMethodsReport(entryMethodCompositionList);

				if (Configuration.rpPmdTxtSw) {
					// Generate the PMD feed file
					reporter.generatePmdFeedFile(visitedClasses);
				}

				allVisitedClasses.addAll(visitedClasses);
				visitedClasses.clear();
				visitedMethods.clear();
			}
			statisticReportList.get(entryClassName).add("Max depth reached = " + maxDepthReached);
		} else {
			log.error(remainingClassesToProcess-- + "> Class does not exist in the class loader: " + entryClassName);
    	}
    }

	private boolean compareMethodDeclarations(String mDeclaration1, String mDeclaration2) {
		// mDeclaration1 should not have a bytecode parameters format
		String readableMDeclaration1 = mDeclaration1;
		String readableMDeclaration2 = ClassParsingUtils.makeDeclaringReadable(mDeclaration2);

		Object[] mAttr1 = getMethodAttributes(readableMDeclaration1);
		String methodName1 = (String) mAttr1[0];
		String[] mParameters1 = (String[]) mAttr1[1];

		Object[] mAttr2 = getMethodAttributes(readableMDeclaration2);
		String methodName2 = (String) mAttr2[0];
		String[] mParameters2 = (String[]) mAttr2[1];

		if (methodName1.equals(methodName2)) {
			if (mParameters1 == null) {
				// Parameters have not been defined. Will use as the same method.
				return true;
			}
			if (mParameters1.length == mParameters2.length) {
				for (int i = 0; i < mParameters1.length; i++) {
					String param1 = mParameters1[i];
					String param2 = mParameters2[i];
					if (!param1.equals(param2)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * @param methodDeclaration
	 */
	private Object[] getMethodAttributes(String methodDeclaration) {
		String fullQualifiedMethodName;
		Object[] methodAttrs = new Object[2];
		String[] methodParams = null;
		String paramsRegex = "\\(.*\\)";
		Pattern pattern = Pattern.compile(paramsRegex);
		Matcher matcher = pattern.matcher(methodDeclaration);
		if (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			fullQualifiedMethodName = methodDeclaration.substring(0, start);
			String methodParamString = methodDeclaration.substring(start + 1, end - 1);
			methodParams = StringUtils.split(methodParamString, ',');
		} else {
			fullQualifiedMethodName = methodDeclaration;
		}
		String methodName = null;
		int classMethodSeparationIx = fullQualifiedMethodName.lastIndexOf(".");
		if (classMethodSeparationIx >= 0) {
			methodName = fullQualifiedMethodName.substring(classMethodSeparationIx + 1);
		} else {
			methodName = fullQualifiedMethodName;
		}

		methodAttrs[0] = methodName;
		methodAttrs[1] = methodParams;
		return methodAttrs;
	}

	private List<MethodCallComposite> getMethodCalls(JavaClass entryClass, Method method, int currentLevel) throws Throwable {
		String classname = entryClass.getClassName();
		String methodname = method.getName();
		String methodSignature = method.getSignature();
		String methodDeclaration = ClassParsingUtils.formatMethodDeclaration(classname, methodname, methodSignature);
		processingMethodsQueue.push(methodDeclaration);

		// log.trace("Getting method calls of " + methodDeclaration);
		
		SerMethodIndexEntry indexEntry = null;
		boolean exists = MethodIndexer.getMethodIndex().containsKey(methodDeclaration);
		if (exists) {
			indexEntry = MethodIndexer.getMethodIndex().get(methodDeclaration);
		} else if (Configuration.serializationOn) {
			// If does not exist in the index and is in serialization mode lazy
			if (Constants.IndexationMode.LAZY.equals(Configuration.indexationMode)) {
				indexEntry = methodIndexer.deserializeObject(classname, methodname, methodDeclaration);
				if (indexEntry != null) {
					MethodIndexer.getMethodIndex().put(methodDeclaration, indexEntry);
					log.debug("Method has been deserialized: " + methodDeclaration);
				}
			}
		} else {
			log.debug("Indexing class on the fly: " + classname);
			Map.Entry<String, JavaClass> e = new AbstractMap.SimpleEntry(classname, entryClass);
			methodIndexer.indexClass(e, Configuration.filter, Configuration.maxDepth);
			indexEntry = MethodIndexer.getMethodIndex().get(methodDeclaration);
		}
		visitedClasses.add(classname);
		
		List<MethodCallComposite> methodCallList = new ArrayList<MethodCallComposite>();
		if (indexEntry != null) {
			MethodCallComposite compositeFromIndex = indexEntry.getMethodComposite();
			if (compositeFromIndex != null) {
				// Already analyzed, use the one from the index
				processingMethodsQueue.pop();
				return compositeFromIndex.getInstructionList();
			}

			Collection<SerInstruction> instructionList = indexEntry.getInstructionList();
			if (instructionList != null) {
				for (SerInstruction instructionEntry : instructionList) {
					SerClass innerClass = instructionEntry.getSerClass();
					String innerClassName = innerClass.getClassName();
					SerMethod innerMethod = instructionEntry.getSerMethod();
					String innerMethodName = innerMethod.getName();
					String innerMethodSignature = innerMethod.getSignature();
					String innerMethodDeclaration = ClassParsingUtils.formatMethodDeclaration(innerClassName, innerMethodName, innerMethodSignature);

					if (detectInfiniteCycles) {
						if (infiniteCycleMap.containsKey(innerMethodDeclaration)) {
							// This inner method has been detected as a infinite cycle
							continue;
						}
						boolean isInfinite = detectInfiniteCycle(innerMethodDeclaration);
						if (isInfinite) {
							continue;
						}
					}

					JavaClass innerJavaClass = classLoader.get(innerClassName);
					if (innerJavaClass == null) {
						// innerClass not found in the classloader
						continue;
					}

					boolean isAllowedClass = ClassLoaderUtils.isAllowedClass(innerJavaClass, Configuration.filter);
					if (!isAllowedClass) {
						continue;
					}

					// Will search for an implementation method containing instruction calls
					Map.Entry<JavaClass, Method> calledMethodPair = this.getCalledMethod(innerJavaClass, innerMethodName, innerMethodSignature);
					if (calledMethodPair != null) {
						Method calledMethod = calledMethodPair.getValue();
						JavaClass calledClass = calledMethodPair.getKey();
						String calledClassName = calledClass.getClassName();
						String calledMethodName = calledMethod.getName();
						String calledMethodSignature = calledMethod.getSignature();
						String callMethodDeclaration = ClassParsingUtils.formatMethodDeclaration(calledClassName, calledMethodName, calledMethodSignature);

						// For statistics purposes, calculate the number of calls per method
						if (Configuration.rpGlobalStatisticsSw) {
							calculateCallsPerMethod(callMethodDeclaration);
						}

						// Discover the method calls to compose a new method call composite
						MethodCallComposite callComposite = null;

						/*
						 * Checking if the method was already visited
						 * to use that one and improve the performance
						 */
						VisitedMethod visitedMethod = null;
						if (visitedCommonMethods.containsKey(callMethodDeclaration)) {
							visitedMethod = visitedCommonMethods.get(callMethodDeclaration);
							if (visitedMethod.getLevel() <= currentLevel) {
								callComposite = visitedMethod.getMethodComposite();
							}
						}
						if (callComposite == null && visitedMethods.containsKey(callMethodDeclaration)) {
							visitedMethod = visitedMethods.get(callMethodDeclaration);
							if (visitedMethod.getLevel() <= currentLevel) {
								callComposite = visitedMethod.getMethodComposite();
							}
						}

						List<MethodCallComposite> innerInstructionList = null;
						SerClass serCallClass = null;
						SerMethod serCallMethod = null;
						if (callComposite != null) {
							serCallClass = callComposite.getClazz();
							serCallMethod = callComposite.getMethod();
						} else {
							if (Configuration.maxDepth == 0 || currentLevel <= Configuration.maxDepth) {
								if (currentLevel > maxDepthReached) {
									maxDepthReached = currentLevel;
								}
								if (currentLevel > globalMaxDepthReached) {
									globalMaxDepthReached = currentLevel;
									if (globalMaxDepthReached > 200) {
										System.out.println(globalMaxDepthReached);
									}
								}
								try {
									innerInstructionList = this.getMethodCalls(calledClass, calledMethod, currentLevel + 1);
								} catch (Throwable t) {
									log.fatal(t.getMessage() + " | Error getting method calls of " + callMethodDeclaration);
									processingMethodsQueue.pop();
									throw t;
								}
							} else {
								innerInstructionList = new ArrayList<MethodCallComposite>();
							}

							serCallClass = SerializationUtils.createSerializableClass(calledClass);
							serCallMethod = SerializationUtils.createSerializableMethod(calledMethod);
							callComposite = new MethodCallComposite(callMethodDeclaration, innerInstructionList);
							callComposite.setClazz(serCallClass);
							callComposite.setMethod(serCallMethod);
							boolean alreadyRegisteredCommonMethod = false;
							for (String commonPkg : Constants.COMMON_METHOD_PACKAGES_CLASSES) {
								if (callMethodDeclaration.startsWith(commonPkg)) {
									visitedMethod = new VisitedMethod(callMethodDeclaration, currentLevel, callComposite);
									visitedCommonMethods.put(callMethodDeclaration, visitedMethod);
									log.trace("Registering common method: " + callMethodDeclaration);
									alreadyRegisteredCommonMethod = true;
									break;
								}
							}
							if (!alreadyRegisteredCommonMethod && !Configuration.rpFlowHtmlSw && !Configuration.rpFlowTxtSw) {
								// Want to register and get from this register only if Flow report generation is activated
								visitedMethod = new VisitedMethod(callMethodDeclaration, currentLevel, callComposite);
								visitedMethods.put(callMethodDeclaration, visitedMethod);
							}
						}
						methodCallList.add(callComposite);

						// Create a composite for a DAO method call if applies.
						boolean isCollection = false;
						if (Configuration.isDaoDiscoveryAllowed()) {
							isCollection = ClassLoaderUtils.isCollectionClass(calledClassName, calledClass.getSuperclassName());
							if (isCollection && allCollectionWithNoDao.contains(calledClassName)) {
								// Do not try to discover a non-existent DAO
								isCollection = false;
							}
						}
						if (Configuration.isDaoDiscoveryAllowed() && isCollection
								&& ArrayUtils.contains(MethodIndexer.ALLOWED_COLLECTION_METHODS, calledMethodName)) {
							String collectionPackage = ClassParsingUtils.getPackageNamePart(calledClassName);
							String collectionSimpleName = ClassParsingUtils.getClassNamePart(calledClassName);

							String daoClassName = null;
							String daoPackage = null;
							String daoSimpleName = null;
							if (Configuration.isABE()) {
								Field[] calledClassFields = calledClass.getFields();
								String packageConstantValue = null;
								for (Field field : calledClassFields) {
									String fieldName = field.getName();
									if (fieldName.equals(Constants.COLLECTION_FIELD_PACKAGE)) {
										ConstantValue constant = field.getConstantValue();
										String constantValue = constant.toString();
										packageConstantValue = constantValue.replaceAll("\\\"", "");
										break;
									}
								}
								if (packageConstantValue != null) {
									String daoPackageConstantValue = MethodCallGraph.substituteDB2PackageName(packageConstantValue);
									if (StringUtils.isNotBlank(daoPackageConstantValue)) {
										int ixOfLastPoint = daoPackageConstantValue.lastIndexOf(".");
										daoPackage = daoPackageConstantValue.substring(0, ixOfLastPoint);
										daoSimpleName = daoPackageConstantValue.substring(ixOfLastPoint + 1);
										daoSimpleName += Constants.ABE_DAO_POSTFIX;
									} else {
										// Use the same constant value for logging purposes only
										daoPackage = "";
										daoSimpleName = packageConstantValue;
									}
								}
							} else {
								daoPackage = StringUtils.replace(collectionPackage, ClassLoaderUtils.PKG_PART_BUSINESS_ENTITIES, ClassLoaderUtils.PKG_PART_DAOS);
								daoSimpleName = StringUtils.replace(collectionSimpleName, ClassLoaderUtils.COLLECTION, ClassLoaderUtils.DAO);
							}
							StringBuilder daoClassNameSb = new StringBuilder(daoPackage).append(Constants.STR_DOT).append(daoSimpleName);
							daoClassName = daoClassNameSb.toString();


							boolean daoExistInClassLoader = classLoader.containsKey(daoClassName);
							if (!daoExistInClassLoader) {
								// TODO check changes needed for ABE
								daoPackage = StringUtils.replace(collectionPackage, ClassLoaderUtils.PKG_PART_BATCH_ENTITIES, ClassLoaderUtils.PKG_PART_DAOS);
								daoClassNameSb = new StringBuilder(daoPackage).append(Constants.STR_DOT).append(daoSimpleName);
								daoClassName = daoClassNameSb.toString();
								daoExistInClassLoader = classLoader.containsKey(daoClassName);
							}

							if (daoExistInClassLoader) {
								String daoMethodDeclaration = ClassParsingUtils.formatMethodDeclaration(daoClassName, calledMethodName, calledMethodSignature);
								MethodCallComposite daoComposite = null;

								/*
								 * Checking if the method was already visited
								 * to use that one and improve the performance
								 */
								VisitedMethod visitedDaoMethod = null;
								if (visitedCommonMethods.containsKey(daoMethodDeclaration)) {
									visitedDaoMethod = visitedCommonMethods.get(daoMethodDeclaration);
									if (visitedDaoMethod.getLevel() <= currentLevel) {
										daoComposite = visitedDaoMethod.getMethodComposite();
									}
								}
								if (daoComposite != null && visitedMethods.containsKey(daoMethodDeclaration)) {
									visitedDaoMethod = visitedMethods.get(daoMethodDeclaration);
									if (visitedDaoMethod.getLevel() >= currentLevel) {
										daoComposite = visitedDaoMethod.getMethodComposite();
									}
								}

								if (daoComposite == null) {
									JavaClass daoClass = classLoader.get(daoClassName);
									SerClass serDaoClass = SerializationUtils.createSerializableClass(daoClass);
									SerMethod serDaoMethod = serCallMethod;

									daoComposite = new MethodCallComposite(daoMethodDeclaration, new ArrayList<MethodCallComposite>());
									daoComposite.setClazz(serDaoClass);
									daoComposite.setMethod(serDaoMethod);

									boolean alreadyRegisteredCommonMethod = false;
									// Check if it is a common method to add it to the map
									for (String commonPkg : Constants.COMMON_METHOD_PACKAGES_CLASSES) {
										if (daoMethodDeclaration.startsWith(commonPkg)) {
											visitedDaoMethod = new VisitedMethod(daoMethodDeclaration, currentLevel, daoComposite);
											visitedCommonMethods.put(daoMethodDeclaration, visitedDaoMethod);
											log.trace("Registering common method: " + daoMethodDeclaration);
											alreadyRegisteredCommonMethod = true;
											break;
										}
									}
									if (!alreadyRegisteredCommonMethod && !Configuration.rpFlowHtmlSw && !Configuration.rpFlowTxtSw) {
										// Want to register and get from this register only if Flow report generation is activated
										visitedDaoMethod = new VisitedMethod(daoMethodDeclaration, currentLevel, daoComposite);
										visitedMethods.put(daoMethodDeclaration, visitedDaoMethod);
									}
								}
								methodCallList.add(daoComposite);
							} else {
								log.error("Found a Collection class [" + calledClassName + "] but not a corresponding DAO class.");
								allCollectionWithNoDao.add(calledClassName);
							}
						}
					}
				}
			}
		}
		processingMethodsQueue.pop();
		return methodCallList;
	}
	
	private Map.Entry<JavaClass, Method> getCalledMethod(JavaClass clazz, String calledMethodName, String calledMethodSignature) {
		String className = clazz.getClassName();
		// Check the same class, not abstract methods
		Map.Entry<JavaClass, Method> discoveredMethod = MethodIndexer.discoverCalledMethod(clazz, calledMethodName, calledMethodSignature, false);
		if(discoveredMethod != null) {
			return discoveredMethod;
		}

		/*
		 * If the method was not found in the current class,
		 * check the classes that are currently being analyzed.
		 * Need to search in the insertion order
		 * since the first visited classes are the most top level implementations.
		 */
		Map.Entry<JavaClass, Method> discoveredMethodImplementation = searchMethodImplementation(visitedClasses, clazz, calledMethodName, calledMethodSignature);
		if (discoveredMethodImplementation != null) {
			return discoveredMethodImplementation;
		}
		List<String> processedClasses = new ArrayList<String>();
		for (String processingMethod : processingMethodsQueue) {
			String qClassname = ClassParsingUtils.getQualifiedClassnamePart(processingMethod);
			processedClasses.add(qClassname);
		}
		discoveredMethodImplementation = searchMethodImplementation(processedClasses, clazz, calledMethodName, calledMethodSignature);
		if (discoveredMethodImplementation != null) {
			return discoveredMethodImplementation;
		}

		String superClassName = clazz.getSuperclassName();

		// Before to check the super classes, check if it is a Collection
		if (Configuration.filter.isAllowCollections()) {
			boolean isCollection = ClassLoaderUtils.isCollectionClass(className, superClassName);
			if (isCollection) {
				Method absCollectionMethod = methodIndexer.getAbstractCollectionMethod(clazz, calledMethodName, calledMethodSignature);
				if (absCollectionMethod != null) {
					/*
					 * Return the current collection class
					 * but along with the "select" method from AbstractCollectionImpl
					 */
					Map.Entry<JavaClass, Method> newEntry = new AbstractMap.SimpleEntry(clazz, absCollectionMethod);
					return newEntry;
				}
			}
		}

		// Check the super-classes
		JavaClass superClazz = classLoader.get(superClassName);
		if (superClazz != null && !superClazz.isInterface()) {
			return getCalledMethod(superClazz, calledMethodName, calledMethodSignature);
		}

		// Check the same class, including abstract methods
		discoveredMethod = MethodIndexer.discoverCalledMethod(clazz, calledMethodName, calledMethodSignature, true);
		if (discoveredMethod != null) {
			return discoveredMethod;
		}

		return null;
	}

	private Map.Entry<JavaClass, Method> searchMethodImplementation(Collection<String> collectionToSearchIn, JavaClass clazz, String calledMethodName, String calledMethodSignature) {
		for (String processingClassName : collectionToSearchIn) {
			JavaClass processingClass = classLoader.get(processingClassName);
			boolean isTypeOf = false;
			if (clazz.isClass() && !clazz.equals(processingClass)) {
				isTypeOf = ClassLoaderUtils.instanceOf(classLoader, processingClass, clazz);
			}
			if (isTypeOf) {
				// Search the same method in the classes that are already being processed
				Map.Entry<JavaClass, Method> discoveredFromProcessingMethod =
						MethodIndexer.discoverCalledMethod(processingClass, calledMethodName, calledMethodSignature, false);
				if (discoveredFromProcessingMethod != null) {
					return discoveredFromProcessingMethod;
				}
			}
		}
		return null;
	}

	/**
	 * TODO ** Pilot implementation ** Fix
	 * 
	 * @param methodDeclarationToDetect
	 * @return
	 */
	private boolean detectInfiniteCycle(String methodDeclarationToDetect) {
		List<String> newInfiniteCycle = new ArrayList<String>();
		newInfiniteCycle.add(methodDeclarationToDetect);
		ListIterator<String> queueIterator = processingMethodsQueue.listIterator();
		while (queueIterator.hasNext()) {
			String previousMethod = queueIterator.next();
			newInfiniteCycle.add(0, previousMethod);
			if (previousMethod.equals(methodDeclarationToDetect)) {
				infiniteCycleMap.put(methodDeclarationToDetect, newInfiniteCycle);
				return true;
			}
		}
		newInfiniteCycle.clear();
		newInfiniteCycle = null;
		queueIterator = null;
		return false;
	}

	/**
	 * From ABE FwDAOFactory
	 * substitutes the entity package with db2 packge
	 */
	private static String substituteDB2PackageName(String aName) {
		int start_pos = aName.indexOf(Constants.ABE_BUSINESS_PACKAGE);
		if (start_pos >= 0) {
			StringBuffer daoName = new StringBuffer(aName.substring(0, start_pos));
			daoName.append(Constants.ABE_DB2_PACKAGE);
			daoName.append(aName.substring(start_pos + Constants.ABE_BUSINESS_PACKAGE.length()));
			return daoName.toString();
		} else {
			System.err.println("Cannot switch to DAO package for collection " + aName);
			log.error("Cannot switch to DAO package for collection " + aName);
			return null;
		}
	}

	/**
	 * @param callMethodDeclaration
	 */
	private static void calculateCallsPerMethod(String callMethodDeclaration) {
		if (methodsMultipleCalls.containsKey(callMethodDeclaration)) {
			int callCounter = methodsMultipleCalls.get(callMethodDeclaration) + 1;
			methodsMultipleCalls.put(callMethodDeclaration, callCounter);
		} else {
			methodsMultipleCalls.put(callMethodDeclaration, 1);
		}
	}

	/**
	 * 
	 */
	private static void generateStatisticReport() {
		log.debug("Writing statistic report.");
		globalStatisticReport = new ReportEngine("global-statistics.txt", reportPath, 0);

		globalStatisticReport.appendLine(classLoader.size() + " classes added in class loader.");
		globalStatisticReport.appendLine("------------------");
		globalStatisticReport.appendLine("  Cargos: " + ClassLoaderUtils.getCargoCounter());
		globalStatisticReport.appendLine("  PKs: " + ClassLoaderUtils.getPrimaryKeyCounter());
		globalStatisticReport.appendLine("  Collections: " + ClassLoaderUtils.getCollectionCounter());
		globalStatisticReport.appendLine("  DAOs: " + ClassLoaderUtils.getDaoCounter());
		globalStatisticReport.appendLine("  Interfaces " + ClassLoaderUtils.getInterfaceCounter());
		globalStatisticReport.appendLine("  Enums: " + ClassLoaderUtils.getEnumCounter());
		globalStatisticReport.appendLine("  VOs: " + ClassLoaderUtils.getVoCounter());
		globalStatisticReport.appendLine("  EJBS: " + ClassLoaderUtils.getEjbCounter());
		globalStatisticReport.appendLine("  Constants: " + ClassLoaderUtils.getConstantCounter());
		globalStatisticReport.appendLine("  Servlets: " + ClassLoaderUtils.getServletCounter());
		globalStatisticReport.appendLine("  CustomTags: " + ClassLoaderUtils.getCustomTagCounter());
		globalStatisticReport.appendLine("  Stubs: " + ClassLoaderUtils.getStubCounter());
		globalStatisticReport.appendLine("  Webservices: " + ClassLoaderUtils.getWsCounter());
		globalStatisticReport.appendLine("  Page elements: " + ClassLoaderUtils.getPageElementCounter());
		globalStatisticReport.appendLine("  Others: " + ClassLoaderUtils.getOtherCounter());
		long total = ClassLoaderUtils.getCargoCounter() + ClassLoaderUtils.getPrimaryKeyCounter() + ClassLoaderUtils.getCollectionCounter() +
				ClassLoaderUtils.getDaoCounter() + ClassLoaderUtils.getInterfaceCounter() + ClassLoaderUtils.getEnumCounter() +
				ClassLoaderUtils.getVoCounter() + ClassLoaderUtils.getEjbCounter() + ClassLoaderUtils.getConstantCounter() +
				ClassLoaderUtils.getServletCounter() + ClassLoaderUtils.getCustomTagCounter() + ClassLoaderUtils.getStubCounter() +
				ClassLoaderUtils.getWsCounter() + ClassLoaderUtils.getPageElementCounter() + ClassLoaderUtils.getOtherCounter();
		globalStatisticReport.appendLine("  Total: " + total);

		int maxCallPermited = 100;
		if (maxCallPermited < 1000) {
			globalStatisticReport.appendLine("\nMethods with more than [" + maxCallPermited + "] calls:");
			globalStatisticReport.appendLine("------------------");
			for (Entry<String, Integer> entry : methodsMultipleCalls.entrySet()) {
				if (entry.getValue() > maxCallPermited) {
					String methodDeclaration = entry.getKey();
					String qualifiedClassName = ClassParsingUtils.getQualifiedClassnamePart(methodDeclaration);
					String packageName = ClassParsingUtils.getPackageNamePart(qualifiedClassName);
					String className = ClassParsingUtils.getClassNamePart(qualifiedClassName);
					String methodDeclPart = ClassParsingUtils.getMethodDeclarationPart(methodDeclaration);
					globalStatisticReport.appendLine("  " + entry.getValue() + ",\"" + packageName + "\",\"" + className + "\",\"" + methodDeclPart + "\"");
				}
			}
		}
		
		globalStatisticReport.appendLine("\nMax depth counts:");
		globalStatisticReport.appendLine("------------------");
		globalStatisticReport.appendLine("Max depth reached globally = " + globalMaxDepthReached);

		for (Entry<String, List<String>> statisticByClass : statisticReportList.entrySet()) {
			String statContent = StringUtils.join(statisticByClass.getValue(), "\n  ");
			globalStatisticReport.appendLine("\nStatistics of " + statisticByClass.getKey());
			globalStatisticReport.appendContent("  " + statContent + "\n", 0);
		}

		globalStatisticReport.appendLine("\n" + infiniteCycleMap.size() + " Infinite cycles detected.");
		globalStatisticReport.appendLine("------------------");
		for (Entry<String, List<String>> cycle : infiniteCycleMap.entrySet()) {
			String statContent = StringUtils.join(cycle.getValue(), "\n  ");
			globalStatisticReport.appendLine("- Cycle in " + cycle.getKey());
			globalStatisticReport.appendContent("  " + statContent + "\n", 0);
		}

		try {
			globalStatisticReport.writeReport(false);
		} catch (IOException e) {
			log.error("Can't generate the the global statistics report.");
		}
	}

	private boolean confirmInfiniteCycle(List<String> methodList) {
		Map<String, SerMethodIndexEntry> methodIndex = methodIndexer.getMethodIndex();
		// if()
		// do {
		// } while();
		// for(String methodDeclaration : methodList) {
		// SerMethodIndexEntry methodEntry = methodIndex.get(methodDeclaration);
		// methodEntry.ge
		// }
		// String className = ClassParsingUtils.getQualifiedClassnamePart(fullMethodDeclaration);
		// String calledMethodDeclarationPart = ClassParsingUtils.getMethodDeclarationPart(fullMethodDeclaration);
		// getCalledMethod(clazz, calledMethodName, calledMethodSignature)
		return false;
	}
}

class GenerateGraphContext {
	private Map<String, SerMethodIndexEntry> methodIndex = null;
	private String entryClassName = null;
	private String entryMethodDeclaration = null;
	private String[] entryValueArray = null;

	public Map<String, SerMethodIndexEntry> getMethodIndex() {
		return methodIndex;
	}

	public void setMethodIndex(Map<String, SerMethodIndexEntry> methodIndex) {
		this.methodIndex = methodIndex;
	}

	public String getEntryClassName() {
		return entryClassName;
	}

	public void setEntryClassName(String entryClassName) {
		this.entryClassName = entryClassName;
	}

	public String getEntryMethodDeclaration() {
		return entryMethodDeclaration;
	}

	public void setEntryMethodDeclaration(String entryMethodDeclaration) {
		this.entryMethodDeclaration = entryMethodDeclaration;
	}

	public String[] getEntryValueArray() {
		return entryValueArray;
	}

	public void setEntryValueArray(String[] entryValueArray) {
		this.entryValueArray = entryValueArray;
	}
}
