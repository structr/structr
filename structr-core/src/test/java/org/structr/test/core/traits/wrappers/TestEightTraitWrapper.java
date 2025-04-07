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
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.test.core.entity.TestEight;

/**
 *
 *
 */
public class TestEightTraitWrapper extends AbstractNodeTraitWrapper implements TestEight {

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

		final Object t = wrappedObject.getTemporaryStorage().get("onCreationTimestamp");
		if (t != null && t instanceof Long l) {

			return l;
		}

		return 0L;
	}

	public long getOnModificationTimestamp() {

		final Object t = wrappedObject.getTemporaryStorage().get("onModificationTimestamp");
		if (t != null && t instanceof Long l) {

			return l;
		}

		return 0L;
	}

	public long getOnDeletionTimestamp() {

		final Object t = wrappedObject.getTemporaryStorage().get("onDeletionTimestamp");
		if (t != null && t instanceof Long l) {

			return l;
		}

		return 0L;
	}

	public long getAfterCreationTimestamp() {

		final Object t = wrappedObject.getTemporaryStorage().get("afterCreationTimestamp");
		if (t != null && t instanceof Long l) {

			return l;
		}

		return 0L;
	}

	public long getAfterModificationTimestamp() {

		final Object t = wrappedObject.getTemporaryStorage().get("afterModificationTimestamp");
		if (t != null && t instanceof Long l) {

			return l;
		}

		return 0L;
	}
}
