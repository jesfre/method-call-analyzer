/**
 * 
 */
package com.blogspot.jesfre.methodflow.visitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.blogspot.jesfre.methodflow.common.ClassFilter;
import com.blogspot.jesfre.methodflow.common.ClassLoaderUtils;
import com.blogspot.jesfre.methodflow.common.ClassParsingUtils;
import com.blogspot.jesfre.methodflow.common.Configuration;
import com.blogspot.jesfre.methodflow.reports.Reporter;
import com.blogspot.jesfre.methodflow.serialization.SerClass;
import com.blogspot.jesfre.methodflow.serialization.SerInstruction;
import com.blogspot.jesfre.methodflow.serialization.SerMethod;
import com.blogspot.jesfre.methodflow.serialization.SerMethodIndexEntry;
import com.blogspot.jesfre.methodflow.serialization.SerializationUtils;

/**
 * Discovers and indexes all methods in the classpath
 * 
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 *         Nov 30, 2016
 */
public class MethodIndexer {
	private static Log log = LogFactory.getLog(MethodIndexer.class);

	public static final String[] ALLOWED_COLLECTION_METHODS = {
			Constants.COLLECTION_SELECT_CALL,
			Constants.COLLECTION_INSERT_CALL,
			Constants.COLLECTION_UPDATE_CALL,
			Constants.COLLECTION_DELETE_CALL,
			Constants.COLLECTION_PERSIST_CALL,
			Constants.COLLECTION_EXECUTE_CALL,
			Constants.COLLECTION_SELECT_BATCH_CALL,
			Constants.COLLECTION_PERSIST_BATCH_CALL,
			Constants.COLLECTION_EXECUTE_BATCH_CALL };

	private static Map<String, JavaClass> classLoader = new LinkedHashMap<String, JavaClass>();
	
	private static Map<String, SerMethodIndexEntry> methodIndex = null;

	private static boolean isInitialIndexation = true;

	private static int remainingClassesToProcess = 0;

	private static MethodIndexer methodIndexer = new MethodIndexer();

	private MethodIndexer() {
	}

	public static MethodIndexer getInstance() {
		if (methodIndexer == null) {
			methodIndexer = new MethodIndexer();
		}
		return methodIndexer;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			Configuration.workingLocationPath = args[0];
		}
		if (StringUtils.isBlank(Configuration.workingLocationPath)) {
			log.error("Working directory is needed.");
			System.exit(-1);
		}

		long heapSize = Runtime.getRuntime().totalMemory();
		log.info("Heap size (MB): " + heapSize / 1024 / 1024);

		String configurationFileName = null;
		if (args.length > 1) {
			configurationFileName = args[1];
		} else {
			configurationFileName = "config.properties";
		}

		MethodIndexer methodIndexer = new MethodIndexer();
		try {
			File configFile = new File(Configuration.workingLocationPath + File.separator + configurationFileName);
			if (configFile.exists() == false) {
				log.error("No cofig file found.");
				System.exit(-1);
			}

			Configuration.loadConfigurations(configFile);

			if (Configuration.numberOfClassesToTest > 0) {
				log.info("--------------------------");
				log.info("Running in test mode!");
				log.info("--------------------------");
			}

			methodIndexer.loadClasses(Configuration.projectClassFolders, Configuration.filter);
			methodIndexer.indexAll(Configuration.filter, Configuration.maxDepth);

			log.info("\nWriting report to " + Configuration.workingLocationPath + "/" + Constants.REPORTS_FOLDER);
			Reporter reporter = new Reporter(Configuration.workingLocationPath + "/" + Constants.REPORTS_FOLDER, null);
			reporter.setMethodIndex(methodIndex);
			reporter.generateMethodRegistryReport(Configuration.outputColumns);

			printFileTypeCounters();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("End.");
	}

