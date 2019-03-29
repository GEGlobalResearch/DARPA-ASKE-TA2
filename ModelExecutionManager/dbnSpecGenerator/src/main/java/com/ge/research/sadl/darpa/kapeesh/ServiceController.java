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
					       @RequestParam(name="models") String models) throws Exception {
		
		JSONObject nodesJSON = new JSONObject();
		JSONObject modelsJSON = new JSONObject();

		if (nodes != null && !nodes.isEmpty()) 
			nodesJSON = Utility.createTable(nodes).toJson();
		if (models != null && !models.isEmpty()) 
			modelsJSON = Utility.createTable(models).toJson();

		JSONObject resultSetJSON = new JSONObject();

		resultSetJSON.put("nodes", nodesJSON);
		resultSetJSON.put("models", modelsJSON);

		return resultSetJSON;
	}

	@CrossOrigin
	@RequestMapping(value="/jsonGenerator", method=RequestMethod.POST)
	public JSONObject generateJSON (@RequestBody JSONObject sadlResultSet) throws Exception {

	        DbnJsonGenerator generator = new DbnJsonGenerator();

                generator.initializeJSON(prop);

		generator.createModelObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("models")));

		generator.createNodeObject(Utility.getJsonFromMap((LinkedHashMap)sadlResultSet.get("nodes")));

		generator.createMapObject();

                generator.printDbnJSON();

		return generator.dbn_all;
	}
}
