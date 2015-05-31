package org.structr.xmpp;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public class XMPPClientOutMessage extends OneToMany<XMPPClient, OutgoingXMPPMessage> {

	@Override
	public Class<XMPPClient> getSourceType() {
		return XMPPClient.class;
	}

	@Override
	public Class<OutgoingXMPPMessage> getTargetType() {
		return OutgoingXMPPMessage.class;
	}

	@Override
	public String name() {
		return "SENT";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
