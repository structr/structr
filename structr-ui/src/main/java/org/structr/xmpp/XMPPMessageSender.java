package org.structr.xmpp;

import org.structr.core.entity.ManyToOne;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public class XMPPMessageSender extends ManyToOne<IncomingXMPPMessage, XMPPContact> {

	@Override
	public Class<IncomingXMPPMessage> getSourceType() {
		return IncomingXMPPMessage.class;
	}

	@Override
	public Class<XMPPContact> getTargetType() {
		return XMPPContact.class;
	}

	@Override
	public String name() {
		return "SENT_FROM";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.TARGET_TO_SOURCE;
	}
}
