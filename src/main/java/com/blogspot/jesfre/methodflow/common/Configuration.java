
/**
 * 
 */
package com.blogspot.jesfre.methodflow.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.visitor.Constants;
import com.blogspot.jesfre.methodflow.visitor.Constants.ReportColumn;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 4, 2016
 */
public final class Configuration {
	private Configuration() {
	}

	public static List<String> projectClassFolders = new ArrayList<String>();

	public static String workingLocationPath = null;

	public static List<String> includedPackages = new ArrayList<String>();
	
	public static List<String> excludedPackages = new ArrayList<String>();
	
	public static List<String> excludedClasses = new ArrayList<String>();

	public static ClassFilter filter = new ClassFilter();

	public static String projectName = null;

	public static String[] outputColumns = null;

	public static int maxDepth = 0;

	public static int numberOfClassesToTest = 0;

	public static int indexationTimeoutMins = 60;

	public static int indexationMaxThreads = 50;

	public static int graphTimeoutMins = 30;

	public static int graphMaxThreads = 1;

	public static int maxRecordsPerFile = 0;

	public static boolean serializationOn = false;

	private static boolean allowDaoDiscovery = false;

	public static boolean rpFlowTxtSw = true;
	public static boolean rpFlowHtmlSw = true;
	public static boolean rpSummaryCsvSw = true;
	public static boolean rpSummaryHtmlSw = true;
	public static boolean rpPmdTxtSw = true;
	public static boolean rpSingleSummaryCsvSw = true;
	public static boolean rpSinglePmdTxtSw = true;
	public static boolean rpGlobalStatisticsSw = true;
	public static boolean rpIndividualStatisticsSw = true;

	public static Constants.IndexationMode indexationMode = null;

	public static Properties properties = new Properties();

	public static void loadConfigurations(File configFile) throws Exception {
		InputStream inStream = new FileInputStream(configFile);
		properties.load(inStream);

		String projName = properties.getProperty("project.name");
		if (projName != null) {
			Configuration.projectName = projName.toUpperCase();
		} else {
			Configuration.projectName = Constants.PROJECT_IES;
		}
		Configuration.numberOfClassesToTest = Integer.valueOf(properties.getProperty("test.numberOfClasses"));
		Configuration.maxDepth = Integer.valueOf(properties.getProperty("depth.max"));
		Configuration.indexationMaxThreads = Integer.valueOf(properties.getProperty("indexation.maxThreads"));
		Configuration.indexationTimeoutMins = Integer.valueOf(properties.getProperty("indexation.timeoutMins"));
		Configuration.graphMaxThreads = Integer.valueOf(properties.getProperty("methodGraph.maxThreads"));
		Configuration.graphTimeoutMins = Integer.valueOf(properties.getProperty("methodGraph.timeoutMins"));
		Configuration.graphTimeoutMins = Integer.valueOf(properties.getProperty("methodGraph.timeoutMins"));
		Configuration.maxRecordsPerFile = Integer.valueOf(properties.getProperty("report.maxRecordsPerFile"));
		Configuration.allowDaoDiscovery = Boolean.valueOf(properties.getProperty("allow.daos.discovery"));

		// Reporting configurations
		String outColumns = properties.getProperty("output.columns");
		Configuration.outputColumns = StringUtils.split(outColumns, ',');
		if (!areValidOutputColumns()) {
			throw new Exception("Configured columns are not valid.");
		}

		String rpFlowTxtSwStr = Configuration.properties.getProperty("report.flow.txt.switch");
		String rpFlowHtmlSwStr = Configuration.properties.getProperty("report.flow.html.switch");
		String rpSummaryCsvSwStr = Configuration.properties.getProperty("report.summary.csv.switch");
		String rpSummaryHtmlSwStr = Configuration.properties.getProperty("report.summary.html.switch");
		String rpPmdTxtSwStr = Configuration.properties.getProperty("report.pmd.feed.switch");
		rpFlowTxtSw = "on".equalsIgnoreCase(rpFlowTxtSwStr);
		rpFlowHtmlSw = "on".equalsIgnoreCase(rpFlowHtmlSwStr);
		rpSummaryCsvSw = "on".equalsIgnoreCase(rpSummaryCsvSwStr);
		rpSummaryHtmlSw = "on".equalsIgnoreCase(rpSummaryHtmlSwStr);
		Configuration.rpPmdTxtSw = "on".equalsIgnoreCase(rpPmdTxtSwStr);

		String rpSingleSummaryCsvSwStr = Configuration.properties.getProperty("report.single.summary.csv.switch");
		String rpSinglePmdTxtSwStr = Configuration.properties.getProperty("report.single.pmd.feed.switch");
		Configuration.rpSingleSummaryCsvSw = "on".equalsIgnoreCase(rpSingleSummaryCsvSwStr);
		Configuration.rpSinglePmdTxtSw = "on".equalsIgnoreCase(rpSinglePmdTxtSwStr);

		String rpGlobalStatisticsStr = Configuration.properties.getProperty("report.statistics.global.switch");
		String rpIndividualStatisticsStr = Configuration.properties.getProperty("report.statistics.individual.switch");
		Configuration.rpGlobalStatisticsSw = "on".equalsIgnoreCase(rpGlobalStatisticsStr);
		Configuration.rpIndividualStatisticsSw = "on".equalsIgnoreCase(rpIndividualStatisticsStr);

		// Serialization configuration
		Configuration.serializationOn = Boolean.valueOf(properties.getProperty("serialization.enable"));
		String ixMode = properties.getProperty("indexation.mode");
		if (StringUtils.isNotBlank(ixMode)) {
			Configuration.indexationMode = Constants.IndexationMode.valueOf(ixMode.toUpperCase());
		}

		// Class and package filtering configurations
		String includedPkgValue = properties.getProperty("packages.included");
		String[] includedPackagesArray = includedPkgValue.split(" ");
		for (String inPkg : includedPackagesArray) {
			Configuration.includedPackages.add(inPkg);
		}

		String excludedPkgValue = properties.getProperty("packages.excluded");
		String[] excludedPkgArray = excludedPkgValue.split(" ");
		for (String exPkg : excludedPkgArray) {
			Configuration.excludedPackages.add(exPkg);
		}

		Configuration.loadProjectClassFolders();

		Configuration.loadClassesFilter();
	}

