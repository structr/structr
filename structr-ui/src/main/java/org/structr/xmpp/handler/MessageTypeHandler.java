package org.structr.xmpp.handler;

import org.jivesoftware.smack.packet.Message;
import org.structr.xmpp.XMPPClient;
import org.structr.xmpp.XMPPContext.StructrXMPPConnection;

/**
 *
 * @author Christian Morgner
 */
public class MessageTypeHandler implements TypeHandler<Message> {

	@Override
	public void handle(final StructrXMPPConnection connection, final Message packet) {

		if (packet.getBody() != null) {

			// let client callback handle the message
			XMPPClient.onMessage(connection.getUuid(), packet);
		}
	}
}
