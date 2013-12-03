package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class RangeToken extends SemanticErrorToken {

	private String rangeDefinition = null;
	
	public RangeToken(final PropertyKey key, final String rangeDefinition) {
		super(key);
		
		this.rangeDefinition = rangeDefinition;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

                obj.add(getErrorToken(), new JsonPrimitive(rangeDefinition));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_be_in_range";
	}
}
