package com.blogspot.jesfre.methodflow.common;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.visitor.Constants;

/**
 * Utilities to read and manipulate the source code
 * 
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 *         Oct 6, 2016
 */
public final class ClassLoaderUtils {
	public static final String Vo = "Vo";
	public static final String VO = "VO";
	public static final String CONSTANT = "Constant";
	public static final String CONSTANTS = "Constants";
	public static final String DAO = "DAO";
	public static final String CARGO = "Cargo";
	public static final String COLLECTION = "Collection";
	public static final String I_PRIMARY_KEY = "IPrimaryKey";
	public static final String PRIMARY_KEY = "PrimaryKey";
	public static final String EJB = "EJB";
	public static final String EJB_HOME = "EJBHome";
	public static final String ABSTRACT_VALUE_OBJECT = "AbstractValueObject";
	public static final String ABSTRACT_COLLECTION_IMPL = "AbstractCollectionImpl";
	public static final String ABSTRACT_COLLECTION_IMPL_QUALIFIED_NAME = "gov.illinois.fw.persistence.data.AbstractCollectionImpl";
	public static final String ABE_ABSTRACT_COLLECTION_QUALIFIED_NAME = "gov.illinois.framework.business.entities.AbstractCollection";
	public static final String PKG_FW_BATCH_ENTITIES = "gov.illinois.fw.batch.entities";
	public static final String PKG_FW_BUSINESS_ENTITIES = "gov.illinois.fw.business.entities";
	public static final String PKG_FRAMEWORK_BATCH_ENTITIES = "gov.illinois.framework.batch.entities";
	public static final String PKG_FRAMEWORK_BUSINESS_ENTITIES = "gov.illinois.framework.business.entities";
	public static final String PKG_DATA_ORACLE = "gov.illinois.ies.data.oracle";
	public static final String PKG_BUSINESS_ENTITIES = "gov.illinois.ies.business.entities";
	public static final String PKG_PERSISTENCE_DATA = "gov.illinois.fw.persistence.data";
	// With no DOT at the end since there are CCD classes with no sub-package under entities
	public static final String PKG_PART_BUSINESS_ENTITIES = ".business.entities";
	public static final String PKG_PART_BATCH_ENTITIES = ".batch.entities";
	public static final String PKG_PART_DAOS = ".data.oracle";

	private static Map<String, JavaClass> classLoader = new LinkedHashMap<String, JavaClass>();
	private static boolean allowAll = false;
	private static boolean doCount = true;
	private static int cargoCounter = 0;
	private static int primaryKeyCounter = 0;
	private static int collectionCounter = 0;
	private static int daoCounter = 0;
	private static int interfaceCounter = 0;
	private static int enumCounter = 0;
	private static int voCounter = 0;
	private static int ejbCounter = 0;
	private static int constantCounter = 0;
	private static int servletCounter = 0;
	private static int customTagCounter = 0;
	private static int stubCounter = 0;
	private static int wsCounter = 0;
	private static int pageElementCounter = 0;
	private static int otherCounter = 0;

	public enum ContainerChar {
		BRACKETS, PARENTHESIS, SQ_PARENTHESIS;
	};
	
	private ClassLoaderUtils() {
	}
	
