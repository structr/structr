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
	LAST_LIST_ENTRY
}
