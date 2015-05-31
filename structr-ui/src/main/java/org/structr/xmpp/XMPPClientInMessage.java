package org.structr.xmpp;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public class XMPPClientInMessage extends OneToMany<XMPPClient, IncomingXMPPMessage> {

	@Override
	public Class<XMPPClient> getSourceType() {
		return XMPPClient.class;
	}

	@Override
	public Class<IncomingXMPPMessage> getTargetType() {
		return IncomingXMPPMessage.class;
	}

	@Override
	public String name() {
		return "RECEIVED";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
