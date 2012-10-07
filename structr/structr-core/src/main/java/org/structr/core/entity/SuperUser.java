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



package org.structr.core.entity;

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class SuperUser implements Principal {

	@Override
	public void block() {

		// not supported
	}

	@Override
	public void removeProperty(String key) throws FrameworkException {}

	@Override
	public void grant(Permission permission, AccessControllable obj) {}

	@Override
	public void revoke(Permission permission, AccessControllable obj) {}

	@Override
	public void unlockReadOnlyPropertiesOnce() {}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		return true;

	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		return true;

	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException {

		return true;

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {}

	@Override
	public void afterModification(SecurityContext securityContext) {}

	@Override
	public void afterDeletion(SecurityContext securityContext) {}

	@Override
	public void ownerModified(SecurityContext securityContext) {}

	@Override
	public void securityModified(SecurityContext securityContext) {}

	@Override
	public void locationModified(SecurityContext securityContext) {}

	@Override
	public void propagatedModification(SecurityContext securityContext) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public long getId() {

		return -1L;

	}

//      @Override
	public String getRealName() {

		return "Super User";

	}

	@Override
	public Boolean getBlocked() {

		return false;

	}

	@Override
	public String getEncryptedPassword() {

		return null;

	}

//      @Override
	public Object getPropertyForIndexing(String key) {

		return null;

	}

//      @Override
	public String getPassword() {

		return null;

	}

//      @Override
	public String getConfirmationKey() {

		return null;

	}

//      @Override
	public String getSessionId() {

		return null;

	}

	@Override
	public String getType() {

		return null;

	}

	@Override
	public Iterable<String> getPropertyKeys(String propertyView) {

		return null;

	}

	@Override
	public Object getProperty(String key) {

		return null;

	}

	@Override
	public Object getProperty(PropertyKey propertyKey) {

		return null;

	}

	@Override
	public String getStringProperty(String key) {

		return null;

	}

	@Override
	public String getStringProperty(PropertyKey propertyKey) {

		return null;

	}

	@Override
	public Integer getIntProperty(String key) {

		return null;

	}

	@Override
	public Integer getIntProperty(PropertyKey propertyKey) {

		return null;

	}

	@Override
	public Date getDateProperty(String key) {

		return null;

	}

	@Override
	public Date getDateProperty(PropertyKey key) {

		return null;

	}

	@Override
	public boolean getBooleanProperty(String key) throws FrameworkException {

		return false;

	}

	@Override
	public boolean getBooleanProperty(PropertyKey key) throws FrameworkException {

		return false;

	}

	@Override
	public Double getDoubleProperty(String key) throws FrameworkException {

		return null;

	}

	@Override
	public Double getDoubleProperty(PropertyKey key) throws FrameworkException {

		return null;

	}

	@Override
	public Comparable getComparableProperty(PropertyKey key) throws FrameworkException {

		return null;

	}

	@Override
	public Comparable getComparableProperty(String key) throws FrameworkException {

		return null;

	}

	@Override
	public PropertyKey getDefaultSortKey() {

		return null;

	}

	@Override
	public String getDefaultSortOrder() {

		return null;

	}

	@Override
	public List<Principal> getParents() {

		return Collections.emptyList();

	}

	@Override
	public String getUuid() {

		return null;

	}

	@Override
	public Long getLongProperty(String key) {

		return null;

	}

	@Override
	public Long getLongProperty(PropertyKey propertyKey) {

		return null;

	}

	@Override
	public Boolean isBlocked() {

		return false;

	}

//      @Override
	public boolean isFrontendUser() {

		return (true);

	}

//      @Override
	public boolean isBackendUser() {

		return (true);

	}

	//~--- set methods ----------------------------------------------------

//      @Override
	public void setPassword(final String passwordValue) {

		// not supported
	}

//      @Override
	public void setRealName(final String realName) {

		// not supported
	}

	@Override
	public void setBlocked(final Boolean blocked) {

		// not supported
	}

//      @Override
	public void setConfirmationKey(String value) throws FrameworkException {}

//      @Override
	public void setFrontendUser(boolean isFrontendUser) throws FrameworkException {}

//      @Override
	public void setBackendUser(boolean isBackendUser) throws FrameworkException {}

	@Override
	public void setProperty(String key, Object value) throws FrameworkException {}

	@Override
	public void setProperty(PropertyKey key, Object value) throws FrameworkException {}
}
