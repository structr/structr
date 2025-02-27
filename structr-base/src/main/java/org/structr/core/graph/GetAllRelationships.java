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
package org.structr.core.graph;

import org.structr.api.DatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;

import java.util.Collections;

/**
 * Fetches all the relationships in the database.
 *
 *
 */
public class GetAllRelationships extends NodeServiceCommand {

	public Iterable<RelationshipInterface> execute() throws FrameworkException {

		RelationshipFactory relationshipFactory = new RelationshipFactory(securityContext);
		DatabaseService graphDb                 = (DatabaseService)arguments.get("graphDb");

		if (graphDb != null) {

			return relationshipFactory.bulkInstantiate(graphDb.getAllRelationships());
		}

		return Collections.emptyList();
	}
}
