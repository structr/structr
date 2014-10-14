package org.structr.core;

import java.util.List;

/**
*
* @author Dennis Laske
*/
public interface IJsonInput {

	boolean isSingle();
	
	boolean isMulti();
	
	void add(JsonInput jsonInput);
	
	List<JsonInput> getJsonInputs();
	
}