	/**
	 * 
	 */
	private static void printFileTypeCounters() {
		log.info(classLoader.size() + " classes added in class loader.");
		log.info("Cargos: " + ClassLoaderUtils.getCargoCounter());
		log.info("PKs: " + ClassLoaderUtils.getPrimaryKeyCounter());
		log.info("Collections: " + ClassLoaderUtils.getCollectionCounter());
		log.info("DAOs: " + ClassLoaderUtils.getDaoCounter());
		log.info("Interfaces " + ClassLoaderUtils.getInterfaceCounter());
		log.info("Enums: " + ClassLoaderUtils.getEnumCounter());
		log.info("VOs: " + ClassLoaderUtils.getVoCounter());
		log.info("EJBS: " + ClassLoaderUtils.getEjbCounter());
		log.info("Constants: " + ClassLoaderUtils.getConstantCounter());
		log.info("Servlets: " + ClassLoaderUtils.getServletCounter());
		log.info("CustomTags: " + ClassLoaderUtils.getCustomTagCounter());
		log.info("Stubs: " + ClassLoaderUtils.getStubCounter());
		log.info("WS: " + ClassLoaderUtils.getWsCounter());
		log.info("Page elements: " + ClassLoaderUtils.getPageElementCounter());
		log.info("Others: " + ClassLoaderUtils.getOtherCounter());
		long total = ClassLoaderUtils.getCargoCounter() + ClassLoaderUtils.getPrimaryKeyCounter() + ClassLoaderUtils.getCollectionCounter() +
				ClassLoaderUtils.getDaoCounter() + ClassLoaderUtils.getInterfaceCounter() + ClassLoaderUtils.getEnumCounter() +
				ClassLoaderUtils.getVoCounter() + ClassLoaderUtils.getEjbCounter() + ClassLoaderUtils.getConstantCounter() +
				ClassLoaderUtils.getServletCounter() + ClassLoaderUtils.getCustomTagCounter() + ClassLoaderUtils.getStubCounter() +
				ClassLoaderUtils.getWsCounter() + ClassLoaderUtils.getPageElementCounter() + ClassLoaderUtils.getOtherCounter();
		log.info("Total: " + total);
	}

	public Map<String, JavaClass> loadClasses(List<String> projectClassFolders, ClassFilter filter) {
		log.info("Loading classes...");
		classLoader = ClassLoaderUtils.loadClasses(projectClassFolders, filter);
		remainingClassesToProcess = classLoader.size();
		log.info("Loaded " + classLoader.size() + " classes.");
		return classLoader;
	}

