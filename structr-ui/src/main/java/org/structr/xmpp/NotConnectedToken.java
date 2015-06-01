package org.structr.xmpp;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.structr.common.error.SemanticErrorToken;

/**
 *
 * @author Christian Morgner
 */
public class NotConnectedToken extends SemanticErrorToken {

	public NotConnectedToken() {
		super(base);
	}

	@Override
	public JsonElement getContent() {
		return new JsonPrimitive(getErrorToken());
	}

	@Override
	public String getErrorToken() {
		return "not_connected";
	}
}
