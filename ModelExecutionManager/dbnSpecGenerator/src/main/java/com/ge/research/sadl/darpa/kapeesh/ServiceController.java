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

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.sadl.darpa.kapeesh.DbnJsonGenerator;
import com.ge.research.sadl.darpa.kapeesh.utility.Utility;

@CrossOrigin
@RestController
@RequestMapping("/dbn")
public class ServiceController {

	@Autowired
	ServiceProperties prop;

	@CrossOrigin
	@RequestMapping(value="/SADLResultSetToJson", method=RequestMethod.POST)
	public JSONObject convertCSVStrToJson (@RequestParam(name="nodes") String nodes,
					       @RequestParam(name="models") String models,
					       @RequestParam(name="mode") String executionMode,
					       @RequestParam(name="data") String data) throws Exception {
		
		JSONObject nodesJSON = new JSONObject();
		JSONObject modelsJSON = new JSONObject();
		JSONObject dataJSON = new JSONObject();

		if (nodes != null && !nodes.isEmpty()) 
			nodesJSON = Utility.createTable(nodes).toJson();
		if (models != null && !models.isEmpty()) 
			modelsJSON = Utility.createTable(models).toJson();
		if (data != null && !data.isEmpty()) 
			dataJSON = Utility.createTable(data).toJson();

		JSONObject resultSetJSON = new JSONObject();

		resultSetJSON.put("nodes", nodesJSON);
		resultSetJSON.put("models", modelsJSON);
		resultSetJSON.put("mode", executionMode);
		resultSetJSON.put("data", dataJSON);

		return resultSetJSON;
	}

        @CrossOrigin
        @RequestMapping(value="/SADLResultSetJsonToTable", method=RequestMethod.POST)
        public JSONObject convertCSVStrToJson (@RequestBody ArrayList<JSONObject> sadlResultSetJson) throws Exception {

		JSONObject resultSetJSON = new JSONObject();
		resultSetJSON.put("models", Utility.createTable(Utility.decipherValueList(sadlResultSetJson)).toJson());
		resultSetJSON.put("computeLayer", "kchain");

		return resultSetJSON;
        }

        @CrossOrigin
        @RequestMapping(value="/SADLResultSetJsonToTableJson", method=RequestMethod.POST)
        public JSONObject convertCSVStrToJson (@RequestBody JSONObject sadlResultSetJson) throws Exception {

		JSONObject resultSetJSON = new JSONObject();

		JSONObject models = Utility.getJsonFromMap((LinkedHashMap)sadlResultSetJson.get("models"));
		JSONArray cols = (JSONArray)((JSONObject)models.get("head")).get("vars");
		JSONArray bindings = (JSONArray)((JSONObject)models.get("results")).get("bindings");
		resultSetJSON.put("models", Utility.createTable(Utility.decipherValueList(Utility.expandBindings(bindings, cols))).toJson());

		JSONObject nodes = Utility.getJsonFromMap((LinkedHashMap)sadlResultSetJson.get("nodes"));
		cols = (JSONArray)((JSONObject)nodes.get("head")).get("vars");
		bindings = (JSONArray)((JSONObject)nodes.get("results")).get("bindings");
		resultSetJSON.put("nodes", Utility.createTable(Utility.decipherValueList(Utility.expandBindings(bindings, cols))).toJson());

		JSONObject expressions = Utility.getJsonFromMap((LinkedHashMap)sadlResultSetJson.get("expressions"));
		cols = (JSONArray)((JSONObject)expressions.get("head")).get("vars");
		bindings = (JSONArray)((JSONObject)expressions.get("results")).get("bindings");
		resultSetJSON.put("expressions", Utility.createTable(Utility.decipherValueList(Utility.expandBindings(bindings, cols))).toJson());

		resultSetJSON.put("context", sadlResultSetJson.get("context"));
		resultSetJSON.put("numOfModels", sadlResultSetJson.get("numOfModels"));
		resultSetJSON.put("modelIndex", sadlResultSetJson.get("modelNum"));
		resultSetJSON.put("computeLayer", "kchain");

		return resultSetJSON;
        }

	@CrossOrigin
	@RequestMapping(value="/jsonGenerator", method=RequestMethod.POST)
	public JSONObject generateJSON (@RequestBody JSONObject sadlResultSet) throws Exception {

	        String computeLayer = (String)sadlResultSet.get("computeLayer");
		JSONObject payload = new JSONObject();

	        if (computeLayer.equals("dbn")) {
	        	DbnJsonGenerator generator = new DbnJsonGenerator();

                	generator.setExecutionMode((String)sadlResultSet.get("mode"));

                	generator.initializeJSON(prop);

			generator.createModelObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")));

			generator.updateObservationData(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("data")));

			generator.createNodeObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")),
					   	   Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")));

			generator.createMapObject();

                	generator.printDbnJSON();

			payload = generator.dbn_all;
	    
	    	} else if (computeLayer.equals("kchain")) {

			KchainJsonGenerator generator = new KchainJsonGenerator();

                	generator.initializeJSON(prop);

			generator.createInputVariablesObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")),
							     Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")));
			generator.createOutputVariablesObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")),
							      Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")));
			generator.createEquationModelObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")),
							    Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")),
							    Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("expressions")),
							    (String)sadlResultSet.get("context"));

			payload.put("build", generator.createArtifact("build", (String)sadlResultSet.get("context"), "", ""));
			payload.put("eval", generator.createArtifact("eval", (String)sadlResultSet.get("context"), "", ""));
			payload.put("visualize", generator.createArtifact("visualize", (String)sadlResultSet.get("context"),
									  (String)sadlResultSet.get("numOfModels"), (String)sadlResultSet.get("modelIndex")));
		}

		return payload;
	}
}
