/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.api.util;

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;

/**
 *
 */
public class NodeWithOwnerResult {

	private Relationship securityRelationship = null;
	private Relationship ownsRelationship     = null;
	private Node newNode                      = null;

	public NodeWithOwnerResult(final Node newNode, final Relationship securityRelationship, final Relationship ownsRelationship) {

		this.securityRelationship = securityRelationship;
		this.ownsRelationship     = ownsRelationship;
		this.newNode              = newNode;
	}

	public Relationship getSecurityRelationship() {
		return securityRelationship;
	}

	public Relationship getOwnsRelationship() {
		return ownsRelationship;
	}

	public Node getNewNode() {
		return newNode;
	}
}
