/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.node;

import java.util.Iterator;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class FilterSourceCollector implements Iterable<AbstractNode> {

	private Iterator<AbstractNode> iterator = null;
	private RelationshipType relType = null;
	private String nodeType = null;

	public FilterSourceCollector(AbstractNode root, RelationshipType relType, String nodeType) {

		this.iterator = root.getDirectChildren(relType, nodeType).iterator();
		this.relType = relType;
		this.nodeType = nodeType;
	}

	@Override
	public Iterator<AbstractNode> iterator() {

		return(new Iterator<AbstractNode>() {

			private Iterator<AbstractNode> currentIterator = null;

			@Override
			public boolean hasNext() {

				if(currentIterator != null) {

					if(currentIterator.hasNext()) {

						return(true);

					} else
					{
						if(iterator.hasNext()) {

							currentIterator = iterator.next().getFilterSource(relType, nodeType).iterator();
							return(true);
						}

					}

				} else {

					if(iterator.hasNext()) {

						currentIterator = iterator.next().getFilterSource(relType, nodeType).iterator();
						return(true);
					}
				}

				return(false);
			}

			@Override
			public AbstractNode next() {

				if(currentIterator != null) {

					return(currentIterator.next());
				}

				return(null);
			}

			@Override
			public void remove() {

				if(currentIterator != null) {

					currentIterator.remove();
				}
			}

		});
	}
}
