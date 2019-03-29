package com.ge.research.sadl.darpa.kapeesh;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.sadl.darpa.kapeesh.utility.Table;

/**
 * Class to enable translation of SADL-generated
 * SPARQL query results into JSON input for DBN
 * execution
 *
 */


public class DbnJsonGenerator {

	public JSONObject dbn_all;

	public JSONObject dbn_models;
	public JSONObject dbn_nodes;

	public HashMap<String, String> variableShortNameMap;

	public String dbnExecMode;
	public boolean hypothesis;

	/**
	 * Constructors
	 */
	public DbnJsonGenerator() {
		this.dbn_all = new JSONObject();

	}

	public DbnJsonGenerator(JSONObject dbn) {
		this.dbn_all = dbn;
	}

	/**
	 * Initialize the DBN JSON with all the default elements
	 */
	public void initializeJSON(ServiceProperties prop) {

		dbn_all.put("taskName", prop.getTaskName());
		dbn_all.put("techniqueName", prop.getTechniqueName());
		dbn_all.put("modelName", prop.getModelName());
		if (prop.getDataSources() == null || prop.getDataSources().isEmpty())
			dbn_all.put("dataSources", new JSONObject());
		if (prop.getDataSourceIds() == null || prop.getDataSourceIds().isEmpty())
			dbn_all.put("dataSourceIds", new JSONObject());
		if (prop.getAdditionalFilesIds() == null || prop.getAdditionalFilesIds().isEmpty())
			dbn_all.put("additionalFilesIds", new JSONObject());
		if (prop.getDataSourcesInfo() == null || prop.getDataSourcesInfo().isEmpty())
			dbn_all.put("dataSourcesInfo", new JSONObject());
		if (prop.getInputs() == null || prop.getInputs().isEmpty())
			dbn_all.put("inputs", new JSONArray());
		if (prop.getOutputs() == null || prop.getOutputs().isEmpty())
			dbn_all.put("outputs", new JSONArray());
		if (prop.getHeaderMappings() == null || prop.getHeaderMappings().isEmpty())
			dbn_all.put("headerMappings", new JSONObject());
		//dbn_all.put("workDir", prop.getWorkDirRoot() + "/" + UUID.randomUUID().toString());
		dbn_all.put("workDir", prop.getWorkDirRoot());

		initializeAnalyticSettings(prop);
		hypothesis = false;
	}

	/**
	 * Set the execution mode for the DBN (e.g. 'prognostic', 'calibration', 'explanation', etc.
	 */
	public void setExecutionMode(String mode) {
		dbnExecMode = mode;
	}

	/**
	 * Initialize the DBN Analytic Settings element with default values
	 */
	public void initializeAnalyticSettings(ServiceProperties prop) {
		
		JSONObject analyticSettings = new JSONObject();
		analyticSettings.put("ObservationData", new JSONObject());
		analyticSettings.put("ExpertKnowledge", new JSONObject());
		analyticSettings.put("PriorData", new JSONObject());
		analyticSettings.put("StateVariableDefinition", new JSONObject());
		analyticSettings.put("Models", new JSONObject());	// needs to be filled later
		analyticSettings.put("Nodes", new JSONObject());	// needs to be filled later
		analyticSettings.put("InspectionSchedule", new JSONObject());
		analyticSettings.put("riskRollup", new JSONObject());
		analyticSettings.put("maintenanceLimits", new JSONArray());
		analyticSettings.put("observationDataSources", new JSONObject());
		analyticSettings.put("fullNetwork", new JSONObject());
		analyticSettings.put("DBNSetup", createInitialDBNSetup());	// needs to be filled later
		JSONObject ut_node = new JSONObject();
		ut_node.put("UT_node_names", new JSONArray());
		analyticSettings.put("UT_node_init_data", ut_node);

		dbn_all.put("analyticSettings", analyticSettings);
	}

	/**
	 * Update the ObservationData for hypothesis testing
	 */
	public void updateObservationData(JSONObject obsData) throws Exception {
		
	    if (!obsData.isEmpty()) {
		Table table = Table.fromJson(obsData);

		JSONObject ObservationData = new JSONObject();
		JSONObject Data = new JSONObject();

		String[] colNames = table.getColumnNames();
		for (String colName : colNames) {
			JSONArray values = new JSONArray();
			for (int row = 0; row < table.getNumRows(); row++) {
				values.add(table.getCellAsFloat(row, colName));
			}
			for (String s : variableShortNameMap.keySet()) {
				String sNoURI = s.substring(s.indexOf("#") + 1);
				if (sNoURI.equals(colName)) {
					Data.put(variableShortNameMap.get(s), values);
				}
			}
		}	
		ObservationData.put("Data", Data);

		JSONObject analyticSettings = (JSONObject) dbn_all.get("analyticSettings");
		analyticSettings.put("ObservationData", ObservationData);
		dbn_all.put("analyticSettings", analyticSettings);

		hypothesis = true;
	    }

	}
		
