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

package org.structr.core.predicate;

import org.structr.common.SecurityContext;
import org.structr.core.Predicate;


/**
 *
 * @author Christian Morgner
 */
public class GreaterThan<T extends Comparable> implements Predicate<T> {

	@Override
	public boolean evaluate(SecurityContext securityContext, T... objs) {
		
		if(objs.length == 0) {
			return false;
		}
		
		if(objs.length == 1) {
			return false;
		}
		
		if(objs.length == 2) {
			
			return objs[0].compareTo(objs[1]) > 0;
		}
		
		throw new IllegalStateException("Cannot compare more than two objects yet.");
	}
}
