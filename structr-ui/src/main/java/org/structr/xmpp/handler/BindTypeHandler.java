package org.structr.xmpp.handler;

import org.jivesoftware.smack.packet.Bind;
import org.structr.xmpp.XMPPContext.StructrXMPPConnection;

/**
 *
 * @author Christian Morgner
 */
public class BindTypeHandler implements TypeHandler<Bind> {

	@Override
	public void handle(final StructrXMPPConnection connection, final Bind bind) {

		connection.setJID(bind.getJid());
		connection.setResource(bind.getResource());
	}
}
