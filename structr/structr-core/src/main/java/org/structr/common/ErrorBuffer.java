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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A buffer that collects error messages to allow for i18n
 * and human readable output later.
 *
 * @author Christian Morgner
 */
public class ErrorBuffer {

	private static final String SEPARATOR_CHAR = " ";
	
	private List<String> messages = new LinkedList<String>();
	private boolean hasError = false;

	public void add(Object... msg) {

		StringBuilder buf = new StringBuilder();
		for(Object o : msg) {
			buf.append(o);
		}

		messages.add(buf.toString());

		hasError = true;
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder(400);

		for(Iterator<String> it = messages.iterator(); it.hasNext();) {

			buf.append(it.next());

			if(it.hasNext()) {
				buf.append(SEPARATOR_CHAR);
			}
		}

		return buf.toString();
	}

	public boolean hasError() {
		return hasError;
	}
}
