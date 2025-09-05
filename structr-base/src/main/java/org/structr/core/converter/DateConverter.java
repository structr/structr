/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.converter;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

import java.util.Date;

/**
 * Converts a Long value to a Date and back.
 *
 *
 */
public class DateConverter extends PropertyConverter<Date, Long> {

	public DateConverter(SecurityContext securityContext) {
		super(securityContext, null);
	}

	@Override
	public Long convert(final Date source) throws FrameworkException {

		if(source != null) {

			return source.getTime();
		}

		return null;
	}

	@Override
	public Date revert(final Long source) throws FrameworkException {

		if (source != null) {

			return new Date(source);
		}

		return null;
	}

	@Override
	public Comparable convertForSorting(final Date source) throws FrameworkException {

		if (source != null) {

			return source;
		}

		return null;
	}
}
