package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *
 * @author Christian Morgner
 */
public class InvalidSchemaToken extends SemanticErrorToken {

	private String source     = null;
	private String errorToken = null;
	
	public InvalidSchemaToken(final String source, final String errorToken) {
		
		super(base);
		
		this.source     = source;
		this.errorToken = errorToken;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

                obj.add(getErrorToken(), new JsonPrimitive(source));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return errorToken;
	}
}
