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
package org.structr.neo4j.wrapper;

import org.structr.api.graph.Label;

/**
 *
 */
public class LabelWrapper implements Label {

	private org.neo4j.graphdb.Label label = null;

	public LabelWrapper(org.neo4j.graphdb.Label label) {
		this.label = label;
	}

	@Override
	public String name() {
		return label.name();
	}

	@Override
	public int hashCode() {
		return label.name().hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Label) {
			return other.hashCode() == hashCode();
		}

		return false;
	}

	// ----- helper methods -----
	public org.neo4j.graphdb.Label unwrap() {
		return label;
	}
}
