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
package org.structr.core.entity;

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.*;
import org.structr.core.EntityContext;
import org.structr.core.graph.NodeService.NodeIndex;

/**
 *
 * @author Christian Morgner
 */
public class TestFive extends AbstractNode {
	
	public static final Property<Integer> intProperty                  = new IntProperty("integerProperty");
	public static final Property<Integer> modifiedInBeforeCreation     = new IntProperty("modifiedInBeforeCreation", 0).systemProperty();
	public static final Property<Integer> modifiedInBeforeModification = new IntProperty("modifiedInBeforeModification", 0).systemProperty();
	public static final Property<Integer> modifiedInAfterCreation      = new IntProperty("modifiedInAfterCreation", 0).systemProperty();
	public static final Property<Integer> modifiedInAfterModification  = new IntProperty("modifiedInAfterModification", 0).systemProperty();
	
	
	
	public static final View publicView = new View(TestFive.class, PropertyView.Public,
		intProperty, modifiedInBeforeCreation, modifiedInBeforeModification, modifiedInAfterCreation, modifiedInAfterModification
	);
	
	static {
		
		EntityContext.registerSearchablePropertySet(TestFive.class, NodeIndex.keyword.name(), publicView.properties());
	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		int value = getIncreasedValue(modifiedInBeforeCreation);
		setProperty(modifiedInBeforeCreation, value);
		
		return true;
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		int value = getIncreasedValue(modifiedInBeforeModification);
		setProperty(modifiedInBeforeModification, value);

		return true;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		
		try {
			int value = getIncreasedValue(modifiedInAfterCreation);
			setProperty(modifiedInAfterCreation, value);
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
		
		try {
			int value = getIncreasedValue(modifiedInAfterModification);
			setProperty(modifiedInAfterModification, value);
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
	}
	
	private int getIncreasedValue(Property<Integer> key) {
		
		Integer value = getProperty(key);
		
		if (value != null) {
			
			return value.intValue() + 1;
		}
		
		return 1;
	}
}
