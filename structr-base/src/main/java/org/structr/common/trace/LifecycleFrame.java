/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common.trace;

/**
 * A lifecycle method, such as onCreate or afterDelete, called for an entity while a transaction
 * changes it or commits. Named by the callback, with the method's node as its code source.
 */
public class LifecycleFrame extends MethodFrame {

	public LifecycleFrame(final Spec spec) {
		super(spec);
	}

	@Override
	public String kind() {
		return "LIFECYCLE";
	}

	/**
	 * <code>Type[id]:callback</code>, or <code>Type.callback</code> after a deletion, when there is no
	 * entity left to name.
	 */
	@Override
	public String describe(final String knownCodeSourceType) {

		if (getEntityId() == null && getDeclaringType() != null) {

			return getDeclaringType() + "." + getName();
		}

		return super.describe(knownCodeSourceType);
	}
}
