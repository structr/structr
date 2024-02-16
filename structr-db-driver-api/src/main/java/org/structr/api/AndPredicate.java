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
package org.structr.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class AndPredicate<T> implements Predicate<T> {

	private List<Predicate<T>> predicates = new ArrayList<>();

	public AndPredicate(final Predicate<T>... predicates) {
		this.predicates.addAll(Arrays.asList(predicates));
	}

	@Override
	public boolean accept(final T value) {

		for (final Predicate<T> p : predicates) {

			if (!p.accept(value)) {
				return false;
			}
		}

		// default is true if no values are given?
		return true;
	}

	public void addPredicate(final Predicate<T> predicate) {
		predicates.add(predicate);
	}
}
