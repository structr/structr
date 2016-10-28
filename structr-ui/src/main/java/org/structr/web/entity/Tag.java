/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ValidatedNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.relation.Tagging;
import org.structr.web.property.UiNotion;

/**
 *
 *
 */
public class Tag extends ValidatedNode {

	public static final Property<List<Taggable>> taggables = new EndNodes<>("taggables", Tagging.class, new UiNotion());

	public static final View defaultView = new View(Tag.class, PropertyView.Public, name, taggables);
	public static final View uiView      = new View(Tag.class, PropertyView.Ui, name, taggables);

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= nonEmpty(AbstractNode.name, errorBuffer);

		return valid;
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyMap map = new PropertyMap();

		// Make tags visible to anyone upon creation
		map.put(visibleToPublicUsers, true);
		map.put(visibleToAuthenticatedUsers, true);

		setProperties(securityContext, map);

		return super.onCreation(securityContext, errorBuffer);
	}


}
