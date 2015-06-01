package org.structr.xmpp;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public class XMPPClientRequest extends OneToMany<XMPPClient, XMPPRequest> {

	@Override
	public Class<XMPPClient> getSourceType() {
		return XMPPClient.class;
	}

	@Override
	public Class<XMPPRequest> getTargetType() {
		return XMPPRequest.class;
	}

	@Override
	public String name() {
		return "PENDING_REQUEST";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
