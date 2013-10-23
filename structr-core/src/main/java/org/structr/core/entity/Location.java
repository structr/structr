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

import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.relationship.LocationRelationship;
import org.structr.core.property.DoubleProperty;

//~--- classes ----------------------------------------------------------------

/**
 * The Location entity.
 *
 * @author Axel Morgner
 */
public class Location extends AbstractNode {

	public static final Property<Double> latitude  = new DoubleProperty("latitude").indexed();
	public static final Property<Double> longitude = new DoubleProperty("longitude").indexed();
	public static final Property<Double> altitude  = new DoubleProperty("altitude").indexed();

	public static final View publicView = new View(Location.class, PropertyView.Public,
		latitude, longitude, altitude
	);

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		notifyLocatables();
	}
	
	@Override
	public void afterModification(SecurityContext securityContext) {
		notifyLocatables();
		
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

//              error |= ValidationHelper.checkPropertyNotNull(this, Key.latitude, errorBuffer);
//              error |= ValidationHelper.checkPropertyNotNull(this, Key.longitude, errorBuffer);
		error |= notifyLocatables();
		
		return !error;

	}
	
	private boolean notifyLocatables() {
		
		// FIXME: LocationRelationship has a direction. but it is ignored here
		
		boolean allLocatablesAreValid = false;
		
		for(LocationRelationship rel : this.getRelationships(LocationRelationship.class)) {
			
			AbstractNode otherNode = rel.getOtherNode(this);
			if(otherNode != null && otherNode instanceof Locatable) {
				
				// notify other node of location change
				allLocatablesAreValid |= !((Locatable)otherNode).locationChanged();
			}
		}
		
		return allLocatablesAreValid;
	}

}
