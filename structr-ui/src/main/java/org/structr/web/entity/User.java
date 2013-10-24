/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.structr.common.KeyAndClass;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.Forward;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.SimpleRegexValidator;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.web.common.RelType;
import org.structr.web.property.ImageDataProperty;


//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public class User extends AbstractUser {
	
	private static final Logger logger = Logger.getLogger(User.class.getName());

	public static final Property<String>		confirmationKey		= new StringProperty("confirmationKey").indexed();
	public static final Property<Boolean>		backendUser		= new BooleanProperty("backendUser").indexed();
	public static final Property<Boolean>		frontendUser		= new BooleanProperty("frontendUser").indexed();
	public static final Property<Image>		userImg			= new EntityProperty<>("userImg", Image.class, RelType.PICTURE_OF, Direction.INCOMING, false);
	public static final ImageDataProperty		userImageData		= new ImageDataProperty("userImageData", new KeyAndClass(userImg, Image.class));
	public static final Property<Folder>		homeDirectory		= new EntityProperty<>("homeDirecory", Folder.class, RelType.HOME_DIR, Direction.OUTGOING, false);
	public static final Property<Folder>		workingDirectory	= new EntityProperty<>("workingDirectory", Folder.class, RelType.WORKING_DIR, Direction.OUTGOING, false);
	
	public static final CollectionProperty<Group> groups          = new CollectionProperty("groups", Group.class, RelType.CONTAINS, Direction.INCOMING, new PropertyNotion(uuid), false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, eMail, isAdmin, password, blocked, sessionId, confirmationKey, backendUser, frontendUser, groups, userImg, homeDirectory
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name
	);
	
	static {
		
		User.eMail.addValidator(new TypeUniquenessValidator(User.class));
		User.name.addValidator(new TypeUniquenessValidator(User.class));
		User.eMail.addValidator(new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"));
	}
	
}
