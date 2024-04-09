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
package org.structr.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.structr.common.helper.ValidationHelper;

/**
 * Controls access to REST resources.
 *
 * Objects of this class act as a doorkeeper for REST resources
 * that match the signature string in the 'signature' field.
 * <p>
 * A ResourceAccess object defines access granted
 * <ul>
 * <li>to everyone (public)
 * <li>to authenticated principals
 * <li>to invidual principals (when connected to a {link @Principal} node
 * </ul>
 *
 * <p>'flags' is a sum of any of the following values:
 *
 *  FORBIDDEN             = 0
 *  AUTH_USER_GET         = 1
 *  AUTH_USER_PUT         = 2
 *  AUTH_USER_POST        = 4
 *  AUTH_USER_DELETE      = 8
 *  NON_AUTH_USER_GET     = 16
 *  NON_AUTH_USER_PUT     = 32
 *  NON_AUTH_USER_POST    = 64
 *  NON_AUTH_USER_DELETE  = 128
 *  AUTH_USER_OPTIONS     = 256
 *  NON_AUTH_USER_OPTIONS = 512
 *
 *
 *
 */
public class ResourceAccess extends AbstractNode {

	private static final Map<String, List<ResourceAccess>> grantCache = new ConcurrentHashMap<>();
	private static final Logger logger                                = LoggerFactory.getLogger(ResourceAccess.class.getName());

	public static final Property<String>               signature          = new StringProperty("signature").indexed();
	public static final Property<Long>                 flags              = new LongProperty("flags").indexed();
	public static final Property<Boolean>              isResourceAccess   = new ConstantBooleanProperty("isResourceAccess", true);

	public static final View uiView = new View(ResourceAccess.class, PropertyView.Ui,
		signature, flags, isResourceAccess
	);

	public static final View publicView = new View(ResourceAccess.class, PropertyView.Public,
		signature, flags, isResourceAccess
	);

	// non-static members
	private String cachedResourceSignature = null;
	private Long cachedFlags               = null;

	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}

	public static boolean hasFlag(long flag, long flags) {
		return (flags & flag) == flag;
	}

	public void setFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		setProperty(ResourceAccess.flags, getFlags() | flag);
	}

	public void clearFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		setProperty(ResourceAccess.flags, getFlags() & ~flag);
	}

	public long getFlags() {

		if (cachedFlags == null) {
			cachedFlags = getProperty(ResourceAccess.flags);
		}

		if (cachedFlags != null) {
			return cachedFlags;
		}

		return 0;
	}

	public String getResourceSignature() {

		if (cachedResourceSignature == null) {
			cachedResourceSignature = getProperty(ResourceAccess.signature);
		}

		return cachedResourceSignature;
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) {
		grantCache.clear();
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this,  ResourceAccess.signature, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, ResourceAccess.flags, errorBuffer);

		return valid;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		grantCache.clear();
	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {
		grantCache.clear();
	}

	public static List<ResourceAccess> findGrants(final SecurityContext securityContext, final String signature) throws FrameworkException {

		List<ResourceAccess> grants = grantCache.get(signature);
		if (grants == null) {

			// Ignore securityContext here (so we can cache all grants for a signature independent of a user)
			grants = StructrApp.getInstance().nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getAsList();
			if (!grants.isEmpty()) {

				grantCache.put(signature, grants);
			}
		}

		return grants;
	}

	public static void clearCache() {
		grantCache.clear();
	}
}
