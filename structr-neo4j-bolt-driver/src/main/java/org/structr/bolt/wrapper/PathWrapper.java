/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.wrapper;

import java.util.Iterator;
import org.neo4j.driver.v1.types.Path.Segment;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.bolt.BoltDatabaseService;


/**
 *
 */
public class PathWrapper implements Path {

	private org.neo4j.driver.v1.types.Path path = null;
	private BoltDatabaseService db              = null;

	public PathWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Path path) {

		this.path = path;
		this.db   = db;
	}

	@Override
	public Iterator<PropertyContainer> iterator() {

		final Iterator<Segment> it = path.iterator();

		return new Iterator<PropertyContainer>() {

			private Segment current = null;
			private int state       = 0;

			@Override
			public boolean hasNext() {
				return it.hasNext() || state < 2;
			}

			@Override
			public PropertyContainer next() {

				if (current == null) {

					// first step, current is uninitialized
					current = it.next();

				} else if (state == 2) {

					// any other step, skip start
					current = it.next();
					state   = 1;
				}

				switch (state) {

					case 0:
						state = 1;
						return NodeWrapper.newInstance(db, current.start());

					case 1:
						state = 2;
						return RelationshipWrapper.newInstance(db, current.relationship());

					case 2:
						state = 0;
						return NodeWrapper.newInstance(db, current.end());
				}

				throw new NotFoundException("No such element.");
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Removal not supported.");
			}
		};
	}
}
