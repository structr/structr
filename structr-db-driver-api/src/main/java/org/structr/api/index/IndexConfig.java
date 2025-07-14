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
package org.structr.api.index;

/**
 * Combines entity type and create / drop flag for database index configurations.
 */
public abstract class IndexConfig {

	protected final boolean createOrDropIndex;
	protected final boolean isNodeIndex;
	protected final boolean isFulltextIndex;
	protected final boolean isTextIndex;

	protected IndexConfig(final boolean createOrDropIndex, final boolean isNodeIndex) {
		this(createOrDropIndex, isNodeIndex, false, false);
	}

	protected IndexConfig(final boolean createOrDropIndex, final boolean isNodeIndex, final boolean  isTextIndex, final boolean isFulltextIndex) {

		this.createOrDropIndex = createOrDropIndex;
		this.isNodeIndex       = isNodeIndex;
		this.isTextIndex       = isTextIndex;
		this.isFulltextIndex   = isFulltextIndex;
	}

	public boolean createOrDropIndex() {
		return this.createOrDropIndex;
	}

	public boolean isNodeIndex() {
		return this.isNodeIndex;
	}

	public boolean isTextIndex() {
		return this.isTextIndex;
	}

	public boolean isFulltextIndex() {
		return this.isFulltextIndex;
	}

	public String getIndexDescriptionForStatement(final String typeName) {

		if (this.isNodeIndex || this.isFulltextIndex) {

			return "(n:" + typeName + ")";
		}

		return "()-[n:" + typeName + "]-()";
	}
}