	/**
	 * Indexes all methods in the classloader with given filtering
	 * 
	 * @param filter
	 * @param maxDepth 0: to index only declared methods; >=1: to include first level method calls
	 * @return
	 */
	public Map<String, SerMethodIndexEntry> indexAll(final ClassFilter filter, final int maxDepth) {
		if (Configuration.serializationOn) {
			log.info("Index will be populated from serialized objects if they exist.\n");
		}
		if (Constants.IndexationMode.LAZY.equals(Configuration.indexationMode)) {
			log.info("Indexation started in LAZY mode.\n");
			methodIndex = new LinkedHashMap<String, SerMethodIndexEntry>();
			return methodIndex;
		}
		log.info("Indexing...");
		long startingTime = System.currentTimeMillis();
		log.info("Starting indexation at " + new Date(startingTime));

		log.info("Indexation will exclude: ");
		if (!filter.isAllowCargos()) {
			log.info("Cargos. ");
		}
		if (!filter.isAllowCollections()) {
			log.info("Collections. ");
		}
		if (!filter.isAllowDaos()) {
			log.info("DAOs. ");
		}
		if (!filter.isAllowEjbs()) {
			log.info("EJBs. ");
		}
		if (!filter.isAllowEnums()) {
			log.info("Enums. ");
		}
		if (!filter.isAllowInterfaces()) {
			log.info("Interfaces. ");
		}
		if (!filter.isAllowVos()) {
			log.info("VOs. ");
		}
		if (!filter.isAllowServlets()) {
			log.info("Servlets. ");
		}
		if (!filter.isAllowCustomtags()) {
			log.info("Custom tags. ");
		}

		if (Configuration.serializationOn) {
			methodIndex = loadSerializedMethodIndexObjects();
		} else {
			methodIndex = new LinkedHashMap<String, SerMethodIndexEntry>();
		}

		int count = 0;
		ExecutorService pool = Executors.newFixedThreadPool(Configuration.indexationMaxThreads);
		for (final Entry<String, JavaClass> classEntry : classLoader.entrySet()) {
			Runnable r = new Runnable() {
				public void run() {
					indexClass(classEntry, filter, maxDepth);
				}
			};
			pool.execute(r);
			if (Configuration.numberOfClassesToTest > 0 && ++count > Configuration.numberOfClassesToTest) {
				break;
			}
		}
		pool.shutdown();
		try {
			pool.awaitTermination(Configuration.indexationTimeoutMins, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// for (final Entry<String, JavaClass> classEntry : classLoader.entrySet()) {
		// indexClass(classEntry, filter);
		// if (++count > 5000) {
		// break;
		// }
		// }
		long endingTime = System.currentTimeMillis();
		log.info("Ending indexation at " + new Date(endingTime));
		long totalMillis = endingTime - startingTime;
		long millis = totalMillis % 1000;
		long totalSecs = totalMillis / 1000;
		long secs = totalSecs % 60;
		long mins = totalSecs / 60;
		log.info(String.format("Total indexation time: %d:%d.%d", mins, secs, millis));
		log.info(methodIndex.size() + " methods have been indexed.\n");

		if (Configuration.serializationOn) {
			startingTime = System.currentTimeMillis();
			log.info("Starting serialization at " + new Date(startingTime));

			this.serializeMethodIndexObjects();

			endingTime = System.currentTimeMillis();
			log.info("Ending serialization at " + new Date(endingTime));
			totalMillis = endingTime - startingTime;
			millis = totalMillis % 1000;
			totalSecs = totalMillis / 1000;
			secs = totalSecs % 60;
			mins = totalSecs / 60;
			log.info(String.format("Total serialization time: %d:%d.%d", mins, secs, millis));
		}

		return methodIndex;
	}

	public Map<String, SerMethodIndexEntry> indexClass(Entry<String, JavaClass> classEntry, ClassFilter filter, int maxDepth) {
		Map<String, SerMethodIndexEntry> classMethodIndexEntries = new LinkedHashMap<String, SerMethodIndexEntry>();
		JavaClass javaClass = classEntry.getValue();
		boolean isAllowedClass = ClassLoaderUtils.isAllowedClass(javaClass, filter);
		if (!isAllowedClass) {
			// Skip classes not to be indexed
			remainingClassesToProcess--;
			return classMethodIndexEntries;
		}
		String qclassname = classEntry.getKey();
		log.trace(remainingClassesToProcess + " Indexing " + qclassname + "... ");

		Method[] entryMethods = javaClass.getMethods();
		for (Method method : entryMethods) {
			String methodDeclaration = ClassParsingUtils.formatMethodDeclaration(
					qclassname, method.getName(), method.getSignature());
			
			try {
				if (!isInitialIndexation && Configuration.serializationOn && methodIndex != null) {
					if (methodIndex.containsKey(methodDeclaration)) {
						// Current method is already deserialized
						continue;
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage() + ". Error while deserializing " + methodDeclaration + ". Indexing methods from class file.");
			}
			try {
				// log.trace("Getting method calls of " + methodDeclaration);
				Set<SerInstruction> methodCalls = null;
				if (maxDepth > 0) {
					methodCalls = this.getMethodCalls(classEntry.getValue(), method, filter);
				} else {
					methodCalls = new LinkedHashSet<SerInstruction>();
				}

				String module = ClassParsingUtils.getModuleName(Configuration.projectClassFolders, javaClass.getFileName());
				SerClass serClass = SerializationUtils.createSerializableClass(javaClass);
				serClass.setModule(module);
				SerMethod serMethod = SerializationUtils.createSerializableMethod(method);
				SerMethodIndexEntry indexEntry = new SerMethodIndexEntry(methodDeclaration, serMethod, serClass, methodCalls);
				methodIndex.put(methodDeclaration, indexEntry);
				classMethodIndexEntries.put(methodDeclaration, indexEntry);
			} catch (Exception e) {
				log.error("Error while getting method calls for " + methodDeclaration);
				e.printStackTrace();
			}
		}
		remainingClassesToProcess--;
		if (remainingClassesToProcess > -1 && remainingClassesToProcess % 1000 == 0) {
			log.info(remainingClassesToProcess + " Remaining classes to index.");
		}
		return classMethodIndexEntries;
	}

	private Set<SerInstruction> getMethodCalls(JavaClass entryClass, Method method, ClassFilter filter) throws Exception {
		ClassVisitor cvisitor = new ClassVisitor(entryClass);
		MethodVisitor mvisitor = cvisitor.visitMethod(method, Configuration.includedPackages, new ArrayList<String>(), new ArrayList<String>());
		Map<String, InvokeInstruction> invInstructions = mvisitor.getCallMap();

		ConstantPoolGen cp = cvisitor.getConstants();
		Set<SerInstruction> methodCallList = new LinkedHashSet<SerInstruction>();
		for (Map.Entry<String, InvokeInstruction> e : invInstructions.entrySet()) {
			InvokeInstruction inst = e.getValue();
			String qClassName = inst.getReferenceType(cp).toString();
			String calledMethodName = inst.getMethodName(cp);
			String signature = inst.getSignature(cp);
			String calledMethodDeclaration = ClassParsingUtils.formatMethodDeclaration(qClassName, calledMethodName, signature);

			if (classLoader.containsKey(qClassName)) {
				JavaClass clazz = classLoader.get(qClassName);
				boolean isAllowedClass = ClassLoaderUtils.isAllowedClass(clazz, filter);
				if (!isAllowedClass) {
					// Skip this class and all of its methods
					continue;
				}

				Map.Entry<JavaClass, Method> calledMethod = this.getCalledMethod(clazz, calledMethodName, signature);
				if (calledMethod != null) {
					SerClass serCalledClass = SerializationUtils.createSerializableClass(calledMethod.getKey());
					SerMethod serCalledMethod = SerializationUtils.createSerializableMethod(calledMethod.getValue());
					SerInstruction serInstruction = new SerInstruction(serCalledMethod, serCalledClass);
					methodCallList.add(serInstruction);
				} else {
					log.debug("!!! Cannot find method " + calledMethodDeclaration);
				}
			} else {
				log.debug("Class " + qClassName + " not found in classloader.");
			}
		}
		return methodCallList;
	}

	private Map.Entry<JavaClass, Method> getCalledMethod(JavaClass clazz, String calledMethodName, String calledMethodSignature) {
		String className = clazz.getClassName();
		// Check the same class
		Map.Entry<JavaClass, Method> discoveredMethod = MethodIndexer.discoverCalledMethod(clazz, calledMethodName, calledMethodSignature, true);
		if (discoveredMethod != null) {
			return discoveredMethod;
		}

		String superClassName = clazz.getSuperclassName();

		// Before to check the super classes, check if it is a Collection
		if (Configuration.filter.isAllowCollections()) {
			boolean isCollection = ClassLoaderUtils.isCollectionClass(className, superClassName);
			if (isCollection) {
				Method absCollectionMethod = getAbstractCollectionMethod(clazz, calledMethodName, calledMethodSignature);
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
		if (StringUtils.isNotBlank(superClassName)) {
			JavaClass superClazz = classLoader.get(superClassName);
			if (superClazz != null && !superClazz.isInterface()) {
				Map.Entry<JavaClass, Method> curCalledMethod = getCalledMethod(superClazz, calledMethodName, calledMethodSignature);
				return curCalledMethod;
			}
		}
		// Check interfaces
		String[] interfaceNames = clazz.getInterfaceNames();
		// if (StringUtils.isNotBlank(superClassName)) {
		// JavaClass superClazz = classLoader.get(superClassName);
		// if (superClazz != null && !superClazz.isInterface()) {
		// Map.Entry<JavaClass, Method> curCalledMethod = getCalledMethod(superClazz, calledMethodName, signature);
		// return curCalledMethod;
		// }
		// }
		return null;
	}

	public Method getAbstractCollectionMethod(JavaClass clazz, String calledMethodName, String calledMethodSignature) {
		Method absCollectionSelect = null;
		if (ArrayUtils.contains(ALLOWED_COLLECTION_METHODS, calledMethodName)) {
			// Take the method from AbstractCollection to use it as the method from this collection
			JavaClass abstractCollection = classLoader.get(ClassLoaderUtils.ABSTRACT_COLLECTION_IMPL_QUALIFIED_NAME);
			if (abstractCollection == null && Configuration.isABE()) {
				abstractCollection = classLoader.get(ClassLoaderUtils.ABE_ABSTRACT_COLLECTION_QUALIFIED_NAME);
			}
			Method[] absCollMethodList = abstractCollection.getMethods();
			for (Method absMethod : absCollMethodList) {
				if (absMethod.getName().equals(calledMethodName) && absMethod.getSignature().equals(calledMethodSignature)) {
					absCollectionSelect = absMethod;
					break;
				}
			}
		}
		return absCollectionSelect;
	}

	public static Map.Entry<JavaClass, Method> discoverCalledMethod(JavaClass processingClass, String discoveringMethodName, String discoveringMethodSignature, boolean checkAbstractMethod) {
		// By default two method objects are said to be equal when their names and signatures are equal.
		Method[] methodArray = processingClass.getMethods();
		boolean isInterface = processingClass.isInterface();
		for (Method refMethod : methodArray) {
			boolean isAllowedMethod = isInterface || (!isInterface && !refMethod.isAbstract());
			if (checkAbstractMethod || isAllowedMethod) {
				String mname = refMethod.getName();
				String msignature = refMethod.getSignature();
				if (mname.equals(discoveringMethodName) && msignature.equals(discoveringMethodSignature)) {
					Map.Entry<JavaClass, Method> e = new AbstractMap.SimpleEntry(processingClass, refMethod);
					return e;
				}
			}
		}
		return null;
	}

	/**
	 * @param indexEntry
	 * @param qclassname
	 * @param method
	 * @param methodDeclarationStr
	 */
	private void serializeMethodIndexObjects() {
		log.debug("Serializing method index objects...");
		try {
			StringBuilder serDirPath = new StringBuilder(Configuration.workingLocationPath).append(Constants.SERIALIZED_OBJECTS_FOLDER);
			File serDir = new File(serDirPath.toString());
			if (!serDir.exists()) {
				serDir.mkdirs();
			}
			
			int remainingObjects = methodIndex.size();
			int serializedCount = 0;
			for (SerMethodIndexEntry ixEntry : methodIndex.values()) {
				if (ixEntry == null) {
					continue;
				}
				String methodDeclaration = ixEntry.getMethodDeclaration();
				String serFileName = null;
				String className = null;
				String methodName = null;
				FileOutputStream fileOut = null;
				ObjectOutputStream out = null;
				GZIPOutputStream zos = null;
				try {
					className = ixEntry.getOwnerClass().getClassName();
					methodName = ixEntry.getMethod().getName();
					methodName = methodName.replaceFirst("<", "-").replaceFirst(">", "-");
					serFileName = String.format(Constants.SERIALIZED_FILE_NAME_FORMAT,
							new Object[] { className, methodName, methodDeclaration.hashCode() });
					StringBuilder serPath = new StringBuilder(serDirPath).append(serFileName);

					File serFile = new File(serPath.toString());
					if (!serFile.exists()) {
						serFile.createNewFile();

						fileOut = new FileOutputStream(serFile);
						zos = new GZIPOutputStream(fileOut);
						out = new ObjectOutputStream(zos);
						out.writeObject(ixEntry);
						serializedCount++;
					}
				} catch (Exception e) {
					log.error(e.getMessage() + ". Cannot serialize " + className + "." + methodName + " to file " + serFileName);
					e.printStackTrace();
				} finally {
					if (out != null) {
						out.reset();
						out.close();
					}
					if (zos != null) {
						zos.close();
					}
					if (fileOut != null) {
						fileOut.flush();
						fileOut.close();
					}
				}
				remainingObjects--;
				if (remainingObjects % 1000 == 0) {
					log.debug("Remaining objects to be serialized " + remainingObjects);
				}
			}
			if(serializedCount > 0) {
				log.debug(serializedCount + " method index objects have been serialized to " + serDirPath);
			} else {
				log.info("No objects have been serialized.");
			}
		} catch (Exception e) {
			log.error(e.getMessage() + ". Cannot serialize method index.");
		}
	}

	private Map<String, SerMethodIndexEntry> loadSerializedMethodIndexObjects() {
		final Map<String, SerMethodIndexEntry> serializedMethodIndex = new LinkedHashMap<String, SerMethodIndexEntry>();
		StringBuilder serDirPath = new StringBuilder(Configuration.workingLocationPath).append(Constants.SERIALIZED_OBJECTS_FOLDER);
		File serDir = new File(serDirPath.toString());
		if (!serDir.exists()) {
			log.error("Serialized objects folder [" + serDirPath + "] does not exist. Indexing methods from class files.");
			return serializedMethodIndex;
		}
		log.debug("Loading method index objects from " + serDirPath);

		Collection<File> serFileList = FileUtils.listFiles(serDir, new String[] { "ser" }, false);
		isInitialIndexation = serFileList.size() == 0;

		// ExecutorService pool = Executors.newFixedThreadPool(Configuration.maxThreads);
		int fileCount = serFileList.size();
		for (final File serFile : serFileList) {
			// Runnable r = new Runnable() {
			// public void run() {
			// log.debug(fileCount + " Deserializing " + serFile.getName());
			SerMethodIndexEntry serIxEntry = this.deserializeObject(serFile);
			if (serIxEntry != null) {
				serializedMethodIndex.put(serIxEntry.getMethodDeclaration(), serIxEntry);
			}
			// }
			// };
			// pool.execute(r);

			fileCount--;
			if (fileCount % 1000 == 0) {
				log.debug("Remaining objects to be deserialized " + fileCount);
			}
		}
		// pool.shutdown();
		// try {
		// pool.awaitTermination(Configuration.timeoutMins, TimeUnit.MINUTES);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		return serializedMethodIndex;
	}

	public SerMethodIndexEntry deserializeObject(String className, String methodName, String methodDeclaration) {
		String newMethodName = methodName.replaceFirst("<", "-").replaceFirst(">", "-");
		StringBuilder serDirPath = new StringBuilder()
				.append(Configuration.workingLocationPath)
				.append(Constants.SERIALIZED_OBJECTS_FOLDER);
		StringBuilder serFilePath = new StringBuilder()
				.append(className).append(".").append(newMethodName)
				.append("_").append(methodDeclaration.hashCode()).append(".ser");
		final File serFile = new File(serDirPath.toString() + serFilePath.toString());

		SerMethodIndexEntry methodIxEntry = null;
		if (serFile.exists()) {
			methodIxEntry = this.deserializeObject(serFile);
		} else {
			log.warn("Serialized object not found: " + serFilePath);
		}
		return methodIxEntry;
	}

	/**
	 * @param serFile
	 * @return
	 */
	private SerMethodIndexEntry deserializeObject(final File serFile) {
		FileInputStream fis = null;
		GZIPInputStream zis = null;
		ObjectInputStream ois = null;
		SerMethodIndexEntry serIxEntry = null;
		try {
			fis = new FileInputStream(serFile);
			zis = new GZIPInputStream(fis);
			ois = new ObjectInputStream(zis);
			serIxEntry = (SerMethodIndexEntry) ois.readObject();
		} catch (Exception e) {
			log.error(e.getMessage() + ". Cannot deserialize [" + serFile.getName() + "]. Indexation will be from class file.");
		} finally {
			try {
				if (ois != null) {
					ois.close();
					ois = null;
				}
				if (zis != null) {
					zis.close();
					zis = null;
				}
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return serIxEntry;
	}

	/**
	 * @param toFile
	 * @return
	 */
	public static String getAsJavaFile(String toFile) {
		if (toFile.indexOf(Constants.BIN_FOLDER) > 0) {
			toFile = StringUtils.replace(toFile, Constants.BIN_FOLDER, Constants.SRC_FOLDER);
		}
		toFile = StringUtils.replace(toFile, Constants.DOT_CLASS, Constants.DOT_JAVA);
		return toFile;
	}

	public static List<String> getModifiers(SerMethod method) {
		List<String> modifierList = new ArrayList<String>();
		if (method.isPublic()) {
			modifierList.add(Constants.MODIF_PUBLIC);
		}
		if (method.isPrivate()) {
			modifierList.add(Constants.MODIF_PRIVATE);
		}
		if (method.isProtected()) {
			modifierList.add(Constants.MODIF_PROTECTED);
		}
		if (method.isAbstract()) {
			modifierList.add(Constants.MODIF_ABSTRACT);
		}
		if (method.isFinal()) {
			modifierList.add(Constants.MODIF_FINAL);
		}
		if (method.isNative()) {
			modifierList.add(Constants.MODIF_NATIVE);
		}
		if (method.isStrictfp()) {
			modifierList.add(Constants.MODIF_STRICTFP);
		}
		if (method.isStatic()) {
			modifierList.add(Constants.MODIF_STATIC);
		}
		if (method.isSynchronized()) {
			modifierList.add(Constants.MODIF_SYNCHRONIZED);
		}
		return modifierList;
	}

	public static Map<String, JavaClass> getClassLoader() {
		return classLoader;
	}

	public static Map<String, SerMethodIndexEntry> getMethodIndex() {
		return methodIndex;
	}

}
