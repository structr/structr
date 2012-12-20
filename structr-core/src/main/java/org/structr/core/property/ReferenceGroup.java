/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;

/**
 * A property that returns grouped properties from a set of {@see Reference} elements.
 * 
 * @author Christian Morgner
 */
public class ReferenceGroup extends GroupProperty {

	private static final Logger logger = Logger.getLogger(ReferenceGroup.class.getName());
	
	public ReferenceGroup(String name, Class<? extends GraphObject> entityClass, Reference... references) {
		super(name, entityClass, references);
	}
	
	// ----- interface PropertyGroup -----
	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		if(source instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)source;
			PropertyMap properties   = new PropertyMap();

			for (PropertyKey key : propertyKeys.values()) {

				Reference reference = (Reference)key;

				GraphObject referencedEntity = reference.getReferencedEntity(rel);
				PropertyKey referenceKey     = reference.getReferenceKey();
				PropertyKey propertyKey      = reference.getPropertyKey();
				
				if (referencedEntity != null) {
					
					properties.put(propertyKey, referencedEntity.getProperty(referenceKey));
				}
			}
			
			return properties;
		}
		
		return null;
	}

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if(destination instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)destination;

			for (PropertyKey key : propertyKeys.values()) {

				Reference reference = (Reference)key;
				
				GraphObject referencedEntity = reference.getReferencedEntity(rel);
				PropertyKey referenceKey     = reference.getReferenceKey();
				PropertyKey propertyKey      = reference.getPropertyKey();
				
				if (referencedEntity != null && !reference.isReadOnlyProperty()) {
					
					Object value = source.get(propertyKey);
					referencedEntity.setProperty(referenceKey, value);
				}
			}
		}
	}
}
