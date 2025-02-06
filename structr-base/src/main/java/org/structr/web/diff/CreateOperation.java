/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class CreateOperation extends InvertibleModificationOperation {

	private List<String> siblingHashes = null;
	private String parentHash          = null;
	private DOMNode newNode            = null;
	private int depth                  = 0;

	public CreateOperation(final Map<String, DOMNode> hashMappedExistingNodes, final String parentHash, final List<String> siblingHashes, final DOMNode newNode, final int depth) {

		super(hashMappedExistingNodes);

		this.siblingHashes = siblingHashes;
		this.parentHash  = parentHash;
		this.newNode     = newNode;
		this.depth       = depth;
	}

	@Override
	public String toString() {

		if (newNode.is("Content")) {

			return "Create Content(" + newNode.getIdHashOrProperty() + ")";

		} else if (newNode.is("DOMElement")) {

			return "Create " + newNode.as(DOMElement.class).getTag() + "(" + newNode.getIdHashOrProperty() + ")";
		}

		return "Create " + newNode.getUuid() + "(" + newNode.getIdHashOrProperty() + ")";
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {

		final InsertPosition insertPosition = findInsertPosition(sourcePage, parentHash, siblingHashes, newNode);
		if (insertPosition != null) {

			final DOMNode parent  = insertPosition.getParent();
			final DOMNode sibling = insertPosition.getSibling();

			if (parent != null && !parent.isSynced()) {

				if (sourcePage != null) {
					sourcePage.adoptNode(newNode);
				}

				parent.insertBefore(newNode, sibling);

				// make existing node known to other operations
				hashMappedExistingNodes.put(newNode.getIdHashOrProperty(), newNode);

				// remove children of new node so that existing nodes can be moved later
				for (final RelationshipInterface childRel : newNode.getChildRelationships()) {
					app.delete(childRel);
				}
			}
		}
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {

		// create operations should go after Delete but before Move
		return 200 + depth;
	}
}