	public static void loadClassesFilter() {
		Configuration.filter.setIncludedPackages(Configuration.includedPackages);
		Configuration.filter.setExcludedPackages(Configuration.excludedPackages);
		Configuration.filter.setExcludedClasses(Configuration.excludedClasses);
		Configuration.filter.setAllowCargos(Boolean.valueOf(Configuration.properties.getProperty("allow.cargos")));
		Configuration.filter.setAllowCollections(Boolean.valueOf(Configuration.properties.getProperty("allow.collections")));
		Configuration.filter.setAllowDaos(Boolean.valueOf(Configuration.properties.getProperty("allow.daos")));
		Configuration.filter.setAllowEjbs(Boolean.valueOf(Configuration.properties.getProperty("allow.ejbs")));
		Configuration.filter.setAllowEnums(Boolean.valueOf(Configuration.properties.getProperty("allow.enums")));
		Configuration.filter.setAllowVos(Boolean.valueOf(Configuration.properties.getProperty("allow.vos")));
		Configuration.filter.setAllowInterfaces(Boolean.valueOf(Configuration.properties.getProperty("allow.interfaces")));
		Configuration.filter.setAllowConstants(Boolean.valueOf(Configuration.properties.getProperty("allow.contants")));
		Configuration.filter.setAllowServlets(Boolean.valueOf(Configuration.properties.getProperty("allow.servlets")));
		Configuration.filter.setAllowCustomtags(Boolean.valueOf(Configuration.properties.getProperty("allow.customtags")));
		Configuration.filter.setAllowOrphanLeaves(Boolean.valueOf(Configuration.properties.getProperty("allow.orphanLeaves")));
		Configuration.filter.setAllowEverything(Boolean.valueOf(Configuration.properties.getProperty("allow.everything")));
	}

	public static boolean isDaoDiscoveryAllowed() {
		return allowDaoDiscovery && filter.isAllowCollections() && filter.isAllowDaos();
	}

	public static void loadProjectClassFolders() {
		String srcFolderValues = properties.getProperty("class.root.folders");
		String[] srcFolderArray = srcFolderValues.split(" ");
		for (String srcFolder : srcFolderArray) {
			srcFolder = srcFolder.trim();
			Configuration.projectClassFolders.add(srcFolder);
			// Configuration.projectClassFolders.add(srcFolder + "/src");
			// Configuration.projectClassFolders.add(srcFolder + "/bin");
			// Configuration.projectClassFolders.add(srcFolder + "/ejbModule");
		}
	}

	public static boolean isABE() {
		return Configuration.projectName.equals(Constants.PROJECT_ABE);
	}

	private static boolean areValidOutputColumns() {
		boolean areColumNamesValid = true;
		// Validates that all configured columns are valid for the report
		for (String columnName : Configuration.outputColumns) {
			try {
				if (columnName.startsWith(Constants.ENTRY_VAL_PREFIX)) {
					continue;
				}
				ReportColumn.valueOf(columnName);
			} catch (Exception e) {
				System.err.println("Given column [" + columnName + "] is not valid.");
				areColumNamesValid = false;
			}
		}
		return areColumNamesValid;
	}

}
