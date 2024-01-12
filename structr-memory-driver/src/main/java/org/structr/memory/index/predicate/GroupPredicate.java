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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;

import java.util.LinkedList;
import java.util.List;

import static org.structr.memory.index.predicate.Conjunction.And;

/**
 */
public class GroupPredicate<T> implements Predicate<T> {

	private final List<Predicate<T>> predicates = new LinkedList<>();
	private Conjunction conjunction             = Conjunction.And;
	private GroupPredicate<T> parent            = null;

	public GroupPredicate(final GroupPredicate<T> parent, final Conjunction conj) {

		this.conjunction = conj;
		this.parent      = parent;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append(conjunction.name());
		buf.append("(");

		for (final Predicate<T> predicate : predicates) {
			buf.append(predicate.toString());
			buf.append(", ");
		}

		// remove last ","
		buf.setLength(buf.length() - 2);

		buf.append(")");

		return buf.toString();

	}

	public GroupPredicate<T> getParent() {
		return parent;
	}

	public void add(final Predicate<T> predicate) {
		predicates.add(predicate);
	}

	public void setConjunction(final Conjunction conj) {
		this.conjunction = conj;
	}

	@Override
	public boolean accept(final T value) {

		boolean result = And.equals(conjunction);

		for (final Predicate<T> predicate : predicates) {

			switch (conjunction) {

				case And:
					result &= predicate.accept(value);
					break;

				case Or:
					result |= predicate.accept(value);
					break;

				case Not:
					result &= !predicate.accept(value);
					break;
			}
		}

		return result;
	}
}
