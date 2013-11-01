package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.web.common.RelType;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class UserWorkDir extends OneToOne<User, Folder> {

	@Override
	public Class<User> getSourceType() {
		return User.class;
	}

	@Override
	public Class<Folder> getTargetType() {
		return Folder.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.WORKING_DIR;
	}
}
