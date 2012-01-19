/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
package org.structr.common;

import org.neo4j.graphdb.RelationshipType;

/**
 * Contains all application-specific relationship types
 * 
 * @author amorgner
 *
 */
public enum RelType implements RelationshipType {
	HAS_CHILD, // IS_CHILD,
	IS_MEMBER_OF_GROUP,
	UNDEFINED, LINK, PAGE_LINK,
	SECURITY,
        USE_TEMPLATE,
        OWNS,
        ROOT_NODE,
        THUMBNAIL,
	NEXT_LIST_ENTRY,
	LAST_LIST_ENTRY,

	// application relationships
	DATA,
	SUBMIT,
	ERROR_DESTINATION,
	SUCCESS_DESTINATION,
	CREATE_DESTINATION,
        
        // type relationships
        TYPE,
        SUBTYPE,

	// web
	CONTAINS
}
