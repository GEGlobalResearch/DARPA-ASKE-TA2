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

import java.util.LinkedHashMap;

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
	@RequestMapping(value="/jsonGenerator", method=RequestMethod.POST)
	public JSONObject generateJSON (@RequestBody JSONObject sadlResultSet) throws Exception {

	        DbnJsonGenerator generator = new DbnJsonGenerator();

                generator.setExecutionMode((String)sadlResultSet.get("mode"));

                generator.initializeJSON(prop);

		generator.createModelObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")));

		generator.updateObservationData(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("data")));

		generator.createNodeObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")),
					   Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")));

		generator.createMapObject();

                generator.printDbnJSON();

		return generator.dbn_all;
	}
}
