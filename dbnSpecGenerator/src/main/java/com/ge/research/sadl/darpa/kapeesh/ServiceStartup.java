package com.ge.research.sadl.darpa.kapeesh;

import java.util.TreeMap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  // print and validate properties - and exit if invalid
	  String[] propertyNames = {
			"dbn.taskName",
			"dbn.techniqueName",
			"dbn.modelName",
			"dbn.dataSources",
			"dbn.dataSourceIds",
			"dbn.dataSourcesInfo",
			"dbn.additionalFilesIds",
			"dbn.inputs",
			"dbn.outputs",
			"dbn.headerMappings",
			"dbn.workDirRoot"
	  };
	  TreeMap<String,String> properties = new TreeMap<String,String>();
	  for(String propertyName : propertyNames){
		  properties.put(propertyName, event.getApplicationContext().getEnvironment().getProperty(propertyName));
	  }
	  // Utility.validatePropertiesAndExitOnFailure(properties); 
	  
	  return;
  }
 
}
