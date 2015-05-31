package org.structr.xmpp;

import org.structr.core.entity.ManyToOne;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public class XMPPMessageReceiver extends ManyToOne<OutgoingXMPPMessage, XMPPContact> {

	@Override
	public Class<OutgoingXMPPMessage> getSourceType() {
		return OutgoingXMPPMessage.class;
	}

	@Override
	public Class<XMPPContact> getTargetType() {
		return XMPPContact.class;
	}

	@Override
	public String name() {
		return "SENT_TO";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.TARGET_TO_SOURCE;
	}
}
