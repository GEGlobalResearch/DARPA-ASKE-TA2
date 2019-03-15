package com.ge.research.sadl.darpa.kapeesh.utility;

import java.io.UnsupportedEncodingException; 
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.sadl.darpa.kapeesh.utility.Table;

/**
 * Utility methods
 */
public abstract class Utility {

       public static Table createTable(String s) throws Exception {
                String[] content = decode(s).split(System.getProperty("line.separator"));

		String[] cols = content[0].split(",");
		String[] colTypes = new String[cols.length];
		Arrays.fill(colTypes, "string");

		ArrayList<String> rows = new ArrayList<String>(Arrays.asList(content));
		rows.remove(0);
		
		ArrayList<ArrayList<String> > rowsList = new ArrayList<ArrayList<String> >();
		for (String row : rows) {
			String[] rowContent = row.split(",", -1);
			rowsList.add(new ArrayList<String>(Arrays.asList(rowContent)));
		}

                return new Table(cols, colTypes, rowsList);
        }

	private static String decode(String s) throws UnsupportedEncodingException {
		// Include all other special encoding cases here
		s = s.replace("+", "%2B");
		s = s.replace("\r", "");

		String decoded = URLDecoder.decode(s, StandardCharsets.UTF_8.toString());
		return decoded;
	}

	public static JSONArray decipherValueList(ArrayList<?> valueList) throws Exception {
                JSONArray valueArray = new JSONArray();
                for (int i = 0; i < valueList.size(); i++) {
        	        if (valueList.get(i) instanceof String || valueList.get(i) instanceof Integer || valueList.get(i) instanceof Double)
               	        	valueArray.add(valueList.get(i));
                        else if (valueList.get(i) instanceof ArrayList<?>) 
				valueArray.add(decipherValueList((ArrayList<?>)valueList.get(i)));
			else
                                valueArray.add(getJsonFromMap((Map<String, Object>) valueList.get(i)));
                }
                return valueArray;
	}

        public static JSONObject getJsonFromMap(Map<String, Object> map) throws Exception {
                JSONObject jsonData = new JSONObject();
                for (String key : map.keySet()) {
                        Object value = map.get(key);
                        if (value instanceof Map<?, ?>) {
                                value = getJsonFromMap((Map<String, Object>) value);
                        }
                        else if (value instanceof ArrayList<?>) {
                                value = decipherValueList((ArrayList<?>) value);
                        }
                        jsonData.put(key, value);
                }
                return jsonData;
        }

}
