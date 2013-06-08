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
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.EntityContext;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.property.DoubleProperty;

/**
 * A simple entity with lat,lon coordinates
 * 
 * 
 * @author Axel Morgner
 */
public class TestSeven extends AbstractNode {
	
	public static final Property<Double> latitude = new DoubleProperty("latitude");
	public static final Property<Double> longitude = new DoubleProperty("longitude");

	public static final View publicView = new View(TestSeven.class, PropertyView.Public,
		latitude, longitude
	);
	
	static {
		
		EntityContext.registerSearchablePropertySet(TestSeven.class, NodeIndex.fulltext.name(), latitude, longitude);
		EntityContext.registerSearchablePropertySet(TestSeven.class, NodeIndex.keyword.name(), latitude, longitude);
	}
	
	@Override
	public boolean beforeCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean beforeModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, name, errorBuffer);

		return !error;
	}
	
}
