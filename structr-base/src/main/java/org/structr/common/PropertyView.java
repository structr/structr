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
package org.structr.common;

import org.structr.docs.Documentation;

/**
 * Defines the default property views for structr, see {@link View} and the
 * example archetype for more information.
 */
public interface PropertyView {

	/**
	 * The "all" view, a system view that is created automatically when
	 * scanning the entities upon system start.
	 */
	String All =	 "all";

	/**
	 * The "public" view, this is the default view for structr entities.
	 */
	String Public =	"public";

	/**
	 * The "custom" view, this is the default view for custom attributes.
	 */
	String Custom =	"custom";

	/**
	 * The "protected" view, free to use.
	 */
	String Protected =	"protected";

	/**
	 * The "private" view, free to use.
	 */
	String Private =	"private";

	/**
	 * The "ui" view used by structr UI.
	 */
	String Ui	=	"ui";

	/**
	 * The "html" view used by structr UI.
	 */
	String Html =	"_html_";

	/**
	 * The "schema" view used by structr UI.
	 */
	String Schema =	"schema";

	public static boolean isManagedView(final String viewName) {

		return viewName.equals(All) || viewName.equals(Custom);
	}

	/**
	 * The views Structr's own back end works with, as opposed to the views an application publishes.
	 * Neither is curated for an audience: "all" is generated from every registered property of a type,
	 * and "ui" is hand-picked for the back end, which is allowed to see everything anyway. On a
	 * Principal that means session ids, refresh tokens, the two-factor token and the confirmation key,
	 * each of which is enough to take the account over, so reading them is restricted to administrators
	 * (ticket 1584).
	 *
	 * <p>"_html_" and "schema" are internal in the same sense but carry no such properties, so they are
	 * deliberately not in here: restricting them would break applications without closing anything.
	 */
	static boolean isInternalView(final String viewName) {

		return All.equals(viewName) || Ui.equals(viewName);
	}
}
