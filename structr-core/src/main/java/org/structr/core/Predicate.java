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
package org.structr.core;

import org.structr.common.SecurityContext;

/**
 * An unary predicate that evaluates objects of type T.
 *
 *
 */
public interface Predicate<T>
{
	/**
	 * Evaluates the given object(s) and returns a boolean value.
	 *
	 * @param obj the object to evaluate
	 * @return true or false
	 */
	public boolean evaluate(SecurityContext securityContext, T... obj);
}
