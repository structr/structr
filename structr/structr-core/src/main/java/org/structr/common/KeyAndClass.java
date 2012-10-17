/*
 *  Copyright (C) 2012 Axel Morgner
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
 * Simple class to hold a {@link PropertyKey} and a {@link Class}
 *
 * @author Axel Morgner
 */
public class KeyAndClass {

	private Class cls;
	private PropertyKey key;

	//~--- constructors ---------------------------------------------------

	public KeyAndClass(final PropertyKey key, final Class cls) {

		this.key = key;
		this.cls = cls;

	}

	//~--- get methods ----------------------------------------------------

	public PropertyKey getPropertyKey() {

		return key;

	}

	public Class getCls() {

		return cls;

	}

}
