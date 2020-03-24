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
import com.ge.research.sadl.darpa.kapeesh.utility.TopologicalSort;

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
	public HashMap<String, String> inputVarMap = new HashMap<String, String>();

	public String kchainExecMode;
	public boolean hypothesis;
	public boolean hasUnitConversion;
	public boolean hasExternalFunction;

	public int VIZSTARTPORT = 35700;
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

                HashSet<String> childNodes = new HashSet<String>(Arrays.asList(nodeTable.getColumnUniqueValues("Child")));
                HashSet<String> parentNodes = new HashSet<String>(Arrays.asList(nodeTable.getColumnUniqueValues("Node")));
                parentNodes.removeAll(childNodes);
                for (String parentNode : parentNodes) {
                        Table filteredTable = nodeTable.getSubsetWhereMatches("Node", parentNode, new String[]{"Node", "Eq", "InlineEq"});
			String inlineEq = filteredTable.getCell(0, "InlineEq");
                        HashMap<String, String> filterMap = new HashMap<String, String>();
                        filterMap.put("Model", filteredTable.getCell(0, "Eq"));
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	filterMap.put("Input", filteredTable.getCell(0, "Node"));
			} else {
                         	filterMap.put("ImpInputAugType", filteredTable.getCell(0, "Node"));
			}
                        filteredTable = table.getSubsetBySubstring(filterMap);
			String inputVarName = "";
			String aliasName = "";
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	inputVarName = filteredTable.getColumnUniqueValues("InputLabel")[0];
                        	aliasName = filteredTable.getColumnUniqueValues("Input")[0];
			} else {
                        	inputVarName = filteredTable.getColumnUniqueValues("ImpInput")[0];
                        	aliasName = filteredTable.getColumnUniqueValues("ImpInputAugType")[0];
			}

			JSONObject inputVar = new JSONObject();
			inputVar.put("name", inputVarName);
			inputVar.put("type", "float");
			inputVar.put("alias", aliasName.substring(aliasName.indexOf("#")+1));
			
			String[] joinIds;
			if (!inlineEq.isEmpty() && inlineEq != "") {
				Table joinTable = table.getSubsetWhereMatches("InputLabel",inputVarName,new String[]{"Input"});
				joinIds = joinTable.getColumnUniqueValues("Input");
			} else {
				Table joinTable = table.getSubsetWhereMatches("ImpInput",inputVarName,new String[]{"ImpInputAugType"});
				joinIds = joinTable.getColumnUniqueValues("ImpInputAugType");
			}

			// TODO: Add checks
			Table filteredNodeTable = nodeTable.getSubsetWhereMatches("Node", joinIds[0], new String[]{"Value","Lower","Upper"});
			String[] values = filteredNodeTable.getColumnUniqueValues("Value");
			String[] lowers = filteredNodeTable.getColumnUniqueValues("Lower");
			String[] uppers = filteredNodeTable.getColumnUniqueValues("Upper");

			if (values.length > 0 && values[0] != "" && !values[0].isEmpty()) {
				inputVar.put("value", values[0]);
			}
			if (lowers.length > 0 && lowers[0] != "" && !lowers[0].isEmpty()) {
				inputVar.put("minValue", filteredNodeTable.getCell(0, "Lower"));
			}
			if (uppers.length > 0 && uppers[0] != "" && !uppers[0].isEmpty()) {
				inputVar.put("maxValue", filteredNodeTable.getCell(0, "Upper"));
			}

			kchainInputs.add(inputVar);
		}

		kchain_all.put("inputVariables", kchainInputs);
	}

	public void createOutputVariablesObject (JSONObject models, JSONObject nodes) throws Exception {

		Table table = Table.fromJson(models);
                Table nodesTable = Table.fromJson(nodes);

		JSONArray kchainOutputs = new JSONArray();

                Table subTable = nodesTable.getSubsetWhereAllEmpty(new String[]{"Child"}, new String[]{"Node", "Eq", "InlineEq", "NodeOutputUnits"});
                for (int i = 0; i < subTable.getNumRows(); i++) {
                        HashMap<String, String> filterMap = new HashMap<String, String>();
                        filterMap.put("Model", subTable.getCell(i, "Eq"));
			String inlineEq = subTable.getCell(i, "InlineEq");
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	filterMap.put("Output", subTable.getCell(i, "Node"));
			} else {
                        	filterMap.put("ImpOutputAugType", subTable.getCell(i, "Node"));
			}
                        Table filteredTable = table.getSubsetBySubstring(filterMap);
			String outputVarName ="";
			String aliasName = "";
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	//outputVarName = filteredTable.getColumnUniqueValues("ImpOutput")[0];
                        	aliasName = filteredTable.getColumnUniqueValues("Output")[0];
			} else {
                        	outputVarName = filteredTable.getColumnUniqueValues("ImpOutput")[0];
                        	aliasName = filteredTable.getColumnUniqueValues("ImpOutputAugType")[0];
			}

			if (!inlineEq.isEmpty() && inlineEq != "") {
				String[] inlineEqComponents = inlineEq.split("=");
				JSONObject outputVar = new JSONObject();
				outputVar.put("name", inlineEqComponents[0]);
				outputVar.put("type", "float");
				outputVar.put("alias", aliasName.substring(aliasName.indexOf("#")+1));
				//outputVar.put("unit", subTable.getCell(i, "NodeOutputUnits"));

				kchainOutputs.add(outputVar);
			} else {
				JSONObject outputVar = new JSONObject();
				outputVar.put("name", outputVarName);
				outputVar.put("type", "float");
				outputVar.put("alias", aliasName.substring(aliasName.indexOf("#")+1));
				outputVar.put("unit", subTable.getCell(i, "NodeOutputUnits"));

				kchainOutputs.add(outputVar);
			}
		}

		kchain_all.put("outputVariables", kchainOutputs);
	}

	public void createEquationModelObject (JSONObject models, JSONObject nodes, JSONObject expressions, String context) throws Exception {

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

		eqnModel += addGetResponseMethod(models, nodes, expressions, context);

System.out.println(eqnModel);
		kchain_all.put("equationModel", eqnModel);
	}


	public String addMethod (String modelURI, JSONObject models, JSONObject expressions) throws Exception {

		String eqnModel = "";
		Table table = Table.fromJson(models);

		Table subTable = table.getSubsetWhereMatches("Model", modelURI, new String[]{"ImpInput", "InpDeclaration", "ImpOutput", "OutpDeclaration", "Input", "InputLabel"});

		String input = subTable.getCell(0, "Input");
		String inputLabel = subTable.getCell(0, "InputLabel");
		if (!input.isEmpty() && input != "" && !inputLabel.isEmpty() && inputLabel != "") {
			// non-empty input implying inline equation
			return "";
		}

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


	public String addGetResponseMethod (JSONObject models, JSONObject nodes, JSONObject expressions, String context) throws Exception {

		StringBuffer eqnModel = new StringBuffer();
		eqnModel.append("\n");
		
		Table table = Table.fromJson(models);
		Table nodesTable = Table.fromJson(nodes);

		Table subTable = nodesTable.getSubsetWhereAllEmpty(new String[]{"Child"}, new String[]{"Node", "Eq", "InlineEq"});
		ArrayList<String> outputVarNames = new ArrayList<String>();
		for (int i = 0; i < subTable.getNumRows(); i++) {
			HashMap<String, String> filterMap = new HashMap<String, String>();
			filterMap.put("Model", subTable.getCell(i, "Eq"));
			String inlineEq = subTable.getCell(i, "InlineEq");
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	filterMap.put("Output", subTable.getCell(i, "Node"));
			} else {
				filterMap.put("ImpOutputAugType", subTable.getCell(i, "Node"));
			}
			Table filteredTable = table.getSubsetBySubstring(filterMap);
			if (!inlineEq.isEmpty() && inlineEq != "") {
				String[] inlineEqComponents = inlineEq.split("=");
				outputVarNames.add(inlineEqComponents[0]);
			} else {
				outputVarNames.add(filteredTable.getColumnUniqueValues("ImpOutput")[0]);
			}
		}
		String[] nodesWithNoChildren = subTable.getColumnUniqueValues("Node");
		
		ArrayList<String> inputVarNames = new ArrayList<String>();
		HashSet<String> childNodes = new HashSet<String>(Arrays.asList(nodesTable.getColumnUniqueValues("Child")));
		HashSet<String> parentNodes = new HashSet<String>(Arrays.asList(nodesTable.getColumnUniqueValues("Node")));
		parentNodes.removeAll(childNodes);
		for (String parentNode : parentNodes) {
			Table filteredTable = nodesTable.getSubsetWhereMatches("Node", parentNode, new String[]{"Node", "Eq", "InlineEq"});
			HashMap<String, String> filterMap = new HashMap<String, String>();
			filterMap.put("Model", filteredTable.getCell(0, "Eq"));
			String inlineEq = filteredTable.getCell(0, "InlineEq");
			if (!inlineEq.isEmpty() && inlineEq != "") {
                        	filterMap.put("Input", filteredTable.getCell(0, "Node"));
			} else {
				filterMap.put("ImpInputAugType", filteredTable.getCell(0, "Node"));
			}
			filteredTable = table.getSubsetBySubstring(filterMap);
			if (!inlineEq.isEmpty() && inlineEq != "") {
				inputVarNames.add(filteredTable.getColumnUniqueValues("InputLabel")[0]);
			} else {
				inputVarNames.add(filteredTable.getColumnUniqueValues("ImpInput")[0]);
			}
		}

		//Table filteredTable = table.getSubsetWhereNonEmpty(new String[]{"ImpInputAugType"},new String[]{"ImpInput"});
                //String[] inputVarNames = filteredTable.getColumnUniqueValues("ImpInput");
		for (String inputVarName : inputVarNames)
			inputVarMap.put(inputVarName, inputVarName + "_val");
		eqnModel.append("def getResponse" + context + "(" + String.join(",",inputVarMap.values()) + "):\n");

		if (! inputVarMap.keySet().isEmpty() ) {
			eqnModel.append("    global " + String.join(",",inputVarMap.keySet()) + "\n");
		}

		eqnModel.append("\n");
		subTable = table.getSubsetWhereAllEmpty(new String[]{"Initializer","Dependency","ImpInput","InputLabel"},new String[]{"Model"});
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
		
		ArrayList<String> nodesNoChildren = new ArrayList<String>(Arrays.asList(nodesWithNoChildren));
		ArrayList<String> finalOrderedNodes = new ArrayList<String>();
		for (String parentNode : parentNodes) {
		    TopologicalSort tSort = new TopologicalSort();
		    String[] allNodes = nodesTable.getColumnUniqueValues("Node");
		    HashMap<String, TopologicalSort.Node> tMap = new HashMap<String, TopologicalSort.Node>();
		    for (String node : allNodes) {
		        TopologicalSort.Node tNode = new TopologicalSort.Node(node);
		        tMap.put(node, tNode);
		    }
		    for (String node : allNodes) {
		        Table childTable = nodesTable.getSubsetWhereMatches("Node", node, new String[]{"Child"});
		        String[] tChildren = childTable.getColumnUniqueValues("Child");
		        TopologicalSort.Node tNode = tMap.get(node);
		        for (String tChild : tChildren) {
			    if (!tChild.isEmpty() && tChild != "") {
				tNode.addneighbours(tMap.get(tChild));
				//System.out.println("Adding child: " + tChild + " to parent: " + node);
			    }
		        }
		    //System.out.println(tNode + " : " + tNode.getNeighbours());
		    }
//				System.out.println(tMap.toString());
		    tSort.topologicalSort(tMap.get(parentNode));

		    ArrayList<String> orderedNodes = new ArrayList<String>();
                    while (tSort.stack.empty()==false) {
			String popped = tSort.stack.pop().toString();
			if (nodesNoChildren.contains(popped))
			    orderedNodes.add(popped);
                      	//System.out.print(tSort.stack.pop() + " ");
                    }
		    if (finalOrderedNodes.size() < orderedNodes.size()) 
			finalOrderedNodes = orderedNodes;
		}

//System.out.println(">>>>>>>>>>>>>>>");
//System.out.println(finalOrderedNodes);

		ArrayList<String> methods = new ArrayList<String>();
		ArrayList<String> nodesThisRound = finalOrderedNodes; //new ArrayList<String>(Arrays.asList(nodesWithNoChildren));
		while (!nodesThisRound.isEmpty()) {
			String node = nodesThisRound.get(0);
			Table roundTable = nodesTable.getSubsetWhereMatches("Child", node, new String[]{"Node", "Eq", "InlineEq"});
			if (roundTable.getNumRows() > 0) {
				String[] inlineEq = roundTable.getColumnUniqueValues("InlineEq");
				if (!inlineEq[0].isEmpty() && inlineEq[0] != "") {
					if (!methods.contains(inlineEq[0])) {
						methods.add(inlineEq[0]);
					}
				} else {
					String[] eq = roundTable.getColumnUniqueValues("Eq");
					if (!methods.contains(eq[0])) {
						methods.add(eq[0]);
					}
				}
				String[] parents = roundTable.getColumnUniqueValues("Node");	
				for (String parent : parents) {
					nodesThisRound.add(parent);
				}
			}
//			System.out.println("Removing: " + node);
			nodesThisRound.remove(0);
		}
		//String mainMethodURI = subTable.getColumnUniqueValues("Model")[0];
		Collections.reverse(methods);
		for (String method : methods) {
			if (method.contains("=")) 
				eqnModel.append("    " + method + "\n");
			else {
				String mainMethod = method.substring(method.lastIndexOf(".") + 1);
				eqnModel.append("    " + mainMethod + "()\n");
			}
		}
		eqnModel.append("\n");

		eqnModel.append("    return " + String.join(",",outputVarNames) + "\n");
		
		return eqnModel.toString();
	}


        public JSONObject createArtifact(String mode, String context, String numOfModels, String modelIndex) {
		
		JSONObject payload = new JSONObject();

		payload.put("outputVariables", (JSONArray) kchain_all.get("outputVariables"));
		payload.put("modelName", (String) kchain_all.get("modelName") + context);
		payload.put("CGType", "python");

		if (mode.equals("build")) {
			JSONArray inputVars = (JSONArray) kchain_all.get("inputVariables");
			JSONArray newInputVars = new JSONArray();
			for (Object o : inputVars) {
				JSONObject inputVar = (JSONObject) o;
				JSONObject newInputVar = new JSONObject();
				newInputVar.put("name", inputVarMap.get((String) inputVar.get("name")));
				newInputVar.put("type", (String) inputVar.get("type"));
				newInputVar.put("alias", (String) inputVar.get("alias"));
				newInputVars.add(newInputVar);
			}
			payload.put("inputVariables", newInputVars);
			payload.put("equationModel", (String) kchain_all.get("equationModel"));
		} else if (mode.equals("eval")) {
			JSONArray inputVars = (JSONArray) kchain_all.get("inputVariables");
			JSONArray newInputVars = new JSONArray();
			for (Object o : inputVars) {
				JSONObject inputVar = (JSONObject) o;
				JSONObject newInputVar = new JSONObject();
				newInputVar.put("name", inputVarMap.get((String) inputVar.get("name")));
				newInputVar.put("type", (String) inputVar.get("type"));
				newInputVar.put("alias", (String) inputVar.get("alias"));
				newInputVar.put("value", (String) inputVar.get("value"));
				if (inputVar.containsKey("minValue"))
					newInputVar.put("minValue", (String) inputVar.get("minValue"));
				if (inputVar.containsKey("maxValue"))
					newInputVar.put("maxValue", (String) inputVar.get("maxValue"));
				newInputVars.add(newInputVar);
			}
			payload.put("inputVariables", newInputVars);
		} else if (mode.equals("visualize")) {
			JSONArray inputVars = (JSONArray) kchain_all.get("inputVariables");
			JSONArray newInputVars = new JSONArray();
			for (Object o : inputVars) {
				JSONObject inputVar = (JSONObject) o;
				JSONObject newInputVar = new JSONObject();
				newInputVar.put("name", inputVarMap.get((String) inputVar.get("name")));
				newInputVar.put("type", (String) inputVar.get("type"));
				newInputVar.put("alias", (String) inputVar.get("alias"));
				newInputVar.put("value", (String) inputVar.get("value"));
				if (inputVar.containsKey("minValue"))
					newInputVar.put("minValue", (String) inputVar.get("minValue"));
				if (inputVar.containsKey("maxValue"))
					newInputVar.put("maxValue", (String) inputVar.get("maxValue"));
				newInputVars.add(newInputVar);
			}
			payload.put("inputVariables", newInputVars);
			payload.put("plotType", "2");
			payload.put("portNum", VIZSTARTPORT + Integer.parseInt(modelIndex));
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
