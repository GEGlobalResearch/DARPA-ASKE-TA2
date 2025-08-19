package com.ge.research.aske.dbnJsonGenService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author alfredo
 *
 */

@SpringBootTest
@AutoConfigureMockMvc
public class DbnSpecGeneratorTest {

    @Autowired
    private MockMvc mockMvc;

    
    @Test
    public void testJsonGenerator() throws Exception {
    	String res = 
    			"{\"models\":{\"col_names\":[\"Model\",\"Input\",\"InputLabel\",\"UniqueInputLabel\",\"Output\",\"OutputLabel\",\"ModelForm\",\"Function\",\"ImpInput\",\"ImpInputAugType\",\"InpDeclaration\",\"ImpOutput\",\"ImpOutputAugType\",\"OutpDeclaration\",\"Initializer\",\"Dependency\"],\"rows\":[[\"http://materials.ge.com/NiEquations#specificHeatEq\",\"http://materials.ge.com/materials#moleFraction\",\"co\",\"http://materials.ge.com/NiEquations#specificHeatEq_co\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"def specificHeatEq(ni, co):\\n   tk = float(temp-32)*5/9 + 273.15\\n   tk1 = 0.001864 * tk\\n   tk2 = 0.000002 * tk**2\\n   return 0.0002390057 * (-0.191195*float(ni) -0.191177*float(co) - tk1 + tk2 + 416.332716 )\\n\",\"http://...\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"],[\"http://materials.ge.com/NiEquations#specificHeatEq\",\"http://materials.ge.com/materials#moleFraction\",\"ni\",\"http://materials.ge.com/NiEquations#specificHeatEq_ni\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"def specificHeatEq(ni, co):\\n   tk = float(temp-32)*5/9 + 273.15\\n   tk1 = 0.001864 * tk\\n   tk2 = 0.000002 * tk**2\\n   return 0.0002390057 * (-0.191195*float(ni) -0.191177*float(co) - tk1 + tk2 + 416.332716 )\\n\",\"http://...\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"],[\"http://materials.ge.com/NiEquations#specificHeatEq\",\"http://materials.ge.com/testing#testTemperature\",\"temp\",\"http://materials.ge.com/NiEquations#specificHeatEq_temp\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"def specificHeatEq(ni, co):\\n   tk = float(temp-32)*5/9 + 273.15\\n   tk1 = 0.001864 * tk\\n   tk2 = 0.000002 * tk**2\\n   return 0.0002390057 * (-0.191195*float(ni) -0.191177*float(co) - tk1 + tk2 + 416.332716 )\\n\",\"http://...\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]],\"type\":\"TABLE\",\"col_type\":[\"uri\",\"uri\",\"literal\",\"uri\",\"uri\",\"\",\"literal\",\"literal\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"],\"col_count\":16,\"row_count\":3},"
    			+ "\"nodes\":"
    			+ "  {\"col_names\":[\"Node\",\"NodeOutputUnits\",\"Child\",\"ChildInputUnits\",\"Eq\",\"InlineEq\",\"Value\",\"Lower\",\"Upper\",\"Variable\"],"
    			+ "  \"rows\":[[\"http://materials.ge.com/materials#moleFraction\",\"\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq\",\"\",0.160404,\"\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq_co\"],[\"http://materials.ge.com/materials#moleFraction\",\"\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq\",\"\",0.521076,\"\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq_ni\"],[\"http://materials.ge.com/testing#specificHeat\",\"\",\"\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq\",\"\",\"\",\"\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq_v10\"],[\"http://materials.ge.com/testing#testTemperature\",\"\",\"http://materials.ge.com/testing#specificHeat\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq\",\"\",1200.0,\"\",\"\",\"http://materials.ge.com/NiEquations#specificHeatEq_temp\"]],"
    			+ "  \"type\":\"TABLE\","
    			+ "  \"col_type\":[\"uri\",\"\",\"uri\",\"\",\"uri\",\"\",\"\",\"\",\"\",\"uri\"],"
    			+ "  \"col_count\":10,\"row_count\":4},"
    			+ "\"expressions\":{\"col_names\":[\"Model\",\"ModelForm\",\"Function\",\"Initializer\",\"Dependency\"],\"rows\":[[\"http://materials.ge.com/NiEquations#specificHeatEq\",\"def specificHeatEq(ni, co):\\n   tk = float(temp-32)*5/9 + 273.15\\n   tk1 = 0.001864 * tk\\n   tk2 = 0.000002 * tk**2\\n   return 0.0002390057 * (-0.191195*float(ni) -0.191177*float(co) - tk1 + tk2 + 416.332716 )\\n\",\"http://...\",\"\",\"\"]],\"type\":\"TABLE\",\"col_type\":[\"uri\",\"literal\",\"literal\",\"\",\"\"],\"col_count\":5,\"row_count\":1},"
    			+ "\"context\":\"\","
    			+ "\"numOfModels\":\"1\","
    			+ "\"modelIndex\":\"1\","
    			+ "\"computeLayer\":\"kchain\"}\n";
        
    	MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/dbn/jsonGenerator")
//                .header("testHeader", "headerValue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(res) 
//                .param("sadlResultSet", res)
//                .body("sadlResultSet", res)
                ;
      
    	try {
    		mockMvc.perform(builder)
    			.andExpect(status().isOk())
	        	.andDo(print())
	        	;
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

    }
}
