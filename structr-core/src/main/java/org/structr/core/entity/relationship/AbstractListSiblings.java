package org.structr.core.entity.relationship;

import org.structr.core.entity.LinkedListNode;
import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractListSiblings<S extends LinkedListNode, T extends LinkedListNode> extends OneToOne<S, T> {

	@Override
	public String name() {
		return "CONTAINS_NEXT_SIBLING";
	}
}
