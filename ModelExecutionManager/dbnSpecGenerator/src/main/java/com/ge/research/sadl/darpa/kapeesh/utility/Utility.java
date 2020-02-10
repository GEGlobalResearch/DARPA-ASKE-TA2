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
package com.ge.research.sadl.darpa.kapeesh.utility;

import java.io.UnsupportedEncodingException; 
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

	public static Table createTable(JSONArray array) throws Exception {
		
		Set<String> colsSet = new HashSet<String>();
		for (Object o : array) {
			JSONObject jsonObject = (JSONObject) o;
			colsSet = jsonObject.keySet();
		}
		String[] cols = colsSet.toArray(new String[colsSet.size()]);
		String[] colTypes = new String[cols.length];
		Arrays.fill(colTypes, "string");

		ArrayList<ArrayList<String> > rowsList = new ArrayList<ArrayList<String> >();
		for (Object o : array) {
			JSONObject jsonObject = (JSONObject) o;
			if (jsonObject instanceof JSONObject) {
				ArrayList<String> row = new ArrayList<String>();
				for(Iterator iterator = jsonObject.keySet().iterator(); iterator.hasNext();) {
    					String key = (String) iterator.next();
					row.add((String)jsonObject.get(key));
				}
				rowsList.add(row);
			}
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

	public static JSONArray expandBindings(JSONArray bindings, JSONArray cols) throws Exception {
                JSONArray rows = new JSONArray();

                for (Object o : bindings) {
                        JSONObject dstRow = new JSONObject();
                        for (Object col : cols) {
                                dstRow.put((String) col, "");
                        }

                        JSONObject srcRow = (JSONObject) o;
                        Set<String> colsThisRow = srcRow.keySet();
                        for(Iterator iterator = colsThisRow.iterator(); iterator.hasNext();) {
                                String key = (String) iterator.next();
                                dstRow.put(key, (String) ((JSONObject) srcRow.get(key)).get("value"));
                        }
                        rows.add(dstRow);
                }

		return rows;
	}
}
