/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.traversal;

import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;
import org.structr.core.notion.Notion;

/**
 * Defines the information necessary to traverse the graph and collect nodes
 * according to a set of predicates.
 * 
 *
 */
public interface TraverserInterface<T> {
	
	public TraversalDescription getTraversalDescription(SecurityContext securityContext);
	public List transformResult(List<T> traversalResult);
	public void addPredicate(Predicate<Node> predicate);
	public Class getResultType();
	public Comparator<T> getComparator();
	public Notion getNotion();
	public void cleanup();
	public boolean collapseSingleResult();
}
