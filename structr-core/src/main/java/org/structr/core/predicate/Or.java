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
package org.structr.core.predicate;

import org.structr.common.SecurityContext;
import org.structr.core.Predicate;

/**
 * Combines two predicates with OR.
 *
 *
 */
public class Or<T> implements Predicate<T> {

	Predicate<T> p1 = null;
	Predicate<T> p2 = null;

	public Or(Predicate<T> p1, Predicate<T> p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public boolean evaluate(SecurityContext securityContext, T... obj) {
		return p1.evaluate(securityContext, obj) || p2.evaluate(securityContext, obj);
	}
}
