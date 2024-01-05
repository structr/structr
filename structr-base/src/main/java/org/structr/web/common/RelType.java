/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.common;

import org.structr.api.graph.RelationshipType;

/**
 * Defines relationship types for structr's internal relationships.
 *
 *
 */
public enum RelType implements RelationshipType {

	// web
	CONTAINS,
	CONTAINS_NEXT_SIBLING,
	DEFINES_TYPE,
	DEFINES_PROPERTY,
	PAGE,
	ROOT,
	SYNC,
	LINK,

	// picture
	THUMBNAIL,
	PICTURE_OF,

	// blog
	AUTHOR,
	COMMENT,

	TAG,

	// files
	HOME_DIR,
	WORKING_DIR

}
