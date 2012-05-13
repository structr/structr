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

import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public interface Principal extends GraphObject {

	public enum Key implements PropertyKey { sessionId, groups }

	public enum UserIndexKey implements PropertyKey{ name, email; }

	//~--- methods --------------------------------------------------------

	public void block() throws FrameworkException;

	//~--- get methods ----------------------------------------------------

	public String getEncryptedPassword();

	public Object getPropertyForIndexing(final String key);

	public String getPassword();

	public String getRealName();

	public String getConfirmationKey();

	public Boolean getBlocked();

	public String getSessionId();

	public Boolean isBlocked();

	public boolean isBackendUser();

	public boolean isFrontendUser();
	
	//~--- set methods ----------------------------------------------------

	public void setPassword(final String passwordValue) throws FrameworkException;

	public void setRealName(final String realName) throws FrameworkException;

	public void setBlocked(final Boolean blocked) throws FrameworkException;

	public void setConfirmationKey(final String value) throws FrameworkException;

	public void setFrontendUser(final boolean isFrontendUser) throws FrameworkException;

	public void setBackendUser(final boolean isBackendUser) throws FrameworkException;
}