	public static File searchFile(String qualifiedClassName, List<String> projectSrcFolders) {
		File file = null;
		String currentPathSearch = null;
		String packagePath = "/" + qualifiedClassName.trim().replaceAll("\\.", "/") + ".class";
		packagePath = packagePath.replaceAll("[\\t\\s+]*", "");

		for (String srcFolder : projectSrcFolders) {
			currentPathSearch = srcFolder + packagePath;
			file = new File(currentPathSearch);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public static String getQualifiedClassname(String classLocation, String srcFolderLocation) {
		classLocation = classLocation.replaceAll("\\\\", Constants.STR_DOT);
		for (String includedPackage : Configuration.includedPackages) {
			int beginIndex = classLocation.indexOf(Constants.STR_DOT + includedPackage);
			if (beginIndex >= 0) {
				// +1 due to added STR_DOT
				String classQName = classLocation.substring(beginIndex + 1);
				classQName = classQName.replaceFirst("\\.class", "");
				return classQName;
			}
		}
		return classLocation;
	}

	public static boolean isAllowedPackage(String qClassName, List<String> includedPackages, List<String> excludedPackages) {
		boolean isIncludedPackage = false;
		if (includedPackages != null && includedPackages.size() > 0) {
			for (String incPkg : includedPackages) {
				if (qClassName.startsWith(incPkg)) {
					isIncludedPackage = true;
					break;
				}
			}
		} else {
			isIncludedPackage = true;
		}

		boolean isExcludedPackage = false;
		if (excludedPackages != null) {
			for (String exPkg : excludedPackages) {
				if (qClassName.startsWith(exPkg)) {
					isExcludedPackage = true;
					break;
				}
			}
		}
		return isIncludedPackage && !isExcludedPackage;
	}

	public static boolean isAllowedClass(JavaClass javaClass, ClassFilter filter) {
		allowAll = Configuration.filter.isAllowEverything();
		if (allowAll) {
			// Nothing is excluded and no report of file-type-counters is needed
			return true;
		}
		String qClassName = javaClass.getClassName();
		String superclassName = javaClass.getSuperclassName();
		if (superclassName == null) {
			superclassName = "";
		}
		List<String> includedPackages = filter.getIncludedPackages();
		List<String> excludedPackages = filter.getExcludedPackages();
		List<String> excludedClasses = filter.getExcludedClasses();

		if (isStubClass(qClassName) || isWebserviceClass(qClassName)) {
			if (!allowAll) {
				return false;
			} else {
				return true;
			}
		}

		if (!ClassLoaderUtils.isAllowedPackage(qClassName, includedPackages, excludedPackages)) {
			if (!allowAll) {
				return false;
			} else {
				return true;
			}
		}

		if (javaClass.isEnum()) {
			// System.out.println("Detected enum " + qClassName);
			if (!allowAll && !filter.isAllowEnums()) {
				return false;
			} else {
				return true;
			}
		}
		if (javaClass.isInterface()) {
			// Check for EJBs
			if (qClassName.endsWith(EJB) || qClassName.endsWith(EJB_HOME)) {
				// System.out.println("Detected interface " + qClassName);
				if (!allowAll && !filter.isAllowEjbs()) {
					return false;
				} else {
					return true;
				}
			}
			if (!allowAll && !filter.isAllowInterfaces()) {
				return false;
			} else {
				return true;
			}
		}

		// TODO add filter for Servlets if necessary

		String superClassname = javaClass.getSuperclassName();
		String superPackageName = ClassParsingUtils.getPackageNamePart(superClassname);
		String currentClassPackage = ClassParsingUtils.getPackageNamePart(qClassName);

		if (!filter.isAllowCargos()) {
			boolean isCargo = isCargoClass(qClassName, currentClassPackage, superClassname, superPackageName);
			if (isCargo && !allowAll && !filter.isAllowCargos()) {
				return false;
			}

			boolean isPrimaryKey = false;
			isPrimaryKey = isPrimaryKeyClass(javaClass);
			if (isPrimaryKey && !allowAll && !filter.isAllowCargos()) {
				return false;
			}
		}
		if (!filter.isAllowCollections()) {
			boolean isCollection = isCollectionClass(qClassName, superClassname);
			if (isCollection && !allowAll && !filter.isAllowCollections()) {
				return false;
			} else {
				return true;
			}
		}
		if (!filter.isAllowDaos()) {
			boolean isDao = isDaoClass(qClassName, currentClassPackage, superClassname, superPackageName);
			if (isDao || !allowAll && !filter.isAllowDaos()) {
				return false;
			} else {
				return true;
			}
		}
		if (!filter.isAllowVos()) {
			boolean isVo = isVoClass(qClassName, currentClassPackage, superClassname);
			if (isVo && !allowAll && !filter.isAllowVos()) {
				return false;
			} else {
				return true;
			}
		}
		if (!filter.isAllowConstants()) {
			if (isConstantClass(qClassName)) {
				// It's a VO
				// System.out.println("Detected Constant " + qClassName);
				if (!allowAll && !filter.isAllowConstants()) {
					return false;
				} else {
					return true;
				}
			}
		}
		if (!filter.isAllowCustomtags()) {
			if (isCustomTagClass(qClassName)) {
				// It's a CustomTag
				// System.out.println("Detected CustomTag " + qClassName);
				if (!allowAll && !filter.isAllowCustomtags()) {
					return false;
				} else {
					return true;
				}
			}
		}

		if (qClassName.endsWith("EJBBean")) {
			// System.out.println("Detected EJB " + qClassName);
			if (!allowAll && !filter.isAllowEjbs()) {
				return false;
			} else {
				return true;
			}
		}
		return true;
	}

	/**
	 * Create a map of loaded classes from the given classpath locations.
	 * 
	 * @param classpathLocations
	 * @param filter
	 * @return
	 */
	public static Map<String, JavaClass> loadClasses(List<String> classpathLocations, ClassFilter filter) {
		List<String> includedPackages = filter.getIncludedPackages();
		List<String> excludedPackages = filter.getExcludedPackages();
		List<String> excludedClasses = filter.getExcludedClasses();
		doCount = Configuration.rpGlobalStatisticsSw;
		for (String srcFolderPath : classpathLocations) {
			File srcFolder = new File(srcFolderPath);
			if (srcFolder.exists()) {
				System.out.print("Loading classes from " + srcFolderPath + "... ");
				Collection<File> files = FileUtils.listFiles(srcFolder, new String[] { "class" }, true);
				System.out.println("Found " + files.size() + " files.");
				for (File classFile : files) {
					String qClassName = null;
					try {
						String classLocation = classFile.getAbsolutePath();
						qClassName = ClassLoaderUtils.getQualifiedClassname(classLocation, srcFolder.getAbsolutePath());
						boolean isAllowedClass = ClassLoaderUtils.isAllowedPackage(qClassName, includedPackages, excludedPackages);
						if (!isAllowedClass) {
							// System.err.println("Class not allowed " + qClassName);
							continue;
						}

						ClassParser cp = new ClassParser(classLocation);
						JavaClass javaClass = cp.parse();
						if (javaClass.getClassName().equals(qClassName)) {
							if (classLoader.containsKey(qClassName)) {
								System.err.println("Class already exist in the class loader: " + qClassName);
							} else {
								classLoader.put(qClassName, javaClass);
								// System.out.println("Loaded " + qClassName);

								if (doCount) {
									increaseCounter(javaClass);
								}
							}
						} else {
							System.err.println(qClassName + " and " + javaClass.getClassName() + " do not match.");
						}
					} catch (Throwable t) {
						System.err.println("Exception processing " + qClassName);
						t.printStackTrace();
					}
				}
			} else {
				// Current srcFolderPath does not exist
				// System.err.println(srcFolderPath + " does not exist.");
			}
		}
		return classLoader;
	}

	/**
	 * @param qClassName
	 * @param javaClass
	 */
	private static void increaseCounter(JavaClass javaClass) {
		String qClassName = javaClass.getClassName();
		if (javaClass.isEnum()) {
			enumCounter++;
			return;
		}
		if (isStubClass(qClassName)) {
			stubCounter++;
			return;
		}
		if (isWebserviceClass(qClassName)) {
			wsCounter++;
			return;
		}
		if (isConstantClass(qClassName)) {
			constantCounter++;
			return;
		}

		if (isCustomTagClass(qClassName)) {
			customTagCounter++;
			return;
		}
		if (javaClass.isInterface()) {
			boolean isEjb = isEjbClass(javaClass, classLoader);
			if (isEjb) {
				ejbCounter++;
			} else {
				interfaceCounter++;
			}
			return;
		}
		boolean isEjb = isEjbClass(javaClass, classLoader);
		if (isEjb) {
			ejbCounter++;
			return;
		}

		boolean isServlet = isServletClass(javaClass, classLoader);
		if (isServlet) {
			servletCounter++;
			return;
		}

		boolean isPrimaryKey = isPrimaryKeyClass(javaClass);
		if (isPrimaryKey) {
			primaryKeyCounter++;
			return;
		}

		boolean isPageElement = isPageElementClass(qClassName);
		if (isPageElement) {
			pageElementCounter++;
			return;
		}

		String superClassname = javaClass.getSuperclassName();
		String superPackageName = ClassParsingUtils.getPackageNamePart(superClassname);
		String currentClassPackage = ClassParsingUtils.getPackageNamePart(qClassName);

		boolean isCargo = isCargoClass(qClassName, currentClassPackage, superClassname, superPackageName);
		if (isCargo) {
			cargoCounter++;
			return;
		}

		boolean isCollection = isCollectionClass(qClassName, superClassname);
		if (isCollection) {
			collectionCounter++;
			return;
		}

		boolean isDao = isDaoClass(qClassName, currentClassPackage, superClassname, superPackageName);
		if (isDao) {
			daoCounter++;
			return;
		}

		boolean isVo = isVoClass(qClassName, currentClassPackage, superClassname);
		if (isVo) {
			voCounter++;
			return;
		}

		// Any other
		otherCounter++;
	}

	public static boolean isEjbClass(JavaClass entryClass, Map<String, JavaClass> classLoader) {
		String className = entryClass.getClassName();
		if (className.endsWith("EJB") || className.endsWith("EJBBean")
					|| className.endsWith("Session") || className.endsWith("SessionBean")) {
			return true;
		}

		String[] interfaces = entryClass.getInterfaceNames();
		if (interfaces.length == 0) {
			return false;
		}
		if (entryClass.isInterface()) {
			for (String interfaceName : interfaces) {
				if (interfaceName.startsWith("javax.ejb.")) {
					return true;
				}
			}
		}
		for (String interfaceName : interfaces) {
			JavaClass superInterface = classLoader.get(interfaceName);
			if (superInterface != null) {
				return isEjbClass(superInterface, classLoader);
			}
		}
		return false;
	}

	public static boolean isServletClass(JavaClass entryClass, Map<String, JavaClass> classLoader) {
		if (entryClass.getClassName().endsWith("Servlet")) {
			return true;
		}

		String superClassName = entryClass.getSuperclassName();
		if (StringUtils.isBlank(superClassName)) {
			return false;
		}
		if (superClassName.startsWith("javax.servlet.")) {
			return true;
		}
		JavaClass superClass = classLoader.get(superClassName);
		if (superClass != null) {
			return isServletClass(superClass, classLoader);
		}
		return false;
	}

	/**
	 * Checks if the given qualifiedClassName belongs to a Collection object
	 * 
	 * @param qualifiedClassName
	 * @param superClassname
	 * @return
	 */
	public static boolean isCollectionClass(String qualifiedClassName, String superClassname) {
		boolean isCollection = StringUtils.contains(qualifiedClassName, COLLECTION);
		if (isCollection) {
			// Conditions for ABE collections discovery
			if (Configuration.isABE()) {

				isCollection = qualifiedClassName.endsWith(COLLECTION) && superClassname.equals(ABE_ABSTRACT_COLLECTION_QUALIFIED_NAME);

			} else {

				// Conditions for IES collections discovery
				isCollection = superClassname.startsWith(PKG_BUSINESS_ENTITIES) && superClassname.endsWith(COLLECTION);
				if (!isCollection) {
					isCollection = superClassname.startsWith(PKG_PERSISTENCE_DATA) && superClassname.endsWith(ABSTRACT_COLLECTION_IMPL);
				}
				if (!isCollection) {
					isCollection = qualifiedClassName.endsWith(COLLECTION) && qualifiedClassName.startsWith(PKG_BUSINESS_ENTITIES);
				}

			}
		}
		return isCollection;
	}

	/**
	 * @param javaClass
	 * @return
	 */
	private static boolean isPrimaryKeyClass(JavaClass javaClass) {
		boolean isPrimaryKey = false;
		if (javaClass.getClassName().endsWith(PRIMARY_KEY)) {
			String[] interfaceNameArray = javaClass.getInterfaceNames();
			for (String interfaceName : interfaceNameArray) {
				if (interfaceName.endsWith(I_PRIMARY_KEY)) {
					// It's a primary key
					// System.out.println("Detected primary key " + qClassName);
					isPrimaryKey = true;
					break;
				}
			}
		}
		return isPrimaryKey;
	}

	/**
	 * @param qClassName
	 * @param currentClassPackage
	 * @param superClassname
	 * @param superPackageName
	 * @return
	 */
	private static boolean isCargoClass(String qClassName, String currentClassPackage, String superClassname, String superPackageName) {
		boolean isCargo = false;
		if (qClassName.endsWith(CARGO) && superClassname.endsWith(CARGO)
				&& (superPackageName.startsWith(PKG_PERSISTENCE_DATA)
						|| superPackageName.startsWith(PKG_BUSINESS_ENTITIES))) {
			// It's a cargo
			// System.out.println("Detected cargo " + qClassName);
			isCargo = true;
		}
		if (superClassname.endsWith(CARGO)
				&& (superPackageName.startsWith(PKG_PERSISTENCE_DATA) || superPackageName.startsWith(PKG_BUSINESS_ENTITIES))
				&& (currentClassPackage.startsWith(PKG_PERSISTENCE_DATA) || currentClassPackage.startsWith(PKG_BUSINESS_ENTITIES))) {
			// System.err.println("Skipping potential Cargo: " + qClassName);
			isCargo = true;
		}
		if (qClassName.endsWith(CARGO) && currentClassPackage.startsWith(PKG_BUSINESS_ENTITIES)) {
			// System.err.println("Skipping potential Cargo: " + qClassName);
			isCargo = true;
		}
		return isCargo;
	}

	/**
	 * @param qClassName
	 * @return
	 */
	private static boolean isPageElementClass(String qClassName) {
		return StringUtils.contains(qClassName, ".presentation.view.pageelements.");
	}

	/**
	 * @param qClassName
	 * @return
	 */
	private static boolean isCustomTagClass(String qClassName) {
		return StringUtils.contains(qClassName, ".presentation.customtags.");
	}

	/**
	 * @param qClassName
	 * @return
	 */
	private static boolean isConstantClass(String qClassName) {
		return qClassName.endsWith(CONSTANT) || qClassName.endsWith(CONSTANTS);
	}

	/**
	 * @param qClassName
	 * @return
	 */
	private static boolean isStubClass(String qClassName) {
		return qClassName.endsWith("_Stub");
	}

	/**
	 * @param qClassName
	 * @param currentClassPackage
	 * @param superClassname
	 * @return
	 */
	private static boolean isVoClass(String qClassName, String currentClassPackage, String superClassname) {
		boolean isVo = false;
		if ((qClassName.endsWith(VO) || qClassName.endsWith(Vo))
				&& (superClassname.endsWith(ABSTRACT_VALUE_OBJECT)
						|| currentClassPackage.startsWith(PKG_BUSINESS_ENTITIES)
						|| currentClassPackage.startsWith(PKG_DATA_ORACLE)
						|| currentClassPackage.startsWith(PKG_FW_BATCH_ENTITIES)
						|| currentClassPackage.startsWith(PKG_FW_BUSINESS_ENTITIES)
						|| currentClassPackage.startsWith(PKG_FRAMEWORK_BATCH_ENTITIES)
						|| currentClassPackage.startsWith(PKG_FRAMEWORK_BUSINESS_ENTITIES))) {
			// It's a VO
			// System.out.println("Detected VO " + qClassName);
			isVo = true;
		}
		if (qClassName.endsWith(VO) || qClassName.endsWith(Vo)) {
			// System.err.println("Skipping potential VO: " + qClassName);
			isVo = true;
		}
		return isVo;
	}

	/**
	 * @param qClassName
	 * @param currentClassPackage
	 * @param superClassname
	 * @param superPackageName
	 * @return
	 */
	private static boolean isDaoClass(String qClassName, String currentClassPackage, String superClassname, String superPackageName) {
		boolean isDao = false;
		if (qClassName.endsWith(DAO)
				&& (superPackageName.equals(currentClassPackage) || superPackageName.startsWith(PKG_PERSISTENCE_DATA))
				&& (superClassname.endsWith(DAO))) {
			// It's a DAO
			// System.out.println("Detected DAO " + qClassName);
			isDao = true;
		}
		if (qClassName.endsWith(DAO) && currentClassPackage.startsWith(PKG_DATA_ORACLE)) {
			// System.err.println("Skipping potential DAO: " + qClassName);
			isDao = true;
		}
		return isDao;
	}

	private static boolean isWebserviceClass(String qClassName) {
		if (qClassName.endsWith("_Ser")
				|| qClassName.endsWith("_Deser")
				|| qClassName.endsWith("_Helper")
				|| qClassName.endsWith("PortType")
				|| qClassName.endsWith("PortTypeProxy")
				// || (qClassName.endsWith("Service") || superclassName.startsWith("javax."))
				// || (qClassName.endsWith("ServiceInformation") || superclassName.startsWith("com.ibm.ws"))
				|| qClassName.endsWith("ServiceLocator")
				|| qClassName.endsWith("PortType")
				|| qClassName.endsWith("HttpStub")
				|| qClassName.endsWith("Soap")
				|| qClassName.endsWith("SoapProxy")
				|| qClassName.endsWith("SoapStub")) {
			return true;
		}
		return false;
	}

	public static boolean instanceOf(Map<String, JavaClass> classLoader, JavaClass childClass, JavaClass targetSuperClass) {
		if (childClass.getClassName().equals(targetSuperClass.getClassName())) {
			return true;
		} else {
			String newChildClassName = childClass.getSuperclassName();
			if (newChildClassName.equals(targetSuperClass.getClassName())) {
				return true;
			} else if (newChildClassName != null && classLoader.containsKey(newChildClassName)) {
				JavaClass newChildClass = classLoader.get(newChildClassName);
				return instanceOf(classLoader, newChildClass, targetSuperClass);
			}
		}
		return false;
	}

	public static boolean isDoCount() {
		return doCount;
	}

	public static void setDoCount(boolean doCount) {
		ClassLoaderUtils.doCount = doCount;
	}

	public static int getCargoCounter() {
		return cargoCounter;
	}

	public static int getPrimaryKeyCounter() {
		return primaryKeyCounter;
	}

	public static int getCollectionCounter() {
		return collectionCounter;
	}

	public static int getDaoCounter() {
		return daoCounter;
	}

	public static int getInterfaceCounter() {
		return interfaceCounter;
	}

	public static int getEnumCounter() {
		return enumCounter;
	}

	public static int getVoCounter() {
		return voCounter;
	}

	public static int getEjbCounter() {
		return ejbCounter;
	}

	public static int getConstantCounter() {
		return constantCounter;
	}

	public static int getServletCounter() {
		return servletCounter;
	}

	public static int getCustomTagCounter() {
		return customTagCounter;
	}

	public static int getStubCounter() {
		return stubCounter;
	}

	public static int getWsCounter() {
		return wsCounter;
	}

	public static int getOtherCounter() {
		return otherCounter;
	}

	public static int getPageElementCounter() {
		return pageElementCounter;
	}

	public static void main(String[] args) throws Exception {
		// For testing purposes or to get counts only
		File configFile = new File("C:\\ProjectILIES\\java-callgraph-master-working-dir\\config.properties");
		Configuration.loadConfigurations(configFile);
		Configuration.filter.setAllowCargos(true);
		Configuration.filter.setAllowCollections(true);
		Configuration.filter.setAllowDaos(true);
		Configuration.filter.setAllowEjbs(true);
		Configuration.filter.setAllowEnums(true);
		Configuration.filter.setAllowVos(true);
		Configuration.filter.setAllowInterfaces(true);
		Configuration.filter.setAllowConstants(true);
		Configuration.filter.setAllowServlets(true);
		Configuration.filter.setAllowCustomtags(true);
		Configuration.filter.setAllowOrphanLeaves(true);
		Configuration.filter.setAllowEverything(true);
		Configuration.rpGlobalStatisticsSw = true;
		
		Map<String, JavaClass> classLoader = loadClasses(Configuration.projectClassFolders, Configuration.filter);

		System.out.println();
		System.out.println(classLoader.size() + " classes added in class loader.");
		System.out.println("Cargos: " + cargoCounter);
		System.out.println("PKs: " + primaryKeyCounter);
		System.out.println("Collections: " + collectionCounter);
		System.out.println("DAOs: " + daoCounter);
		System.out.println("Interfaces " + interfaceCounter);
		System.out.println("Enums: " + enumCounter);
		System.out.println("VOs: " + voCounter);
		System.out.println("EJBS: " + ejbCounter);
		System.out.println("Constants: " + constantCounter);
		System.out.println("Servlets: " + servletCounter);
		System.out.println("CustomTags: " + customTagCounter);
		System.out.println("Stubs: " + stubCounter);
		System.out.println("WS: " + wsCounter);
		System.out.println("Page elements: " + pageElementCounter);
		System.out.println("Others: " + otherCounter);
		long total = cargoCounter + primaryKeyCounter + collectionCounter + daoCounter + interfaceCounter +
				enumCounter + voCounter + ejbCounter + constantCounter + servletCounter +
				customTagCounter + stubCounter + wsCounter + pageElementCounter + otherCounter;
		System.out.println("Total: " + total);

	}
}
