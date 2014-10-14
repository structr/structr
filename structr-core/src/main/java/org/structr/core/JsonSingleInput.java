package org.structr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
*
* @author Dennis Laske
*/
public class JsonSingleInput implements IJsonInput {

	private List<JsonInput> jsonInputs = new ArrayList<JsonInput>();
	
	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean isMulti() {
		return false;
	}

	@Override
	public void add(JsonInput jsonInput) {
		jsonInputs.add(jsonInput);
	}

	@Override
	public List<JsonInput> getJsonInputs() {
		return jsonInputs;
	}

}
