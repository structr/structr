package org.structr.core.traits;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.property.*;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.relationship.ImagePICTURE_OFUser;
import org.structr.web.entity.relationship.UserHOME_DIRFolder;
import org.structr.web.entity.relationship.UserWORKING_DIRFolder;

public class UserImpl extends PrincipalTrait implements User {

	static {

		final Trait trait = Trait.create(User.class, n -> new UserImpl(n));

		trait.registerProperty(new EndNode<>("homeDirectory", UserHOME_DIRFolder.class).partOfBuiltInSchema());
		trait.registerProperty(new EndNode<>("workingDirectory", UserWORKING_DIRFolder.class).partOfBuiltInSchema());
		trait.registerProperty(new StartNode<>("img", ImagePICTURE_OFUser.class));
		trait.registerProperty(new StringProperty("confirmationKey").indexed().partOfBuiltInSchema());
		trait.registerProperty(new StringProperty("localStorage").partOfBuiltInSchema());
		trait.registerProperty(new BooleanProperty("skipSecurityRelationships").defaultValue(false).indexed().partOfBuiltInSchema());
		trait.registerProperty(new ConstantBooleanProperty("isUser", true).partOfBuiltInSchema());

		Traits.registerNodeType("User",
			Trait.of(User.class),
			Trait.of(Principal.class)
		);
	}

	public UserImpl(final PropertyContainer obj) {
		super(obj);
	}

	@Override
	public Folder getHomeDirectory() {
		return getProperty(traits.key("homeDirectory"));
	}

	@Override
	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		setProperty(traits.key("workingDirectory"), workDir);
	}

	@Override
	public Folder getWorkingDirectory() {
		return getProperty(traits.key("workingDirectory"));
	}

	@Override
	public void setLocalStorage(final String localStorage) throws FrameworkException {
		setProperty(traits.key("localStorage"), localStorage);
	}

	@Override
	public String getLocalStorage() {
		return getProperty(traits.key("localStorage"));
	}
}
