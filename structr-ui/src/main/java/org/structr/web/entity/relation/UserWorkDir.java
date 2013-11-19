package org.structr.web.entity.relation;

import org.structr.core.entity.OneToOne;
import org.structr.core.property.Property;
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
	public String name() {
		return "WORKING_DIR";
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}
}
