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
package org.structr.core;

import org.structr.common.SecurityContext;

/**
 * A generic value parameter for functional evaluation, with a static value
 * assigned to it. This value object will return the exact given value on
 * every evaluation.
 *
 *
 */
public class StaticValue<T> implements Value<T> {

	private T value = null;

	public StaticValue(T value) {
		this.value = value;
	}
	
	@Override
	public void set(SecurityContext securityContext, T value) {
		this.value = value;
	}

	@Override
	public T get(SecurityContext securityContext) {
		return value;
	}
}
