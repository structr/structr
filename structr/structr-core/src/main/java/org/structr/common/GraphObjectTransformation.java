/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.core.GraphObject;
import org.structr.core.Transformation;

/**
 *
 * @author Axel Morgner
 */
public abstract class GraphObjectTransformation implements Comparable, Transformation<GraphObject> {

	@Override
	public int compareTo(Object t) {
		if (t == null || !(t instanceof Transformation)) {
			return -1;
		}

		return ((Integer) this.getOrder()).compareTo(
			(Integer) (((Transformation) t).getOrder())
			);
	}	

	@Override
	public int getOrder() {
		return 10;
	}
}
