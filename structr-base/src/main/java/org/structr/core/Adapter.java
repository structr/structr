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
package org.structr.core;

import org.structr.common.error.FrameworkException;

/**
 * Defines a filtering adapter that can adapt an instance of source
 * class S to an instance of target class T.
 *
 *
 */
public interface Adapter<S, T>
{
	/**
	 * Adapts the given instance of S to T.
	 *
	 * @param s the source class
	 * @return the target class
	 */
	T adapt(S s) throws FrameworkException;
}
