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
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

import java.util.Map;

/**
 *
 *
 */
public class UpdateOperation extends InvertibleModificationOperation {

	private DOMNode existingNode = null;
	private DOMNode newNode      = null;

	public UpdateOperation(final Map<String, DOMNode> hashMappedExistingNodes, final DOMNode existingNode, final DOMNode newNode) {

		super(hashMappedExistingNodes);

		this.existingNode = existingNode;
		this.newNode      = newNode;
	}

	@Override
	public String toString() {

		if (existingNode.is("Content")) {

			return "Update Content(" + existingNode.getIdHashOrProperty() + ") with " + newNode.getIdHashOrProperty();

		} else if (existingNode.is("DOMElement")) {

			return "Update " + ((DOMElement)newNode).getTag() + "(" + existingNode.getIdHashOrProperty() + ") with " + newNode.getIdHashOrProperty();
		}

		return "Update " + existingNode.getUuid() + "(" + existingNode.getIdHash();
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {
		existingNode.updateFromNode(newNode);
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {

		// update operations should go last
		return 400;
	}
}
