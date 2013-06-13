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


package org.structr.core.entity;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.property.PropertyMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * A generic node entity that will be instantiated when a node with an unknown
 * type is encountered.
 * 
 * @author Axel Morgner
 */
public class GenericNode extends AbstractNode {

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		
		List<PropertyKey> augmentedProperties = new LinkedList<PropertyKey>();
		String _type                          = getType();

		// add property keys from superclass
		for (PropertyKey key : super.getPropertyKeys(propertyView)) {
			augmentedProperties.add(key);
		}

		if (_type != null) {
			
			Iterable<PropertyDefinition> defs = PropertyDefinition.getPropertiesForKind(_type);
			if (defs != null) {

				for (PropertyDefinition propertyDefinition : defs) {
					augmentedProperties.add(propertyDefinition);
				}
			}
		}

		return augmentedProperties;
	}	

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return true;
	}
}
