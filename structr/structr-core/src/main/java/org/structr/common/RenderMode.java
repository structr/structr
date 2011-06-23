/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

/**
 * The possible rendering modes.
 *
 * @author amorgner
 */
public enum RenderMode
{
	/**
	 * The default rendering mode. This mode will be used when a normal
	 * rendering turn is requested.
	 */
	Default,
	
	/**
	 * The direct rendering mode. This mode will be used when a single
	 * node is directly addressed via URL.
	 */
	Direct,
	
	/**
	 * The edit rendering mode. This mode will be used when inline editing
	 * is requested.
	 */
	Edit,

	// old types
	PUBLIC, LOCAL, EDIT;
}
