/**********************************************************************
* Note: This license has also been called the "New BSD License" or
* "Modified BSD License". See also the 2-clause BSD License.
*
* Copyright © 2018-2019 - General Electric Company, All Rights Reserved
*
* Project: KApEESH, developed with the support of the Defense Advanced
* Research Projects Agency (DARPA) under Agreement  No.  HR00111990007.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its
*    contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
* THE POSSIBILITY OF SUCH DAMAGE.
*
***********************************************************************/
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
