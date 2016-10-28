/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;

/**
 * Controls access to resource properties
 *
 * Objects of this class act as a doorkeeper for properties of REST resources.
 * <p>
 * A PropertyAccess object defines access granted
 * <ul>
 * <li>to everyone (public)
 * <li>to authenticated principals
 * <li>to invidual principals (when connected to a {link @Principal} node
 * </ul>
 *
 *
 *
 */
public class PropertyAccess extends AbstractNode {

	private static final Logger logger = LoggerFactory.getLogger(PropertyAccess.class.getName());

	private Long cachedFlags       = null;
	private Integer cachedPosition = null;

	public static final Property<Long>    flags    = new LongProperty("flags").indexed();
	public static final Property<Integer> position = new IntProperty("position").indexed();

	public static final View uiView = new View(PropertyAccess.class, PropertyView.Ui,
		flags, position
	);

	public static final View publicView = new View(PropertyAccess.class, PropertyView.Public,
		flags
	);

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append("('").append(flags.jsonName()).append(": ").append(getFlags()).append("', ").append(position.jsonName()).append(": ").append(getPosition()).append(")");

		return buf.toString();
	}

	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}

	public void setFlag(long flag) throws FrameworkException {

		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(ResourceAccess.flags, getFlags() | flag);
	}

	public void clearFlag(long flag) throws FrameworkException {

		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(ResourceAccess.flags, getFlags() & ~flag);
	}

	public long getFlags() {

		if (cachedFlags == null) {
			cachedFlags = getProperty(ResourceAccess.flags);
		}

		if (cachedFlags != null) {
			return cachedFlags;
		}

		return 0;
	}

	public int getPosition() {

		if (cachedPosition == null) {
			cachedPosition = getProperty(ResourceAccess.position);
		}

		if (cachedPosition != null) {
			return cachedPosition.intValue();
		}

		return 0;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this,  PropertyAccess.name, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, PropertyAccess.flags, errorBuffer);

		return valid;
	}
}
