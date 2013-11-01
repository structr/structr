package org.structr.web.entity.blog.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;
import org.structr.web.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public class BlogAuthor extends OneToMany<Principal, BlogComment> {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public Class<BlogComment> getTargetType() {
		return BlogComment.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.AUTHOR;
	}
}
