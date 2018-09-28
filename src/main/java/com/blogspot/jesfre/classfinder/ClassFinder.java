/**
 * 
 */
package com.blogspot.jesfre.classfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.common.Configuration;

/**
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 * Dec 7, 2016
 */
public class ClassFinder {

	private static final String PATH_JAVA_SOURCE = "/JavaSource/";
	private static final String PATH_EJB_MODULE = "/ejbModule/";
	private static final String PATH_SRC = "/src/";

	public static void main(String[] args) throws Exception {

		String searchThisClassesFilePath = null;
		if (args.length > 0) {
			Configuration.workingLocationPath = args[0];
			searchThisClassesFilePath = args[1];
		}
		if (StringUtils.isBlank(Configuration.workingLocationPath)) {
			System.err.println("Working directory is needed.");
			System.exit(-1);
		}

		String configurationFileName = null;
		if (args.length > 2) {
			configurationFileName = args[2];
		} else {
			configurationFileName = "config.properties";
		}

		File configFile = new File(Configuration.workingLocationPath + File.separator + configurationFileName);
		if (configFile.exists() == false) {
			System.err.println("No cofig file found.");
			System.exit(-1);
		}

		Configuration.loadConfigurations(configFile);

		File searchThisClassesPath = new File(searchThisClassesFilePath);
		if (!searchThisClassesPath.exists()) {
			searchThisClassesFilePath = Configuration.workingLocationPath + File.separator + searchThisClassesFilePath;
			searchThisClassesPath = new File(searchThisClassesFilePath);
		}
		if (!searchThisClassesPath.exists()) {
			throw new FileNotFoundException("Cannot find file " + searchThisClassesPath);
		}
		Map<String, String> knownClassesFileContent = new LinkedHashMap<String, String>();
		List<String> duplicatedClasses = new ArrayList<String>();

		System.out.println("Feed file is " + searchThisClassesFilePath);
		String workingPath = FilenameUtils.getFullPath(searchThisClassesFilePath);
		String outFileName = FilenameUtils.getBaseName(searchThisClassesFilePath);
		String outputFileLocation = workingPath + outFileName + "-result_classes.txt";
		System.out.println("Target output file is " + outputFileLocation);
		System.out.println();

		List<String> dontKnowClassList = FileUtils.readLines(searchThisClassesPath);
		Map<String, String> foundBaseNameMap = new LinkedHashMap<String, String>();

		for (String projectFolder : Configuration.projectClassFolders) {
			try {
				File projPath = new File(projectFolder);
				if (!projPath.exists()) {
					System.err.println(projPath + " location does not exist.");
					continue;
				}
				System.out.println("Looking into " + projectFolder);
				List<File> fileList = (List<File>) FileUtils.listFiles(projPath, new String[] { "java" }, true);
				for (File file : fileList) {
					String filePath = file.getAbsolutePath();

					filePath = filePath.replaceAll("\\\\", "/");
					String fileName = file.getName();
					String baseName = FilenameUtils.getBaseName(fileName);

					String jobId = null;
					String foundBaseName = null;
					if (dontKnowClassList.contains(baseName)) {
						foundBaseName = baseName;
					} else if (dontKnowClassList.contains(baseName + ".java")) {
						foundBaseName = baseName + ".java";
					} else {
						for (String className : dontKnowClassList) {
							String[] entryValues = StringUtils.splitPreserveAllTokens(className, '|');
							int valLength = entryValues.length;
							if (valLength > 0) {
								className = entryValues[0].trim();
							}
							if (valLength > 1) {
								jobId = entryValues[1].trim();
							}

							if (className.contains(baseName)) {
								foundBaseName = baseName;
							}
						}
					}

					if (StringUtils.isNotBlank(foundBaseName)) {
						String className = filePath;
						if (StringUtils.contains(className, PATH_SRC)) {
							className = StringUtils.remove(className, projectFolder + PATH_SRC);
						}
						if (StringUtils.contains(className, PATH_EJB_MODULE)) {
							className = StringUtils.remove(className, projectFolder + PATH_EJB_MODULE);
						}
						if (Configuration.isABE()) {
							if (StringUtils.contains(className, PATH_JAVA_SOURCE)) {
								className = StringUtils.remove(className, projectFolder + PATH_JAVA_SOURCE);
							}
						}
						/*
						 * If still has the project folder path,
						 * try by removing at least the folder path (that ugly ABE structure)
						 */
						if (StringUtils.contains(className, projectFolder)) {
							className = StringUtils.remove(className, projectFolder);
							if (className.charAt(0) == '/') {
								className = className.replaceFirst("/", "");
							}
						}

						className = className.replaceAll("/", ".");
						className = className.replaceAll("\\.java", "");
						if (foundBaseNameMap.containsKey(foundBaseName)) {
							String duplicatedPath = foundBaseNameMap.get(foundBaseName);
							duplicatedClasses.add("Duplicated " + className + "\n in " + duplicatedPath + "\n in " + filePath);
						} else {
							foundBaseNameMap.put(foundBaseName, filePath);
							if (StringUtils.isNotBlank(jobId)) {
								className += "||" + jobId;
							}
							knownClassesFileContent.put(className, filePath);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error while looking into " + projectFolder);
				e.printStackTrace();
			}
		}
		System.out.println("Found " + knownClassesFileContent.size() + " classes.");

		List<String> resultsFileContent = new ArrayList<String>(knownClassesFileContent.keySet());
		File outputFile = new File(outputFileLocation);
		FileUtils.writeLines(outputFile, resultsFileContent);
		System.out.println("\nCreated result file in " + outputFileLocation);
		System.out.println();

		List<String> errorsFileContent = new ArrayList<String>();
		if (duplicatedClasses.size() > 0) {
			System.out.println("Found " + duplicatedClasses.size() + " DUPLICATED classes. See output for details.");
			errorsFileContent.add("- Duplicated classes -");
			errorsFileContent.addAll(duplicatedClasses);
		}
		errorsFileContent.add("");
		if (dontKnowClassList.size() != knownClassesFileContent.size() + duplicatedClasses.size()) {
			List<String> missingFiles = new ArrayList<String>(dontKnowClassList);
			missingFiles.removeAll(foundBaseNameMap.keySet());
			if (missingFiles.size() > 0) {
				System.out.println("Found " + missingFiles.size() + " MISSING classes. See output for details.");
				errorsFileContent.add("- Missing classes -");
				errorsFileContent.addAll(missingFiles);
			}
		}
		String errorsFileLocation = workingPath + outFileName + "-errors.txt";
		File errorsFile = new File(errorsFileLocation);
		FileUtils.writeLines(errorsFile, errorsFileContent);
		System.out.println("Created errors file in " + errorsFileLocation);

		System.out.println("End.");
	}
}
