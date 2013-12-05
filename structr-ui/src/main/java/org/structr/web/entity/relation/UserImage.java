package org.structr.web.entity.relation;

import org.structr.core.entity.OneToOne;
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
	public String name() {
		return "PICTURE_OF";
	}
}
