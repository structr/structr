package org.structr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
*
* @author Dennis Laske
*/
public class JsonMultiInput implements IJsonInput {

	private List<JsonInput> jsonInputs = new ArrayList<JsonInput>();
	
	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public boolean isMulti() {
		return true;
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
