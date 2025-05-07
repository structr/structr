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
package org.structr.common;

/**
 * Defines the default property views for structr, see {@link View} and the
 * example archetype for more information.
 *
 *
 */
public interface PropertyView {

	/**
	 * The "all" view, a system view that is created automatically when
	 * scanning the entities upon system start.
	 */
	public static final String All =	 "all";

	/**
	 * The "public" view, this is the default view for structr entities.
	 */
	public static final String Public =	"public";

	/**
	 * The "custom" view, this is the default view for custom attributes.
	 */
	public static final String Custom =	"custom";

	/**
	 * The "protected" view, free to use.
	 */
	public static final String Protected =	"protected";

	/**
	 * The "private" view, free to use.
	 */
	public static final String Private =	"private";

	/**
	 * The "ui" view used by structr UI.
	 */
	public static final String Ui	=	"ui";

	/**
	 * The "html" view used by structr UI.
	 */
	public static final String Html =	"_html_";

	/**
	 * The "schema" view used by structr UI.
	 */
	public static final String Schema =	"schema";
}
