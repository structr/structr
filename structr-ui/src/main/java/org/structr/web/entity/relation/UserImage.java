package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.web.common.RelType;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class UserImage extends OneToOne<Image, User> {

	@Override
	public Class<User> getTargetType() {
		return User.class;
	}

	@Override
	public Class<Image> getSourceType() {
		return Image.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.PICTURE_OF;
	}
}
