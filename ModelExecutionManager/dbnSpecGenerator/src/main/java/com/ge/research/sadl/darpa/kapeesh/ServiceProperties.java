package com.ge.research.sadl.darpa.kapeesh;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="dbn", ignoreUnknownFields = true)
public class ServiceProperties {
	
	private String taskName = null;
	private String techniqueName = null;
	private String modelName = null;
	private String dataSources = null;
	private String dataSourceIds = null;
	private String dataSourcesInfo = null;
	private String additionalFilesIds = null;
	private String inputs = null;
	private String outputs = null;
	private String headerMappings = null;
	private String workDirRoot = null;

	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public String getTechniqueName() {
		return techniqueName;
	}
	public void setTechniqueName(String techniqueName) {
		this.techniqueName = techniqueName;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	public String getDataSources() {
		return dataSources;
	}
	public void setDataSources(String dataSources) {
		this.dataSources = dataSources;
	}
	public String getDataSourceIds() {
		return dataSourceIds;
	}
	public void setDataSourceIds(String dataSourceIds) {
		this.dataSourceIds = dataSourceIds;
	}
	public String getDataSourcesInfo() {
		return dataSourcesInfo;
	}
	public void setDataSourcesInfo(String dataSourcesInfo) {
		this.dataSourcesInfo = dataSourcesInfo;
	}
	public String getAdditionalFilesIds() {
		return additionalFilesIds;
	}
	public void setAdditionalFilesIds(String additionalFilesIds) {
		this.additionalFilesIds = additionalFilesIds;
	}
	public String getInputs() {
		return inputs;
	}
	public void setInputs(String inputs) {
		this.inputs = inputs;
	}
	public String getOutputs() {
		return outputs;
	}
	public void setOutputs(String outputs) {
		this.outputs = outputs;
	}
	public String getHeaderMappings() {
		return headerMappings;
	}
	public void setHeaderMappings(String headerMappings) {
		this.headerMappings = headerMappings;
	}
	public String getWorkDirRoot() {
		return workDirRoot;
	}
	public void setWorkDirRoot(String workDirRoot) {
		this.workDirRoot = workDirRoot;
	}
		
}
