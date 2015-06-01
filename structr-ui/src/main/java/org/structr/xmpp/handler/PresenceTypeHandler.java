package org.structr.xmpp.handler;

import org.jivesoftware.smack.packet.Presence;
import org.structr.xmpp.XMPPContext.StructrXMPPConnection;

/**
 *
 * @author Christian Morgner
 */
public class PresenceTypeHandler implements TypeHandler<Presence> {

	@Override
	public void handle(final StructrXMPPConnection connection, final Presence presence) {
		// not implemented yet..
	}
}
