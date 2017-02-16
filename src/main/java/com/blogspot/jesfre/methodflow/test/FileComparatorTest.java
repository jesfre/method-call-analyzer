/**
 * 
 */
package com.blogspot.jesfre.methodflow.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.blogspot.jesfre.methodflow.visitor.Constants;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 12, 2016
 */
public class FileComparatorTest {
	private static final String STR_EMPTY = "";
	private static final String ROW_EQUAL = "<tr><td>%s</td><td>%s</td><tr>";
	private static final String ROW_NOT_EQUAL = "<tr style='background-color:yellow'><td>%s</td><td>%s</td><tr>";
	private static final String modelFilePath = "C:\\ProjectILIES\\java-callgraph-master-working-dir\\reports\\gov.illinois.ies.business.batch.co.COMultipleRecipientsPostProcessForAr-visited_methods_model_1.csv";
	private static final String testingClassName = "gov.illinois.ies.business.batch.co.COMultipleRecipientsPostProcessForAr";

	public static void main(String[] args) throws IOException {
		FileComparatorTest test = new FileComparatorTest();
		test.test("C:\\ProjectILIES\\java-callgraph-master-working-dir\\reports\\2-batch-classes");
	}

	public void test(String reportLocation) throws IOException {
		File modelFile = new File(modelFilePath);
		List<String> sortedModelFile = sortFileContent(modelFile);

		String testingFilePath = reportLocation + File.separator + String.format(Constants.VISITED_METHODS_REPORT_CSV, testingClassName);
		File testingFile = new File(testingFilePath);
		List<String> sortedTestingFile = sortFileContent(testingFile);

		int lastModelLineFound = -1;
		int lastTestingLineFound = -1;
		List<String> results = new ArrayList<String>();
		results.add("<style>th{font-weight:bold;}</style>");
		results.add("<table>");
		results.add("<tr><th>MODEL</th><th>NEW FILE</th>");
		for (int i = 0; i < sortedModelFile.size(); i++) {
			String modelFileLine = sortedModelFile.get(i);

			for (int j = lastTestingLineFound + 1; j < sortedTestingFile.size(); j++) {
				String testingFileLine = sortedTestingFile.get(j);

				if (modelFileLine.equals(testingFileLine)) {
					lastModelLineFound = i;
					lastTestingLineFound = j;
				} else {
					String newResultLine = String.format(ROW_NOT_EQUAL, modelFileLine, STR_EMPTY);
					results.add(newResultLine);
				}
			}
		}

		File output = new File(reportLocation + File.separator + String.format(Constants.VISITED_METHODS_REPORT_CSV, testingClassName) + ".html");
		FileUtils.writeLines(output, results);
	}

	public List<String> sortFileContent(File fileToSort) throws IOException {
		List<String> fileContent = FileUtils.readLines(fileToSort);
		Collections.sort(fileContent);
		return fileContent;
	}
}
