/**
 * 
 */
package com.blogspot.jesfre.classfinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 7, 2016
 */
public class CheckEJBClasses {
	private static final String[] projectFolderPaths = {
			"C:/ProjectILIES/ILIES_BATCH/FrameworkEJB",
			"C:/ProjectILIES/ILIES_BATCH/AppealsEJB",
			"C:/ProjectILIES/ILIES_BATCH/BenefitMgtEJB",
			"C:/ProjectILIES/ILIES_BATCH/CorrespondanceEJB",
			"C:/ProjectILIES/ILIES_BATCH/ConversionEJB",
			"C:/ProjectILIES/ILIES_BATCH/EligibilityEJB",
			"C:/ProjectILIES/ILIES_BATCH/FrontofficeEJB",
			"C:/ProjectILIES/ILIES_BATCH/InterfacesEJB",
			"C:/ProjectILIES/ILIES_BATCH/SelfServiceEJB",
			"C:/ProjectILIES/ILIES_BATCH/SupportFunctionEJB",
			"C:/ProjectILIES/ILIES_BATCH/WVSEJB"
	};

	public static void main(String[] args) {
		int totalOfEjb = 0;
		int totalOfExcluded = 0;
		for (String projectFolder : projectFolderPaths) {
			List<String> ejbFilesInProject = new ArrayList<String>();
			List<String> excludedFilesInProject = new ArrayList<String>();
			File path = new File(projectFolder);
			List<File> fileList = (List<File>) FileUtils.listFiles(path, new String[] { "java" }, true);
			for (File file : fileList) {
				String filePath = file.getAbsolutePath();
				if (filePath.endsWith("Bean.java")) {
					ejbFilesInProject.add(filePath);
				} else {
					excludedFilesInProject.add(filePath);
				}
			}
			String projectName = projectFolder.substring(projectFolder.lastIndexOf('/') + 1);
			System.out.println(projectName + ":");
			System.out.println(" - Found " + ejbFilesInProject.size() + " EJB files.");
			System.out.println(" - Excluded " + excludedFilesInProject.size() + " files.");
			totalOfEjb += ejbFilesInProject.size();
			totalOfExcluded += excludedFilesInProject.size();
			System.out.println();
		}
		System.out.println("Total of EJB files from all modules: " + totalOfEjb);
		System.out.println("Total of excluded files from all modules: " + totalOfExcluded);
	}
}
