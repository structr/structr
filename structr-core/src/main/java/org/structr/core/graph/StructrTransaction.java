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
package org.structr.core.graph;

import org.structr.common.error.FrameworkException;

/**
 * Encapsulates the code that need to be wrapped into a transaction.
 * 
 * @author Christian Morgner
 */
public abstract class StructrTransaction<T> {
	
	protected boolean doValidation = true;
	
	public StructrTransaction() {
		this.doValidation = true;
	}
	
	public StructrTransaction(boolean doValidation) {
		this.doValidation = doValidation;
	}
	
	public abstract T execute() throws FrameworkException;
}
