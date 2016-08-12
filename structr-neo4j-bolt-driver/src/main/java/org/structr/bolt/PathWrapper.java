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
package org.structr.bolt;

import java.util.Iterator;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;


/**
 *
 * @author Christian Morgner
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

		return new Iterator<PropertyContainer>() {

			@Override
			public boolean hasNext() {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public PropertyContainer next() {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
		};
	}
}
