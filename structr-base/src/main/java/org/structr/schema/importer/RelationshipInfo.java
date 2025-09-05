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
package org.structr.schema.importer;

/**
 *
 *
 */
public class RelationshipInfo {

	private String startNodeType = null;
	private String endNodeType   = null;
	private String relType       = null;

	public RelationshipInfo(final String startNodeType, final String endNodeType, final String relType) {
		this.startNodeType = startNodeType;
		this.endNodeType   = endNodeType;
		this.relType       = relType;
	}

	public String getStartNodeType() {
		return startNodeType;
	}

	public String getEndNodeType() {
		return endNodeType;
	}

	public String getRelType() {
		return relType;
	}

	@Override
	public int hashCode() {
		return startNodeType.concat(relType).concat(endNodeType).hashCode();
	}

	@Override
	public boolean equals(final Object o) {

		if (o instanceof RelationshipInfo) {
			return ((RelationshipInfo)o).hashCode() == hashCode();
		}

		return false;
	}
	
	@Override
	public String toString() {
		
		return startNodeType + "-[:" + relType + "]->" + endNodeType;
	}
}
