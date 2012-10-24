/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
 * Convenience interface to enable the use of enum types as node property keys.
 * The signature of this interface matches the signature of the enum class, so
 * you can use the following code to define property keys for
 * {@see org.structr.core.entity.AbstractNode} and subclasses.
 *
 * <pre>
 * public enum Key { property1, property2, property3 }
 * </pre>
 *
 * @author Christian Morgner
 */
public interface PropertyKey
{
	public String name();
}
