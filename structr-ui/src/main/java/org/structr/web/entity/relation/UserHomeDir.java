package org.structr.web.entity.relation;

import org.structr.core.entity.OneToOne;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class UserHomeDir extends OneToOne<User, Folder> {

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
		return "HOME_DIR";
	}
}
