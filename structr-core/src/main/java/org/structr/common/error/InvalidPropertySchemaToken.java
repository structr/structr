package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 *
 * @author Christian Morgner
 */
public class InvalidPropertySchemaToken extends SemanticErrorToken {

	private String source = null;
	
	public InvalidPropertySchemaToken(final String source) {
		
		super(base);
		
		this.source = source;
	}

	@Override
	public JsonElement getContent() {
		return new JsonPrimitive(getErrorToken());
	}

	@Override
	public String getErrorToken() {
		return "invalid_property_schema";
	}
}
