package com.blogspot.jesfre.methodflow.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.methodflow.visitor.Constants;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Nov 17, 2016
 */
public class ReportEngine {
	private StringBuffer reportContent = new StringBuffer();
	private File currentFile = null;
	private String reportName = null;
	private String headerContent = null;
	private boolean writeToConsole = false;
	private String reportLocationPath = null;
	private long maxLinesPerFile = 0;
	private long contentLineCounter = 0;
	private int fileCounter = 0;
	private boolean splitFile = false;
	private boolean isOn = true;

	/**
	 * 
	 * @param reportName
	 * @param reportLocationPath
	 * @param maxLinesPerFile <b>0</b> for single file, unlimited lines
	 */
	public ReportEngine(String reportName, String reportLocationPath, long maxLinesPerFile) {
		if (StringUtils.isBlank(reportName) || StringUtils.isBlank(reportLocationPath)) {
			throw new IllegalArgumentException("Given report name or location is illegal.");
		}
		this.reportName = reportName;
		this.reportLocationPath = reportLocationPath;
		this.splitFile = maxLinesPerFile > 0;
		this.maxLinesPerFile = maxLinesPerFile;
		initNewFile();
	}

	private void initNewFile() {
		String fileName = null;
		if (splitFile) {
			fileCounter++;
			String ext = FilenameUtils.getExtension(reportName);
			String baseName = FilenameUtils.getBaseName(reportName);
			fileName = baseName + Constants.STR_HYPHEN + fileCounter + Constants.STR_DOT + ext;
		} else {
			fileName = reportName;
		}

		currentFile = new File(reportLocationPath + File.separator + fileName);
		if (currentFile.exists()) {
			currentFile.delete();
		}
		contentLineCounter = 0;
	}

	public void appendHeader(String line) {
		if (!isOn) {
			return;
		}
		headerContent = line;
		appendLine(line);
	}

	/**
	 * Appends a single line of content to the current content
	 * 
	 * @param line
	 */
	public void appendLine(String line) {
		if (!isOn) {
			return;
		}
		reportContent.append(line);
		reportContent.append("\n");
		if (writeToConsole) {
			System.out.println(line);
		}
		contentLineCounter++;
		if (maxLinesPerFile > 0 && contentLineCounter >= maxLinesPerFile) {
			try {
				writeReport(false);
				initNewFile();
				if (StringUtils.isNotBlank(headerContent)) {
					this.appendLine(headerContent);
				}
			} catch (IOException e) {
				System.err.println("Can't write current content to create a new file.");
			}
		}
	}

	/**
	 * Appends the new content to the current report content
	 * 
	 * @param content
	 */
	public void appendContent(String newContent, int addedLines) {
		if (!isOn) {
			return;
		}
		reportContent.append(newContent);
		if (writeToConsole) {
			System.out.println(newContent);
		}
		contentLineCounter += addedLines;
		if (maxLinesPerFile > 0 && contentLineCounter >= maxLinesPerFile) {
			try {
				writeReport(false);
				initNewFile();
				if (StringUtils.isNotBlank(headerContent)) {
					this.appendLine(headerContent);
				}
			} catch (IOException e) {
				System.err.println("Can't write current content to create a new file.");
			}
		}
	}

	/**
	 * Writes the report in the given location
	 * 
	 * @param keepContent false to clear the current content
	 * @throws IOException
	 */
	public void writeReport(boolean keepContent) throws IOException {
		if (!isOn) {
			return;
		}
		if (reportLocationPath == null) {
			URL locationUrl = ReportEngine.class.getProtectionDomain().getCodeSource().getLocation();
			reportLocationPath = locationUrl.getPath();
		}
		File newReportPath = new File(reportLocationPath);
		if (!newReportPath.exists()) {
			newReportPath.mkdirs();
		}
		if (currentFile == null) {
			currentFile = new File(reportLocationPath + File.separator + reportName);
		}
		if (!currentFile.exists()) {
			currentFile.createNewFile();
		}
		FileWriter fileWritter = new FileWriter(currentFile.getAbsolutePath(), true);
		BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		bufferWritter.write(reportContent.toString());
		bufferWritter.close();

		if (!keepContent) {
			reportContent = new StringBuffer();
		}
		// System.out.println("Generated file " + newReportFile.getAbsolutePath());
	}

	public int getFileCounter() {
		return fileCounter;
	}

	public long getLineCounter() {
		return contentLineCounter;
	}

	public void enableConsoleLogging() {
		writeToConsole = true;
	}

	public void disableConsoleLogging() {
		writeToConsole = false;
	}

	public void off() {
		isOn = false;
	}

	public void on() {
		isOn = true;
	}

}
