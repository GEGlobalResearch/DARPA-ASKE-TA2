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

import java.lang.StringBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.sadl.darpa.kapeesh.utility.Table;

/**
 * Class to enable translation of SADL-generated
 * SPARQL query results into JSON input for KCHAIN
 * execution
 *
 */


public class KchainJsonGenerator {

	public JSONObject kchain_all;

	public JSONObject kchain_models;
	public JSONObject kchain_nodes;

	public HashMap<String, String> variableShortNameMap;

	public String kchainExecMode;
	public boolean hypothesis;
	public boolean hasUnitConversion;
	public boolean hasExternalFunction;

	/**
	 * Constructors
	 */
	public KchainJsonGenerator() {
		this.kchain_all = new JSONObject();

	}

	public KchainJsonGenerator(JSONObject kchain) {
		this.kchain_all = kchain;
	}

	/**
	 * Initialize the KCHAIN JSON with all the default elements
	 */
	public void initializeJSON(ServiceProperties prop) {

		kchain_all.put("inputVariables", new JSONArray());
		kchain_all.put("outputVariables", new JSONArray());
		kchain_all.put("equationModel", "");
		kchain_all.put("modelName", "getResponse");

		//hypothesis = false;
		//hasUnitConversion = false;
		//hasExternalFunction = false;
	}

	/**
	 * Set the execution mode for the KCHAIN (e.g. 'prognostic', 'calibration', 'explanation', etc.
	 */
	public void setExecutionMode(String mode) {
		kchainExecMode = mode;
	}

