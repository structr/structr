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

public class NewIndexConfig {

	private final String propertyKey;
	private final String type;
	private final boolean isNodeIndex;
	private final boolean isTextIndex;
	private final boolean isFulltextIndex;

	public NewIndexConfig(final String type, final String propertyKey, final boolean isNodeIndex, final boolean isTextIndex, final boolean isFulltextIndex) {

		this.isNodeIndex     = isNodeIndex;
		this.isTextIndex     = isTextIndex;
		this.isFulltextIndex = isFulltextIndex;
		this.propertyKey     = propertyKey;
		this.type            = type;
	}

	public boolean isFulltextIndex() {
		return isFulltextIndex;
	}

	public boolean isTextIndex() {
		return isTextIndex;
	}

	public String getType() {
		return type;
	}

	public String getPropertyKey() {
		return propertyKey;
	}

	public String getIndexDescriptionForStatement() {

		if (this.isNodeIndex) {

			return "(n:" + type + ")";
		}

		return "()-[n:" + type + "]-()";
	}

}
