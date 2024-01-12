/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;

/**
 *
 *
 */
public class TestFive extends AbstractNode {

	private static final Logger logger = LoggerFactory.getLogger(TestFive.class.getName());

	public static final Property<Integer> intProperty                  = new IntProperty("integerProperty").indexed();
	public static final Property<Integer> modifiedInBeforeCreation     = new IntProperty("modifiedInBeforeCreation").defaultValue(0).indexed().unvalidated();
	public static final Property<Integer> modifiedInBeforeModification = new IntProperty("modifiedInBeforeModification").defaultValue(0).indexed().unvalidated();
	public static final Property<Integer> modifiedInAfterCreation      = new IntProperty("modifiedInAfterCreation").defaultValue(0).indexed().unvalidated();
	public static final Property<Integer> modifiedInAfterModification  = new IntProperty("modifiedInAfterModification").defaultValue(0).indexed().unvalidated();

	public static final View publicView = new View(TestFive.class, PropertyView.Public,
		intProperty, modifiedInBeforeCreation, modifiedInBeforeModification, modifiedInAfterCreation, modifiedInAfterModification
	);

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		int value = getIncreasedValue(modifiedInBeforeCreation);
		setProperty(modifiedInBeforeCreation, value);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		int value = getIncreasedValue(modifiedInBeforeModification);
		setProperty(modifiedInBeforeModification, value);
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final int value = getIncreasedValue(modifiedInAfterCreation);
			setProperty(modifiedInAfterCreation, value);

			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		try (final Tx tx = app.tx()) {

			final int value = getIncreasedValue(modifiedInAfterModification);

			setProperty(modifiedInAfterModification, value);
			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
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
