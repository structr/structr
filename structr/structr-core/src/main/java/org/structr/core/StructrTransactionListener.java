/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

package org.structr.core;

import java.util.Map;
import org.structr.common.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * An interface that allows you to be notified when a
 * GraphObject is modified, with the option to veto the modification.
 * In order to use this interface, you must register your
 * implementation in {@see EntityContext}.
 *
 * @author Christian Morgner
 */
public interface StructrTransactionListener {

	public void begin(SecurityContext securityContext, long transactionKey);
	public void commit(SecurityContext securityContext, long transactionKey);
	public void rollback(SecurityContext securityContext, long transactionKey);

	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue, Object newValue);
	public boolean propertyRemoved(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue);

	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) throws FrameworkException;
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) throws FrameworkException;
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, Map<String, Object> properties) throws FrameworkException;
}