	public JSONObject createInitialDBNSetup() {
		JSONObject dbnSetup = new JSONObject();

		dbnSetup.put("WorkDir", (String)dbn_all.get("workDir"));
		dbnSetup.put("NumberOfSamples", 500);
		dbnSetup.put("PlotFlag", false);
		dbnSetup.put("TrackingTimeSteps", 1);
		dbnSetup.put("TaskName", "Prognosis");		// or 'Calibration' -- apparently, this does not matter
		
		JSONObject pfOptions = new JSONObject();
		pfOptions.put("NodeNamesNotRecorded", new JSONArray());
		pfOptions.put("BackendKeepVectors", true);
		pfOptions.put("BackendFname", (String)dbn_all.get("workDir") + "/Results");
		pfOptions.put("BackendKeepScalars", true);
		pfOptions.put("ParallelProcesses", "5");
		pfOptions.put("ResampleThreshold", 0.4);
		pfOptions.put("Parallel", true);
		pfOptions.put("Backend", "RAM");
		dbnSetup.put("ParticleFilterOptions", pfOptions);

		return dbnSetup;
	}

	public void initializeShortNames(Table table) {

		String[] inputs = table.getColumn("Input");
		String[] labels = table.getColumn("InputLabel");

		variableShortNameMap = new HashMap<String, String>();

		for (int count = 0; count < inputs.length; count++) 
			variableShortNameMap.put(inputs[count], labels[count]);

		return;
	}

	public JSONObject createModelObject (JSONObject models) throws Exception {

		Table table = Table.fromJson(models);
		Random rand = new Random();

		JSONObject dbnModels = new JSONObject();

		initializeShortNames(table);

		String[] outputNames = table.getColumnUniqueValues("Output");
		for (String outputName : outputNames) {
			String outputNameNoURI = outputName.substring(outputName.indexOf("#") + 1);
	
			String varName = "";
			if (variableShortNameMap.containsKey(outputName))
				varName = variableShortNameMap.get(outputName);
			else {
				for (char c : outputNameNoURI.toCharArray()) {
					if (Character.isUpperCase(c))
						varName += Character.toLowerCase(c);
				}
				varName += String.valueOf(rand.nextInt(100));
				variableShortNameMap.put(outputName, varName);
			}

			System.out.println(outputNameNoURI + " mapped to " + varName);	
		}

		for (String outputName : outputNames) {
			JSONObject modelObject = new JSONObject();

			//Table labels = table.getSubsetWhereMatches("Output", outputName, new String[]{"InputLabel", "ModelForm"});
			Table filteredtable = table.getSubsetWhereMatches("Output", outputName);
			String[] labelNames = filteredtable.getColumnUniqueValues("InputLabel");
			JSONArray labelsJsonArr = new JSONArray();
			for (String labelName : labelNames)
				labelsJsonArr.add(labelName);
			String[] inputs = filteredtable.getColumnUniqueValues("Input");
			JSONArray inputsJsonArr = new JSONArray();
			for (String inputName : inputs)
				inputsJsonArr.add(variableShortNameMap.get(inputName));
			modelObject.put("Input", inputsJsonArr);

			modelObject.put("Type", "Equation");
			modelObject.put("FunctionName", variableShortNameMap.get(outputName));
			
			String[] modelFormNames = filteredtable.getColumnUniqueValues("ModelForm");
			modelObject.put("ModelForm", modelFormNames[0]);
			
			filteredtable = table.getSubsetWhereMatches("Input", outputName);
			String[] outputs = filteredtable.getColumnUniqueValues("Output");
			JSONArray outputsJsonArr = new JSONArray();
			for (String output : outputs)
				outputsJsonArr.add(variableShortNameMap.get(output));
			modelObject.put("Output", outputsJsonArr);

			dbnModels.put(variableShortNameMap.get(outputName), modelObject);
		}
		JSONObject analyticSettings = (JSONObject) dbn_all.get("analyticSettings");
		analyticSettings.put("Models", dbnModels);
		dbn_all.put("analyticSettings", analyticSettings);

		return dbnModels;
	}

