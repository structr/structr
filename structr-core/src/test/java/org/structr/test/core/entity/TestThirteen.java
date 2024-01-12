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

import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 */
public class TestThirteen extends AbstractNode {

	public static final Property<String>        notNull            = new StringProperty("notNull").indexed().indexedWhenEmpty().notNull();
	
	@Override
	public boolean isValid(final ErrorBuffer buf) {

		boolean valid = super.isValid(buf);

		valid &= ValidationHelper.isValidPropertyNotNull(this, notNull, buf);

		return valid;
	}
}
