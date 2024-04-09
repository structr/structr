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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public class TestEight extends AbstractNode {

	public static final Property<Integer> testProperty = new IntProperty("testProperty");

	public static final View defaultView = new View(TestEight.class, PropertyView.Public, testProperty);

	private long onCreationTimestamp        = 0L;
	private long onModificationTimestamp    = 0L;
	private long onDeletionTimestamp        = 0L;
	private long afterCreationTimestamp     = 0L;
	private long afterModificationTimestamp = 0L;

	@Override
	public void onCreation(SecurityContext securityContext1, ErrorBuffer errorBuffer) throws FrameworkException {
		this.onCreationTimestamp = System.currentTimeMillis();
	}

	@Override
	public void onModification(SecurityContext securityContext1, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		this.onModificationTimestamp = System.currentTimeMillis();
	}

	@Override
	public void onDeletion(SecurityContext securityContext1, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		this.onDeletionTimestamp = System.currentTimeMillis();
	}

	@Override
	public void afterCreation(SecurityContext securityContext1) {

		this.afterCreationTimestamp = System.currentTimeMillis();
	}

	@Override
	public void afterModification(SecurityContext securityContext1) throws FrameworkException {

		this.afterModificationTimestamp = System.currentTimeMillis();
	}

	public void resetTimestamps() {

		onCreationTimestamp        = 0L;
		onModificationTimestamp    = 0L;
		onDeletionTimestamp        = 0L;
		afterCreationTimestamp     = 0L;
		afterModificationTimestamp = 0L;
	}

	public long getOnCreationTimestamp() {
		return onCreationTimestamp;
	}

	public long getOnModificationTimestamp() {
		return onModificationTimestamp;
	}

	public long getOnDeletionTimestamp() {
		return onDeletionTimestamp;
	}

	public long getAfterCreationTimestamp() {
		return afterCreationTimestamp;
	}

	public long getAfterModificationTimestamp() {
		return afterModificationTimestamp;
	}
}
