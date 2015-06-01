package org.structr.xmpp.handler;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.structr.xmpp.XMPPContext.StructrXMPPConnection;

/**
 *
 * @author Christian Morgner
 */
public class RosterPacketTypeHandler implements TypeHandler<RosterPacket> {

	@Override
	public void handle(final StructrXMPPConnection connection, final RosterPacket roster) {
		// not implemented yet...
	}
}
