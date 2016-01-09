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
package org.structr.core.predicate;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;


/**
 * A predicate that evaluates to <b>true</b> if two Comparable objects compare
 * to a value greater or equal to 0 using the compareTo methods. This predicate
 * evaluates to <b>true</b> if there is only one parameter value for the
 * evaluate method.
 *
 *
 */
public class MoreOrEqual<T extends Comparable> implements Predicate<T> {

	private static final Logger logger = Logger.getLogger(MoreOrEqual.class.getName());
	
	@Override
	public boolean evaluate(SecurityContext securityContext, T... objs) {
		
		if(objs.length == 0) {
			return false;
		}
		
		if(objs.length == 1) {
			return true;
		}
		
		if(objs.length == 2) {
			
			if (objs[0] != null && objs[1] != null) {
				
				return objs[0].compareTo(objs[1]) >= 0;
				
			} else {
			
				logger.log(Level.WARNING, "Cannot compare null value(s). Did you forget to set a default value?");
			}
		}
		
		throw new IllegalStateException("Cannot compare more than two objects yet.");
	}
}
