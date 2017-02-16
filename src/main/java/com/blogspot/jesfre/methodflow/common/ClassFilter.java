/**
 * 
 */
package com.blogspot.jesfre.methodflow.common;

import java.util.List;

/**
 * @author <a href="mailto:jruizaquino@deloitte.com">Jorge Ruiz Aquino</a>
 * Dec 1, 2016
 */
public class ClassFilter {
	private boolean allowEverything = false;
	private boolean allowCargos = true;
	private boolean allowCollections = true;
	private boolean allowDaos = true;
	private boolean allowInterfaces = true;
	private boolean allowEnums = true;
	private boolean allowVos = true;
	private boolean allowEjbs = true;
	private boolean allowConstants = true;
	private boolean allowServlets = true;
	private boolean allowCustomtags = true;
	private boolean allowOrphanLeaves = true;
	private List<String> includedPackages = null;
	private List<String> excludedPackages = null;
	private List<String> excludedClasses = null;

	public boolean isAllowEverything() {
		return allowEverything;
	}

	public void setAllowEverything(boolean allowEverything) {
		this.allowEverything = allowEverything;
	}

	public boolean isAllowCargos() {
		return allowCargos;
	}

	public void setAllowCargos(boolean allowCargos) {
		this.allowCargos = allowCargos;
	}

	public boolean isAllowCollections() {
		return allowCollections;
	}

	public void setAllowCollections(boolean allowCollections) {
		this.allowCollections = allowCollections;
	}

	public boolean isAllowDaos() {
		return allowDaos;
	}

	public void setAllowDaos(boolean allowDaos) {
		this.allowDaos = allowDaos;
	}

	public boolean isAllowInterfaces() {
		return allowInterfaces;
	}

	public void setAllowInterfaces(boolean allowInterfaces) {
		this.allowInterfaces = allowInterfaces;
	}

	public boolean isAllowEnums() {
		return allowEnums;
	}

	public void setAllowEnums(boolean allowEnums) {
		this.allowEnums = allowEnums;
	}

	public boolean isAllowVos() {
		return allowVos;
	}

	public void setAllowVos(boolean allowVos) {
		this.allowVos = allowVos;
	}

	public boolean isAllowEjbs() {
		return allowEjbs;
	}

	public void setAllowEjbs(boolean allowEjbs) {
		this.allowEjbs = allowEjbs;
	}

	public boolean isAllowConstants() {
		return allowConstants;
	}

	public void setAllowConstants(boolean allowConstants) {
		this.allowConstants = allowConstants;
	}

	public boolean isAllowServlets() {
		return allowServlets;
	}

	public void setAllowServlets(boolean allowServlets) {
		this.allowServlets = allowServlets;
	}

	public boolean isAllowCustomtags() {
		return allowCustomtags;
	}

	public void setAllowCustomtags(boolean allowCustomtags) {
		this.allowCustomtags = allowCustomtags;
	}

	public boolean isAllowOrphanLeaves() {
		return allowOrphanLeaves;
	}

	public void setAllowOrphanLeaves(boolean allowOrphanLeaves) {
		this.allowOrphanLeaves = allowOrphanLeaves;
	}

	public List<String> getIncludedPackages() {
		return includedPackages;
	}

	public void setIncludedPackages(List<String> includedPackages) {
		this.includedPackages = includedPackages;
	}

	public List<String> getExcludedPackages() {
		return excludedPackages;
	}

	public void setExcludedPackages(List<String> excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	public List<String> getExcludedClasses() {
		return excludedClasses;
	}

	public void setExcludedClasses(List<String> excludedClasses) {
		this.excludedClasses = excludedClasses;
	}

}
