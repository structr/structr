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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;

/**
 * Combines two predicates with AND.
 *
 *
 */
public class And<T> implements Predicate<T> {

	final List<Predicate<T>> predicates = new LinkedList<>();

	public And(Predicate<T>... predicateArray) {
		this(Arrays.asList(predicateArray));
	}

	public And(List<Predicate<T>> predicateList) {
		predicates.addAll(predicateList);
	}

	@Override
	public boolean evaluate(SecurityContext securityContext, T... obj) {
		
		boolean result = true;
		
		for (Predicate<T> predicate : predicates) {
			result &= predicate.evaluate(securityContext, obj);
		}
		
		return result;
	}
}
