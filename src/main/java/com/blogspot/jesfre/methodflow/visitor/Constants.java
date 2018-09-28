/**
 * 
 */
package com.blogspot.jesfre.methodflow.visitor;

/**
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 * Nov 17, 2016
 */
public final class Constants {

	private Constants() {
	}

	public enum IndexationMode {
		LAZY, EAGER
	};
	
	public enum ReportColumn {
		FROM_MODULE, FROM_METHOD_DECLARATION, FROM_METHOD_TYPE, FROM_METHOD_NAME, FROM_CLASS, FROM_CLASS_TYPE, FROM_FILE,
		TO_MODULE, TO_FILE, TO_METHOD_NAME, TO_METHOD_DECLARATION, TO_CLASS, TO_CLASS_TYPE
	};
	
	public static final String[] COMMON_METHOD_PACKAGES_CLASSES = {
			"gov.illinois.fw.business.exceptions",
			"gov.illinois.fw.batch.Controller",
			"gov.illinois.fw.batch.StreamHandler",
			"gov.illinois.framework.exceptions",
			"gov.illinois.framework.factories",
			"gov.illinois.framework.management.logging",
			"gov.illinois.framework.management.util",
			"gov.illinois.fw.batch.HelperClasses.DataFormatter"
		};

	public static final String PMD_FEED_FILE_TXT = "%s-pmd_feed.txt";
	public static final String FLOW_REPORT_HTML = "%s-method_flow.html";
	public static final String FLOW_REPORT_TXT = "%s-method_flow.txt";
	public static final String VISITED_METHODS_REPORT_HTML = "%s-visited_methods.html";
	public static final String VISITED_METHODS_REPORT_CSV = "%s-visited_methods.csv";
	public static final String ENTRY_VAL_PREFIX = "ENTRY_VAL_";

	public static final String REPORTS_FOLDER = "reports";
	public static final String SERIALIZED_OBJECTS_FOLDER = "/tmp/serialization/";
	public static final String SERIALIZED_FILE_NAME_FORMAT = "%s.%s_%s.ser";
	public static final String SER_METHOD_INDEX_FILE_NAME = "methodIndex.ser";

	public static final String METHOD_TYPE_CONSTRUCTOR = "Constructor";
	public static final String METHOD_TYPE_METHOD = "Method";

	public static final String MAIN_METHOD_NAME = "main";

	public static final String MAIN_METHOD_SIGNATURE = "([Ljava/lang/String;)V";

	public static final String CONSTRUCTOR_NAME = "<init>";

	public static final String STR_SLASH = "/";
	public static final String STR_BACK_SLASH = "\\";
	public static final String STR_QUEST = "?";
	public static final String STR_QUOTES = "\"";
	public static final String STR_COMMA = ",";
	public static final String STR_SEMICOLON = ";";
	public static final String STR_SPACE = " ";
	public static final String STR_DOT = ".";
	public static final String STR_HYPHEN = "-";
	public static final String DOT_JAVA = ".java";
	public static final String DOT_CLASS = ".class";
	public static final String SRC_FOLDER = "\\src\\";
	public static final String BIN_FOLDER = "\\bin\\";

	public static final String METHOD_DECLARATION_FORMAT_FULL = "%s.%s%s";
	public static final String METHOD_DECLARATION_FORMAT_SHORT = "%s%s";

	public static final String PROJECT_IES = "IES";
	public static final String PROJECT_ABE = "ABE";

	public static final String[] GROUPING_MODULES = { "FrameworkEJB", "CCD" };
	public static final String DEFAULT_GROUPING_MODULE = "Main";

	public static final String COLLECTION_SELECT_CALL = "select";
	public static final String COLLECTION_UPDATE_CALL = "update";
	public static final String COLLECTION_INSERT_CALL = "insert";
	public static final String COLLECTION_DELETE_CALL = "delete";
	public static final String COLLECTION_PERSIST_CALL = "persist";
	public static final String COLLECTION_EXECUTE_CALL = "execute";
	public static final String COLLECTION_SELECT_BATCH_CALL = "selectBatch";
	public static final String COLLECTION_PERSIST_BATCH_CALL = "persistBatch";
	public static final String COLLECTION_EXECUTE_BATCH_CALL = "executeBatch";

	public static final String MODIF_SYNCHRONIZED = "synchronized";
	public static final String MODIF_STATIC = "static";
	public static final String MODIF_STRICTFP = "strictfp";
	public static final String MODIF_NATIVE = "native";
	public static final String MODIF_FINAL = "final";
	public static final String MODIF_ABSTRACT = "abstract";
	public static final String MODIF_PROTECTED = "protected";
	public static final String MODIF_PRIVATE = "private";
	public static final String MODIF_PUBLIC = "public";

	public static final String COLLECTION_FIELD_PACKAGE = "PACKAGE";
	public static final String ABE_CARGO_POSTFIX = "_Cargo";
	public static final String ABE_PK_POSTFIX = "_PrimaryKey";
	public static final String ABE_COLLECTION_POSTFIX = "_Collection";
	public static final String ABE_DAO_POSTFIX = "_DAO";
	public static final String ABE_BUSINESS_PACKAGE = "business.entities";
	public static final String ABE_DB2_PACKAGE = "data.db2";
}
