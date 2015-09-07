package org.structr.xmpp;

import org.structr.common.error.SemanticErrorToken;

/**
 *
 * @author Christian Morgner
 */
public class NotConnectedToken extends SemanticErrorToken {

	public NotConnectedToken(final String type) {
		super(type, null, "not_connected");
	}
}
