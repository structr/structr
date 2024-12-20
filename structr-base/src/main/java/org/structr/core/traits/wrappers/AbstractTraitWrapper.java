/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.traits.wrappers;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.traits.Traits;

import java.util.Date;

public abstract class AbstractTraitWrapper<T extends GraphObject> {

	protected final T wrappedObject;
	protected final Traits traits;

	public AbstractTraitWrapper(final Traits traits, final T wrappedObject) {

		this.wrappedObject = wrappedObject;
		this.traits        = traits;
	}

	public SecurityContext getSecurityContext() {
		return wrappedObject.getSecurityContext();
	}

	public String getUuid() {
		return wrappedObject.getUuid();
	}

	public String getType() {
		return wrappedObject.getType();
	}

	public String getName() {
		return wrappedObject.getProperty(traits.key("name"));
	}

	public void setName(final String name) throws FrameworkException {
		wrappedObject.setProperty(traits.key("name"), name);
	}

	public Date getCreatedDate() {
		return wrappedObject.getProperty(traits.key("createdDate"));
	}

	public Date getLastModifiedDate() {
		return wrappedObject.getProperty(traits.key("lastModifiedDate"));
	}

	public T getWrappedNode() {
		return wrappedObject;
	}

	public boolean isVisibleToPublicUsers() {
		return wrappedObject.getProperty(traits.key("visibleToPublicUsers"));
	}

	public boolean isVisibleToAuthenticatedUsers() {
		return wrappedObject.getProperty(traits.key("visibleToAuthenticatedUsers"));
	}

	public void setVisibleToPublicUsers(final boolean visible) throws FrameworkException {
		wrappedObject.setProperty(traits.key("visibleToPublicUsers"), visible);
	}

	public void  setVisibleToAuthenticatedUsers(final boolean visible) throws FrameworkException {
		wrappedObject.setProperty(traits.key("visibleToAuthenticatedUsers"), visible);
	}
}
