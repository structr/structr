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

import java.util.LinkedList;
import java.util.List;

import org.neo4j.graphdb.Direction;

import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.validator.SimpleRegexValidator;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.core.notion.PropertyNotion;

import org.structr.common.KeyAndClass;
import org.structr.common.PropertyView;

import org.structr.web.common.RelType;
import org.structr.web.property.ImageDataProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public class User extends Person implements Principal {

	public static final Property<String>          confirmationKey = new StringProperty("confirmationKey");
	public static final Property<Boolean>         backendUser     = new BooleanProperty("backendUser");
	public static final Property<Boolean>         frontendUser    = new BooleanProperty("frontendUser");
	public static final Property<Image>           img             = new EntityProperty("img", Image.class, RelType.PICTURE_OF, Direction.INCOMING, false);
	public static final ImageDataProperty         imageData       = new ImageDataProperty("imageData", new KeyAndClass(img, Image.class));
	
	public static final CollectionProperty<Group> groups          = new CollectionProperty<Group>("groups", Group.class, RelType.CONTAINS, Direction.INCOMING, new PropertyNotion(uuid), false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, email, password, blocked, sessionId, confirmationKey, backendUser, frontendUser, groups, img
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name
	);
	
	static {

		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.user.name(),     name, email);
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.keyword.name(),  uiView.properties());
		
		User.email.addValidator(new TypeUniquenessValidator(User.class));
		User.email.addValidator(new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"));
	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public List<Principal> getParents() {

		List<Principal> parents                   = new LinkedList<Principal>();
		Iterable<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getStartNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (User.password.equals(key)) {

			return "";
			
		}

		return super.getPropertyForIndexing(key);

	}

	/**
	 * Intentionally return null.
	 * @return
	 */
	@Override
	public <T> T getProperty(final PropertyKey<T> key) {

		if (User.password.equals(key)) {
			
			return null;
			
		} else {
			
			return super.getProperty(key);
			
		}

	}

}
