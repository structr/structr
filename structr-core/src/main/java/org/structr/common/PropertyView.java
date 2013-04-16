/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

/**
 * Defines the default property views for structr, see {@link View} and the
 * example archetype for more information.
 *
 * @author Christian Morgner
 */
public interface PropertyView {

	/**
	 * The "all" view, a system view that is created automatically when
	 * scanning the entities upon system start.
	 */
	public static final String All =	"all";
	
	/**
	 * The "public" view, this is the default view for structr entities.
	 */
	public static final String Public =	"public";
	
	/**
	 * The "protected" view, free to use.
	 */
	public static final String Protected =	"protected";
	
	/**
	 * The "private" view, free to use.
	 */
	public static final String Private =	"private";
	
	/**
	 * The "owner" view, free to use.
	 */
	public static final String Owner =	"owner";
	
	/**
	 * The "ui" view used by structr UI.
	 */
	public static final String Ui	=	"ui";
	
	/**
	 * The "html" view used by structr UI.
	 */
	public static final String Html =	"_html_";
}
