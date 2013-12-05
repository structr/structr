package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *
 * @author Christian Morgner
 */
public class InvalidPropertySchemaToken extends SemanticErrorToken {

	private String source     = null;
	private String reason     = null;
	private String errorToken = null;
	
	public InvalidPropertySchemaToken(final String source, final String errorToken, final String reason) {
		
		super(base);
		
		this.source     = source;
		this.reason     = reason;
		this.errorToken = errorToken;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

                obj.add(getErrorToken(), new JsonPrimitive(source));
                obj.add("reason", new JsonPrimitive(reason));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return errorToken;
	}
}
