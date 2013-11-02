package org.structr.web.entity.relation;

import org.structr.core.entity.OneToOne;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class LinkRelationship<S extends NodeInterface, T extends NodeInterface> extends OneToOne<S, T> {

	@Override
	public String name() {
		return "LINK";
	}
}
