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
package org.structr.embedded;

import org.neo4j.graphdb.Entity;
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

	private org.neo4j.graphdb.Path path = null;
	private EmbeddedDatabaseService db  = null;

	public PathWrapper(final EmbeddedDatabaseService db, final org.neo4j.graphdb.Path path) {

		this.path = path;
		this.db   = db;
	}

	@Override
	public Iterator<PropertyContainer> iterator() {

		System.out.println(path.length() + ": " + path);

		if (path.length() > 0) {

			return new SegmentIterator(path);
		}

		final EmbeddedTransaction     tx   = db.getCurrentTransaction();
		final List<PropertyContainer> list = new LinkedList<>();

		list.add(tx.getNodeWrapper(path.startNode()));

		return list.iterator();
	}

	// ----- nested classes -----
	private class SegmentIterator implements Iterator<PropertyContainer> {

		private EmbeddedTransaction tx = null;
		private Iterator<Entity>    it = null;

		public SegmentIterator(final org.neo4j.graphdb.Path path) {

			this.it = path.iterator();
			this.tx = db.getCurrentTransaction();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public PropertyContainer next() {

			final Entity next = it.next();

			if (next instanceof org.neo4j.graphdb.Node node) {
				return tx.getNodeWrapper(node);
			}

			if (next instanceof org.neo4j.graphdb.Relationship relationship) {
				return tx.getRelationshipWrapper(relationship);
			}

			throw new NotFoundException("No such element.");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Removal not supported.");
		}
	}

}
