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
package org.structr.docs.ontology;

public enum Verb {

	Has("has",               "ispartof"),
	Uses("uses",             "isusedby"),
	Provides("provides",     "isprovidedby"),
	Opens("opens",           "isopenedby"),
	Closes("closes",         "isclosedby"),
	Contains("contains",     "iscontainedby"),
	Creates("creates",       "iscreatedby"),
	Removes("removes",       "isremovedby"),
	Deletes("deletes",       "isdeletedby"),
	Configures("configures", "isconfiguredby"),
	Displays("displays",     "isdisplayedby"),
	WritesTo("writesto",     "iswrittenfrom"),
	Executes("executes",     "isexecutedby"),
	Matches("matches",       "matches"),
	Is("is",                 "is");

	private final String leftToRight;
	private final String rightToLeft;

	Verb(final String leftToRight, final String rightToLeft) {

		this.leftToRight = leftToRight;
		this.rightToLeft = rightToLeft;
	}

	public String getLeftToRight() {
		return leftToRight;
	}

	public String getRightToLeft() {
		return rightToLeft;
	}

	public static Verb leftToRight(final String name) {

		for (final Verb verb : Verb.values()) {

			if (verb.getLeftToRight().equals(name)) {
				return verb;
			}
		}

		return null;
	}

	public static Verb rightToLeft(final String name) {

		for (final Verb verb : Verb.values()) {

			if (verb.getRightToLeft().equals(name)) {
				return verb;
			}
		}

		return null;
	}
}
