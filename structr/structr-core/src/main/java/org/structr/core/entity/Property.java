/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.entity;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class Property {

	private static final Logger logger = Logger.getLogger(Property.class.getName());
	private SecurityContext securityContext = null;
	private final Node dbNode;
	private String key;
	private Object value;

	/**
	 * Create bean with reference to backend database and node
	 *
	 * @param graphDb
	 * @param dbNode
	 */
	public Property(final SecurityContext securityContext, final Node dbNode) {
		this.dbNode = dbNode;
		this.key = null;
		this.value = null;
		this.securityContext = securityContext;
	}

	/**
	 * Create bean with reference to backend database and node,
	 * get value for key from backend
	 *
	 * @param graphDb
	 * @param dbNode
	 * @param key
	 */
	public Property(final SecurityContext securityContext, final Node dbNode, final String key) {
		this.dbNode = dbNode;
		this.key = key;
		this.value = getValue();
		this.securityContext = securityContext;
	}

	/**
	 * Create bean and set key and value in backend
	 *
	 * @param graphDb
	 * @param dbNode
	 * @param key
	 * @param value
	 */
	public Property(final SecurityContext securityContext, final Node dbNode, final String key, final String value) {
		this.dbNode = dbNode;
		this.key = key;
		this.value = value;
		this.securityContext = securityContext;
		setPropertyInBackend();
	}

	public String getKey() {
		return key;
	}

	/**
	 * Get value from underlying db node with given key
	 *
	 * TODO: support others than String values
	 */
	public final Object getValue() {

		Object ret = null;

		if(dbNode.hasProperty(key)) {
			if(key.equals(AbstractNode.Key.createdDate.name())
			    || key.equals(AbstractNode.Key.lastModifiedDate.name())) {

				ret = new Date((Long)dbNode.getProperty(key));

			} else {

				ret = dbNode.getProperty(key);

			}

		}

		return ret;
	}

	private void setPropertyInBackend() {

		try {
			Command transaction = Services.command(securityContext, TransactionCommand.class);
			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					if(key.equals(AbstractNode.Key.createdDate.name())
					    || key.equals(AbstractNode.Key.lastModifiedDate.name())) {

						Date d = (Date)value;

						// store date as long
						dbNode.setProperty(key, d.getTime());

					} else {

						dbNode.setProperty(key, value);

					}

					return (null);
				}
			});

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to set property in backend", fex);
		}
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public void setValue(final Object value) {
		this.value = value;
		setPropertyInBackend();
	}
}
