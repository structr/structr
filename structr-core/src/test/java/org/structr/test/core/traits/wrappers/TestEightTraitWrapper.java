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
package org.structr.test.core.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.test.core.entity.TestEight;

/**
 *
 *
 */
public class TestEightTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements TestEight {

	public TestEightTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public void resetTimestamps() {

		wrappedObject.getTemporaryStorage().put("onCreationTimestamp", 0L);
		wrappedObject.getTemporaryStorage().put("onModificationTimestamp", 0L);
		wrappedObject.getTemporaryStorage().put("onDeletionTimestamp", 0L);
		wrappedObject.getTemporaryStorage().put("afterCreationTimestamp", 0L);
		wrappedObject.getTemporaryStorage().put("afterModificationTimestamp", 0L);
	}

	public long getOnCreationTimestamp() {
		return (Long)wrappedObject.getTemporaryStorage().get("onCreationTimestamp");
	}

	public long getOnModificationTimestamp() {
		return (Long)wrappedObject.getTemporaryStorage().get("onModificationTimestamp");
	}

	public long getOnDeletionTimestamp() {
		return (Long)wrappedObject.getTemporaryStorage().get("onDeletionTimestamp");
	}

	public long getAfterCreationTimestamp() {
		return (Long)wrappedObject.getTemporaryStorage().get("afterCreationTimestamp");
	}

	public long getAfterModificationTimestamp() {
		return (Long)wrappedObject.getTemporaryStorage().get("afterModificationTimestamp");
	}
}
