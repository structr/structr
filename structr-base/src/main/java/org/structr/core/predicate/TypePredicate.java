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
package org.structr.core.predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.core.entity.AbstractNode;

/**
 * A node predicate that evaluates to <b>true</b> if the type of the given node
 * matches the given type.
 *
 *
 */
public class TypePredicate implements Predicate<Node> {

	private static final Logger logger = LoggerFactory.getLogger(TypePredicate.class.getName());
	private String type = null;

	public TypePredicate(String type) {
		this.type = type;
	}

	@Override
	public boolean accept(Node node) {

		if(node.hasProperty(AbstractNode.type.dbName())) {

			String value = (String)node.getProperty(AbstractNode.type.dbName());

			logger.debug("Type property: {}, expected {}", new Object[] { value, type } );

			return type.equals(value);

		} else {

			logger.warn("Node has no type property.");
		}

		return false;
	}
}
