/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A transformation that can be applied to the result set of a resource.
 * 
 *
 */
public abstract class ViewTransformation<T> extends Transformation<List<T>> {

	@Override
	public abstract void apply(SecurityContext securityContext, List<T> list) throws FrameworkException;
	
	/**
	 * Indicates whether the underlying resource should be evaluated.
	 * 
	 * @return whether the underlying resource should be evaluated.
	 */
	public abstract boolean evaluateWrappedResource();
}
