package org.structr.xmpp.handler;

import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.structr.xmpp.XMPPContext.StructrXMPPConnection;

/**
 *
 * @author Christian Morgner
 */
public class EmptyResultIQTypeHandler implements TypeHandler<EmptyResultIQ> {

	@Override
	public void handle(final StructrXMPPConnection connection, final EmptyResultIQ result) {
		// not implemented yet...
	}
}