	/**
	 * Initialize the KCHAIN Analytic Settings element with default values
	 */
	public void initializeAnalyticSettings(ServiceProperties prop) {
		
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

		JSONObject analyticSettings = (JSONObject) kchain_all.get("analyticSettings");
		analyticSettings.put("ObservationData", ObservationData);
		kchain_all.put("analyticSettings", analyticSettings);

		hypothesis = true;
	    }

	}
		
	public JSONObject createInitialKCHAINSetup() {
		// Placeholder for any default values for KCHAIN settings (e.g. number of samples, etc)
		return new JSONObject();
	}

	public void updateNumberOfSamples(int numSamples) {
	}

	public void initializeShortNames(Table table) {

		String[] inputs = table.getColumn("Input");
		String[] labels = table.getColumn("InputLabel");

		variableShortNameMap = new HashMap<String, String>();

		for (int count = 0; count < inputs.length; count++) {
			if (!variableShortNameMap.containsKey(inputs[count]))
				variableShortNameMap.put(inputs[count], labels[count]);
		}

		return;
	}

	public void createInputVariablesObject (JSONObject models, JSONObject nodes) throws Exception {

		Table table = Table.fromJson(models);
		Table nodeTable = Table.fromJson(nodes);

		JSONArray kchainInputs = new JSONArray();

                ArrayList<String> inputVarNames = new ArrayList<String>();
                HashSet<String> childNodes = new HashSet<String>(Arrays.asList(nodeTable.getColumnUniqueValues("Child")));
                HashSet<String> parentNodes = new HashSet<String>(Arrays.asList(nodeTable.getColumnUniqueValues("Node")));
                parentNodes.removeAll(childNodes);
                for (String parentNode : parentNodes) {
                        Table filteredTable = nodeTable.getSubsetWhereMatches("Node", parentNode, new String[]{"Node", "Eq"});
                        HashMap<String, String> filterMap = new HashMap<String, String>();
                        filterMap.put("Model", filteredTable.getCell(0, "Eq"));
                        filterMap.put("ImpInputAugType", filteredTable.getCell(0, "Node"));
                        filteredTable = table.getSubsetBySubstring(filterMap);
                        inputVarNames.add(filteredTable.getColumnUniqueValues("ImpInput")[0]);
                }

		for (String inputVarName : inputVarNames) {
			JSONObject inputVar = new JSONObject();
			inputVar.put("name", inputVarName);
			inputVar.put("type", "float");
			Table joinTable = table.getSubsetWhereMatches("ImpInput",inputVarName,new String[]{"ImpInputAugType"});
			String[] joinIds = joinTable.getColumnUniqueValues("ImpInputAugType");

			// TODO: Add checks
			Table filteredNodeTable = nodeTable.getSubsetWhereMatches("Node", joinIds[0], new String[]{"Value"});
			String[] values = filteredNodeTable.getColumnUniqueValues("Value");

			if (values.length > 0 && values[0] != "" && !values[0].isEmpty())
				inputVar.put("value", values[0]);

			kchainInputs.add(inputVar);
		}

		kchain_all.put("inputVariables", kchainInputs);
	}

	public void createOutputVariablesObject (JSONObject models, JSONObject nodes) throws Exception {

		Table table = Table.fromJson(models);
                Table nodesTable = Table.fromJson(nodes);

		JSONArray kchainOutputs = new JSONArray();

                Table subTable = nodesTable.getSubsetWhereAllEmpty(new String[]{"Child"}, new String[]{"Node", "Eq"});
                ArrayList<String> outputVarNames = new ArrayList<String>();
                for (int i = 0; i < subTable.getNumRows(); i++) {
                        HashMap<String, String> filterMap = new HashMap<String, String>();
                        filterMap.put("Model", subTable.getCell(i, "Eq"));
                        filterMap.put("ImpOutputAugType", subTable.getCell(i, "Node"));
                        Table filteredTable = table.getSubsetBySubstring(filterMap);
                        outputVarNames.add(filteredTable.getColumnUniqueValues("ImpOutput")[0]);
                }

		for (String outputVarName : outputVarNames) {
			JSONObject outputVar = new JSONObject();
			outputVar.put("name", outputVarName);
			outputVar.put("type", "float");

			kchainOutputs.add(outputVar);
		}

		kchain_all.put("outputVariables", kchainOutputs);
	}

	public void createEquationModelObject (JSONObject models, JSONObject nodes, JSONObject expressions) throws Exception {

		String eqnModel = "";
		Table table = Table.fromJson(models);

		HashSet<String> globalArrays = new HashSet<String>();
		HashMap<String, String> filterMap = new HashMap<String, String>();
		filterMap.put("InpDeclaration", "None");
		Table filteredTable = table.getSubsetBySubstring(filterMap);
		String[] inputArrays = filteredTable.getColumnUniqueValues("InpDeclaration");
		for (String inpArray : inputArrays)
			globalArrays.add(inpArray.replace("None", "0"));

		filterMap.remove("InpDeclaration");
		filterMap.put("OutpDeclaration", "None");
		filteredTable = table.getSubsetBySubstring(filterMap);
		String[] outputArrays = filteredTable.getColumnUniqueValues("OutpDeclaration");
		for (String outpArray : outputArrays)
			globalArrays.add(outpArray.replace("None", "0"));

		if (!globalArrays.isEmpty()) {
			eqnModel += "\n\"\"\" Global variable declarations \"\"\" \n";
		}
		for (String globalArray : globalArrays)
			eqnModel += globalArray + "\n";
		eqnModel += "\n";

		String[] methodNameURIs = table.getColumnUniqueValues("Model");
		for (String URI : methodNameURIs)
			eqnModel += addMethod(URI, models, expressions) + "\n";

		eqnModel += addGetResponseMethod(models, nodes, expressions);

System.out.println(eqnModel);
		kchain_all.put("equationModel", eqnModel);
	}


	public String addMethod (String modelURI, JSONObject models, JSONObject expressions) throws Exception {

		String eqnModel = "";
		Table table = Table.fromJson(models);

		Table subTable = table.getSubsetWhereMatches("Model", modelURI, new String[]{"ImpInput", "InpDeclaration", "ImpOutput", "OutpDeclaration"});

		HashMap<String, String> filterMap = new HashMap<String, String>();
		filterMap.put("InpDeclaration", "= 0");
		Table filteredTable = subTable.getSubsetBySubstring(filterMap);
		String[] inputs = filteredTable.getColumnUniqueValues("ImpInput");

		filterMap.remove("InpDeclaration");
		filterMap.put("OutpDeclaration", "= 0");
		filteredTable = subTable.getSubsetBySubstring(filterMap);
		String[] outputs = filteredTable.getColumnUniqueValues("ImpOutput");

		String insertString = "";
		List list = new ArrayList<String>(Arrays.asList(inputs));
		list.addAll(Arrays.asList(outputs));
		Collections.sort(list);

		if (list.size() > 0) {
			insertString = "    \"\"\" Specifying global scope for implicit variables \"\"\"\n";
			insertString += "    global " + String.join(", ", list) + "\n";
		}

		Table exprTable = Table.fromJson(expressions);
                Table subExprTable = exprTable.getSubsetWhereMatches("Model", modelURI, new String[]{"ModelForm"});
		String modelForm = subExprTable.getCellAsString(0, "ModelForm").replaceAll("\\r$","");
		String[] lines = modelForm.split("\n");
		list = new ArrayList<String>(Arrays.asList(lines));
		list.add(1, insertString);
		
		eqnModel = String.join("\n", list);
		
		return eqnModel + "\n";
	}


	public String addGetResponseMethod (JSONObject models, JSONObject nodes, JSONObject expressions) throws Exception {

		StringBuffer eqnModel = new StringBuffer();
		eqnModel.append("\n");
		
		Table table = Table.fromJson(models);
		Table nodesTable = Table.fromJson(nodes);

		Table subTable = nodesTable.getSubsetWhereAllEmpty(new String[]{"Child"}, new String[]{"Node", "Eq"});
		ArrayList<String> outputVarNames = new ArrayList<String>();
		for (int i = 0; i < subTable.getNumRows(); i++) {
			HashMap<String, String> filterMap = new HashMap<String, String>();
			filterMap.put("Model", subTable.getCell(i, "Eq"));
			filterMap.put("ImpOutputAugType", subTable.getCell(i, "Node"));
			Table filteredTable = table.getSubsetBySubstring(filterMap);
			outputVarNames.add(filteredTable.getColumnUniqueValues("ImpOutput")[0]);
		}
		String[] nodesWithNoChildren = subTable.getColumnUniqueValues("Node");
		
		ArrayList<String> inputVarNames = new ArrayList<String>();
		HashSet<String> childNodes = new HashSet<String>(Arrays.asList(nodesTable.getColumnUniqueValues("Child")));
		HashSet<String> parentNodes = new HashSet<String>(Arrays.asList(nodesTable.getColumnUniqueValues("Node")));
		parentNodes.removeAll(childNodes);
		for (String parentNode : parentNodes) {
			Table filteredTable = nodesTable.getSubsetWhereMatches("Node", parentNode, new String[]{"Node", "Eq"});
			HashMap<String, String> filterMap = new HashMap<String, String>();
			filterMap.put("Model", filteredTable.getCell(0, "Eq"));
			filterMap.put("ImpInputAugType", filteredTable.getCell(0, "Node"));
			filteredTable = table.getSubsetBySubstring(filterMap);
			inputVarNames.add(filteredTable.getColumnUniqueValues("ImpInput")[0]);
		}

		//Table filteredTable = table.getSubsetWhereNonEmpty(new String[]{"ImpInputAugType"},new String[]{"ImpInput"});
                //String[] inputVarNames = filteredTable.getColumnUniqueValues("ImpInput");
		HashMap<String, String> inputVarMap = new HashMap<String, String>();
		for (String inputVarName : inputVarNames)
			inputVarMap.put(inputVarName, inputVarName + "_val");
		eqnModel.append("def getResponse(" + String.join(",",inputVarMap.values()) + "):\n");

		if (! inputVarMap.keySet().isEmpty() ) {
			eqnModel.append("    global " + String.join(",",inputVarMap.keySet()) + "\n");
		}

		eqnModel.append("\n");
		subTable = table.getSubsetWhereAllEmpty(new String[]{"Initializer","Dependency","ImpInput"},new String[]{"Model"});
		String[] setterURIs = subTable.getColumnUniqueValues("Model");
		for (String setterURI : setterURIs) {
			String setterMethod = setterURI.substring(setterURI.lastIndexOf(".") + 1);
			eqnModel.append("    " + setterMethod + "()\n");
		}
		eqnModel.append("\n");
		
		subTable = table.getSubsetWhereMatches("Initializer", "true", new String[]{"Model"});
		String[] initializerURIs = subTable.getColumnUniqueValues("Model");
		for (String initializerURI : initializerURIs) {
			String initializerMethod = initializerURI.substring(initializerURI.lastIndexOf(".") + 1);
			eqnModel.append("    " + initializerMethod + "()\n");
		}

		eqnModel.append("\n");
		for (String inputVarName : inputVarNames) {
			eqnModel.append("    " + inputVarName + " = " + inputVarMap.get(inputVarName) + "\n");
		}
	
		//subTable = table.getSubsetWhereNonEmpty(new String[]{"ImpInputAugType","ImpOutputAugType"},new String[]{"Model"});
//System.out.println(table.toCSVString());
//System.out.println(subTable.toCSVString());
		
		ArrayList<String> methods = new ArrayList<String>();
		ArrayList<String> nodesThisRound = new ArrayList<String>(Arrays.asList(nodesWithNoChildren));
		while (!nodesThisRound.isEmpty()) {
			String node = nodesThisRound.get(0);
			Table roundTable = nodesTable.getSubsetWhereMatches("Child", node, new String[]{"Node", "Eq"});
			if (roundTable.getNumRows() > 0) {
				String[] eq = roundTable.getColumnUniqueValues("Eq");
				methods.add(eq[0]);
				String[] parents = roundTable.getColumnUniqueValues("Node");	
				for (String parent : parents) {
					nodesThisRound.add(parent);
				}
			}
			System.out.println("Removing: " + node);
			nodesThisRound.remove(0);
		}
		//String mainMethodURI = subTable.getColumnUniqueValues("Model")[0];
		Collections.reverse(methods);
		for (String method : methods) {
			String mainMethod = method.substring(method.lastIndexOf(".") + 1);
			eqnModel.append("    " + mainMethod + "()\n");
		}
		eqnModel.append("\n");

		eqnModel.append("    return " + String.join(",",outputVarNames) + "\n");
		
		return eqnModel.toString();
	}


        public JSONObject createArtifact(String mode) {
		
		JSONObject payload = new JSONObject();

		payload.put("outputVariables", (JSONArray) kchain_all.get("outputVariables"));
		payload.put("modelName", (String) kchain_all.get("modelName"));

		if (mode.equals("build")) {
			JSONArray inputVars = (JSONArray) kchain_all.get("inputVariables");
			JSONArray newInputVars = new JSONArray();
			for (Object o : inputVars) {
				JSONObject inputVar = (JSONObject) o;
				JSONObject newInputVar = new JSONObject();
				newInputVar.put("name", (String) inputVar.get("name"));
				newInputVar.put("type", (String) inputVar.get("type"));
				newInputVars.add(newInputVar);
			}
			payload.put("inputVariables", newInputVars);
			payload.put("equationModel", (String) kchain_all.get("equationModel"));
		} else if (mode.equals("eval")) {
			payload.put("inputVariables", (JSONArray) kchain_all.get("inputVariables"));
		}

		return payload;
	}


	public void printKchainJSON () {

		System.out.println(kchain_all.toString());

		// TODO: Provide some pretty-print capability
	}

        public String convertUnit(String label, String inUnit, String outUnit) {
		//return label + " * (__import__('pint').UnitRegistry().parse_expression('" + inUnit + "').to('" + outUnit + "').magnitude)";
		return " __import__('pint').UnitRegistry().Quantity(" + label + ",'" + inUnit + "').to('" + outUnit + "').magnitude";
	}

	public static void main (String[] args) {

		KchainJsonGenerator generator = new KchainJsonGenerator();

		generator.printKchainJSON();
	}
}
