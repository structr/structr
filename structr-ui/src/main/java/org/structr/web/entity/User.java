/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.util.List;
import org.structr.common.KeyAndClass;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Group;
import org.structr.core.entity.relationship.Groups;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.LowercaseTypeUniquenessValidator;
import org.structr.core.validator.SimpleNonEmptyValueValidator;
import org.structr.core.validator.SimpleRegexValidator;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.UserHomeDir;
import org.structr.web.entity.relation.UserImage;
import org.structr.web.entity.relation.UserWorkDir;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.UiNotion;


//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public class User extends AbstractUser {

	public static final Property<String>      confirmationKey  = new StringProperty("confirmationKey").indexed();
	public static final Property<Boolean>     backendUser      = new BooleanProperty("backendUser").indexed();
	public static final Property<Boolean>     frontendUser     = new BooleanProperty("frontendUser").indexed();
	public static final Property<Image>       img              = new StartNode<>("img", UserImage.class);
	public static final ImageDataProperty     imageData        = new ImageDataProperty("imageData", new KeyAndClass(img, Image.class));
	public static final Property<Folder>      homeDirectory    = new EndNode<>("homeDirectory", UserHomeDir.class);
	public static final Property<Folder>      workingDirectory = new EndNode<>("workingDirectory", UserWorkDir.class);
	public static final Property<List<Group>> groups           = new StartNodes<>("groups", Groups.class, new UiNotion());
	public static final Property<Boolean>     isUser           = new BooleanProperty("isUser").defaultValue(true).readOnly();
	public static final Property<String>      eMail            = new StringProperty("eMail").indexedCaseInsensitive();
	public static final Property<String>      twitterName      = new StringProperty("twitterName").indexed();
	public static final Property<String>      localStorage     = new StringProperty("localStorage");

	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, eMail, isAdmin, password, blocked, sessionIds, confirmationKey, backendUser, frontendUser, groups, img, homeDirectory, isUser
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, isUser
	);

	static {

	// register this type as an overridden builtin type
		SchemaService.registerBuiltinTypeOverride("User", User.class.getName());

		User.eMail.addValidator(new LowercaseTypeUniquenessValidator(User.class));
		User.name.addValidator(new SimpleNonEmptyValueValidator(User.class));
		User.name.addValidator(new TypeUniquenessValidator(User.class));
		User.eMail.addValidator(new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"));
	}

	@Override
	public boolean canHaveOwner() {
		return false;
	}
}
