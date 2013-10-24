/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.entity;

import org.structr.core.entity.ValidatedNode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Endpoints;
import org.structr.web.common.RelType;
import org.structr.web.property.UiNotion;

/**
 *
 * @author Christian Morgner
 */
public class Tag extends ValidatedNode {

	public static final Endpoints<Taggable> taggables = new Endpoints<>("taggables", Taggable.class, RelType.TAG, new UiNotion(), false);
	
	public static final View defaultView = new View(Tag.class, PropertyView.Public, name, taggables);
	public static final View uiView      = new View(Tag.class, PropertyView.Ui, name, taggables);
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean valid = true;
		
		valid &= nonEmpty(AbstractNode.name, errorBuffer);
		
		return valid;
	}
	
	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		// Make tags visible to anyone upon creation
		setProperty(visibleToPublicUsers, true);
		setProperty(visibleToAuthenticatedUsers, true);
		
		return super.onModification(securityContext, errorBuffer);
	}


}
