package com.ge.research.sadl.darpa.kapeesh;

import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
		dbn_all.put("workDir", prop.getWorkDirRoot() + "/" + UUID.randomUUID().toString());

		initializeAnalyticSettings(prop);
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
		analyticSettings.put("Models", new JSONObject());
		analyticSettings.put("Nodes", new JSONObject());
		analyticSettings.put("InspectionSchedule", new JSONObject());
		analyticSettings.put("riskRollup", new JSONObject());
		analyticSettings.put("maintenanceLimits", new JSONArray());
		analyticSettings.put("observationDataSources", new JSONObject());
		analyticSettings.put("fullNetwork", new JSONObject());
		analyticSettings.put("DBNSetup", new JSONObject());

		dbn_all.put("analyticSettings", analyticSettings);
	}

	public JSONObject createModelObject (JSONObject models) {


		return new JSONObject();
	}

	public JSONObject createNodeObject (JSONObject nodes) {



		return new JSONObject();
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
