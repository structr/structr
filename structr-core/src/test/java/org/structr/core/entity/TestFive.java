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

import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

/**
 *
 * @author Christian Morgner
 */
public class TestFive extends AbstractNode {
	
	public static final Property<Integer> intProperty                  = new IntProperty("integerProperty").indexed();
	public static final Property<Integer> modifiedInBeforeCreation     = new IntProperty("modifiedInBeforeCreation", 0).indexed().unvalidated();
	public static final Property<Integer> modifiedInBeforeModification = new IntProperty("modifiedInBeforeModification", 0).indexed().unvalidated();
	public static final Property<Integer> modifiedInAfterCreation      = new IntProperty("modifiedInAfterCreation", 0).indexed().unvalidated();
	public static final Property<Integer> modifiedInAfterModification  = new IntProperty("modifiedInAfterModification", 0).indexed().unvalidated();
	
	public static final View publicView = new View(TestFive.class, PropertyView.Public,
		intProperty, modifiedInBeforeCreation, modifiedInBeforeModification, modifiedInAfterCreation, modifiedInAfterModification
	);
	
	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		int value = getIncreasedValue(modifiedInBeforeCreation);
		setProperty(modifiedInBeforeCreation, value);
		
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		int value = getIncreasedValue(modifiedInBeforeModification);
		setProperty(modifiedInBeforeModification, value);

		return true;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		
		final App app = StructrApp.getInstance(securityContext);
		try {
			final int value = getIncreasedValue(modifiedInAfterCreation);
			
			app.beginTx();
			setProperty(modifiedInAfterCreation, value);
			app.commitTx();
			
		} catch (Throwable t) {
			
			t.printStackTrace();

		} finally {

			app.finishTx();
		}
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
		
		final App app = StructrApp.getInstance(securityContext);
		try {
			final int value = getIncreasedValue(modifiedInAfterModification);
			
			app.beginTx();
			setProperty(modifiedInAfterModification, value);
			app.commitTx();
			
		} catch (Throwable t) {
			
			t.printStackTrace();

		} finally {

			app.finishTx();
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
