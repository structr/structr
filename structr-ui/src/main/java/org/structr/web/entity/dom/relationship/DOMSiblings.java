package org.structr.web.entity.dom.relationship;

import org.structr.core.entity.relationship.AbstractListSiblings;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 * @author Christian Morgner
 */
public class DOMSiblings extends AbstractListSiblings<DOMNode, DOMNode> {

	@Override
	public Class<DOMNode> getSourceType() {
		return DOMNode.class;
	}

	@Override
	public Class<DOMNode> getTargetType() {
		return DOMNode.class;
	}
}
