/*
 *  Copyright (C) 2010-2013 Axel Morgner
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

import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * An interface that allows you to be notified when a {@link  GraphObject} is
 * modified, with the option to veto the modification. In order to use this
 * interface, you must register your implementation in {@link EntityContext}.
 *
 * @author Christian Morgner
 */
public interface StructrTransactionListener {

	/**
	 * Called when the transaction has started. When this method is called,
	 * the transaction is already in the process of being finished.
	 * 
	 * @param securityContext the security context
	 * @param transactionKey the transaction key
	 */
	public void begin(SecurityContext securityContext, long transactionKey);
	
	/**
	 * Called when the transaction is about to be committed.
	 * 
	 * @param securityContext the security context
	 * @param transactionKey the transaction key
	 */
	public void commit(SecurityContext securityContext, long transactionKey);
	
	/**
	 * Called when the transaction is about to be rolled back.
	 * 
	 * @param securityContext the security context
	 * @param transactionKey the transaction key
	 */
	public void rollback(SecurityContext securityContext, long transactionKey);

	/**
	 * Called for every property that is modified on a given node in the
	 * transaction.
	 * 
	 * @param securityContext
	 * @param transactionKey
	 * @param errorBuffer
	 * @param graphObject
	 * @param key
	 * @param oldValue
	 * @param newValue
	 * @return 
	 */
	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue, Object newValue);
	
	/**
	 * Called for every property that is removed from a given node in the
	 * transaction.
	 * 
	 * @param securityContext
	 * @param transactionKey
	 * @param errorBuffer
	 * @param graphObject
	 * @param key
	 * @param oldValue
	 * @return 
	 */
	public boolean propertyRemoved(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue);

	/**
	 * Called for every entity that is created in the transaction.
	 * 
	 * @param securityContext
	 * @param transactionKey
	 * @param errorBuffer
	 * @param graphObject
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) throws FrameworkException;
	
	/**
	 * Called for every entity that is modified in the transaction.
	 * 
	 * @param securityContext
	 * @param transactionKey
	 * @param errorBuffer
	 * @param graphObject
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) throws FrameworkException;
	
	/**
	 * Called for every entity that is deleted in the transaction.
	 * 
	 * @param securityContext
	 * @param transactionKey
	 * @param errorBuffer
	 * @param graphObject
	 * @param properties
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyMap properties) throws FrameworkException;
}
