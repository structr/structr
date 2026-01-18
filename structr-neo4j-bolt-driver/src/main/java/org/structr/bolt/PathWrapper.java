/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.bolt;

import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Relationship;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 *
 */
class PathWrapper implements Path {

	private org.neo4j.driver.types.Path path = null;
	private BoltDatabaseService db           = null;

	public PathWrapper(final BoltDatabaseService db, final org.neo4j.driver.types.Path path) {

		this.path = path;
		this.db   = db;
	}

	@Override
	public Iterator<PropertyContainer> iterator() {

		if (path.length() > 0) {

			return new SegmentIterator(path);
		}

		final SessionTransaction tx        = db.getCurrentTransaction();
		final List<PropertyContainer> list = new LinkedList<>();

		list.add(tx.getNodeWrapper(path.start()));

		return list.iterator();
	}

	// ----- nested classes -----
	private class SegmentIterator implements Iterator<PropertyContainer> {

		private SessionTransaction tx = null;
		private Iterator<Segment> it  = null;
		private Segment current       = null;
		private int state             = 0;

		public SegmentIterator(final org.neo4j.driver.types.Path path) {

			this.it = path.iterator();
			this.tx = db.getCurrentTransaction();
		}

		@Override
		public boolean hasNext() {
			return state < 3;
		}

		@Override
		public PropertyContainer next() {

			if (current == null) {

				current = it.next();
			}

			switch (state) {

				case 0:
					state = 1;
					return tx.getNodeWrapper(current.start());

				case 1:
					final Relationship rel = current.relationship();
					if (it.hasNext()) {

						state = 0;
						current = null;

					} else {

						state = 2;
					}

					return tx.getRelationshipWrapper(rel);

				case 2:
					state = 3;
					return tx.getNodeWrapper(current.end());
			}

			throw new NotFoundException("No such element.");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Removal not supported.");
		}
	}

}