	public JSONObject createNodeObject (JSONObject nodes) throws Exception {

		Table table = Table.fromJson(nodes);

		JSONObject dbnNodes = new JSONObject();

		String[] nodeNames = table.getColumnUniqueValues("Node");
		String[] nodesWithParent = table.getColumnUniqueValues("Child");	// includes ""

		for (String nodeName : nodesWithParent) {
		    if (!nodeName.isEmpty() && nodeName != "") {
			JSONObject nodeObject = new JSONObject();
			nodeObject.put("Type", "Deterministic");
			nodeObject.put("Tag", new JSONArray());
			nodeObject.put("Distribution", "");
			nodeObject.put("DistributionParameters", new JSONObject());
			nodeObject.put("ModelName", variableShortNameMap.get(nodeName));

			Table filteredtable = table.getSubsetWhereMatches("Child", nodeName);
			String[] parentNames = filteredtable.getColumnUniqueValues("Node");
			JSONArray parentJsonArr = new JSONArray();
			for (String parentName : parentNames)
				parentJsonArr.add(variableShortNameMap.get(parentName));
			nodeObject.put("Parents", parentJsonArr);

			filteredtable = table.getSubsetWhereMatches("Node", nodeName);
			String[] children = filteredtable.getColumnUniqueValues("Child");
			JSONArray childrenJsonArr = new JSONArray();
			for (String child : children)
				if (!child.isEmpty() && child != "") 
					childrenJsonArr.add(variableShortNameMap.get(child));
			nodeObject.put("Children", childrenJsonArr);

			dbnNodes.put(variableShortNameMap.get(nodeName), nodeObject);

			String[] value = filteredtable.getColumnUniqueValues("Value");
			if (value.length > 0 && !value[0].isEmpty() && value[0] != "") {
				// Possibly, a calibration task
				JSONObject obsNodeObject = new JSONObject();
				obsNodeObject.put("Type", "Stochastic_Transient_Observation");
				obsNodeObject.put("Tag", new JSONArray());
				obsNodeObject.put("Distribution", "Normal");
				JSONObject distParams = new JSONObject();
				distParams.put("mu", variableShortNameMap.get(nodeName));
				distParams.put("sigma", 0.05);		// TODO: Confirm is hardcoding this is ok
				obsNodeObject.put("DistributionParameters", distParams);
				JSONArray obsParentJsonArr = new JSONArray();
				obsParentJsonArr.add(variableShortNameMap.get(nodeName));
				obsNodeObject.put("Parents", obsParentJsonArr);
				obsNodeObject.put("ObservationNoise", 0.05);
				JSONArray values = new JSONArray();
				values.add(Double.parseDouble(value[0]));
				obsNodeObject.put("ObservationData", values);

				dbnNodes.put(variableShortNameMap.get(nodeName) + "_obs", obsNodeObject);
			}
		    }
		}

		for (String nodeName : nodeNames) {
		    if (!dbnNodes.containsKey(variableShortNameMap.get(nodeName))) {
			JSONObject nodeObject = new JSONObject();
			nodeObject.put("Type", "Stochastic_Transient");
			nodeObject.put("Tag", new JSONArray());
			nodeObject.put("InitialChildren", new JSONArray());	// TODO: Confirm if this is ok
			
			nodeObject.put("Parents", new JSONArray());
			Table filteredtable = table.getSubsetWhereMatches("Node", nodeName);
			String[] children = filteredtable.getColumnUniqueValues("Child");
			JSONArray childrenJsonArr = new JSONArray();
			for (String child : children)
				if (!child.isEmpty() && child != "") 
					childrenJsonArr.add(variableShortNameMap.get(child));
			nodeObject.put("Children", childrenJsonArr);

			String[] distributions = filteredtable.getColumnUniqueValues("Distribution");
			String distributionNameNoURI = distributions[0].substring(distributions[0].indexOf("#") + 1);
			distributionNameNoURI = distributionNameNoURI.substring(0,1).toUpperCase() + 
						distributionNameNoURI.substring(1);
			nodeObject.put("Distribution", distributionNameNoURI);
			
			String[] lower = filteredtable.getColumnUniqueValues("Lower");
			String[] upper = filteredtable.getColumnUniqueValues("Upper");
			String[] value = filteredtable.getColumnUniqueValues("Value");
			dbnNodes.put(variableShortNameMap.get(nodeName), nodeObject);
			JSONObject distribution = new JSONObject();
			distribution.put("lower", Double.parseDouble(lower[0]));
			distribution.put("upper", Double.parseDouble(upper[0]));
			nodeObject.put("DistributionParameters", distribution);
			JSONArray range = new JSONArray();
			range.add(Double.parseDouble(lower[0]));
			range.add(Double.parseDouble(upper[0]));
			nodeObject.put("Range", range);
			JSONArray values = new JSONArray();
			if (!hypothesis)
				nodeObject.put("IsDistributionFixed", true);
			if (!value[0].isEmpty() && value[0] != "") {
				values.add(Double.parseDouble(value[0]));
			} else if (dbnExecMode.equals("calibration") && !hypothesis) {
				nodeObject.put("IsDistributionFixed", false);
			}
			nodeObject.put("ObservationData", values);
		    }
		}

		JSONObject analyticSettings = (JSONObject) dbn_all.get("analyticSettings");
		analyticSettings.put("Nodes", dbnNodes);
		dbn_all.put("analyticSettings", analyticSettings);

		return dbnNodes;
	}

	public void createMapObject () throws Exception {

		JSONObject map = new JSONObject();

		for (Object name : variableShortNameMap.keySet())
			map.put(variableShortNameMap.get((String)name), (String)name);
		
		dbn_all.put("mapping", map);
		return;
	}

	public void printDbnJSON () {

		System.out.println(dbn_all.toString());

		// TODO: Provide some pretty-print capability
	}

	public static void main (String[] args) {

		DbnJsonGenerator generator = new DbnJsonGenerator();

		generator.printDbnJSON();
	}
}
