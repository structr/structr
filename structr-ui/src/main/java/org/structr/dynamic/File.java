/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.dynamic;

import java.lang.reflect.Method;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.FileBase;
import org.structr.schema.SchemaService;
import org.structr.schema.compiler.NodeExtender;

/**
 * Base class for all File entities. This class is intended to be overridden
 * by a dynamic class with the exact same FQCN. The fact that the FQCN is
 * exactly the same causes the dynamic class not to be visible to the rest
 * of the system, hence the callback methods must be called on the helper
 * class using reflection. This will be significantly slower that "real"
 * dynamic types, so you should avoid using dynamic properties and save
 * actions on the File class directly.
 *
 *
 */
public class File extends FileBase {

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("File", FileBase.class.getName());
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		try {

			final Class helper  = NodeExtender.getClass("org.structr.dynamic._FileHelper");
			final Method method = helper.getDeclaredMethod("isValid", AbstractNode.class, ErrorBuffer.class);
			final Object result = method.invoke(null, this, errorBuffer);

			if (result instanceof Boolean) {

				return ((Boolean)result) && super.isValid(errorBuffer);

			}

		} catch (Throwable t) { }

		return super.isValid(errorBuffer);
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		try {

			final Class helper  = NodeExtender.getClass("org.structr.dynamic._FileHelper");
			final Method method = helper.getDeclaredMethod("onCreation", AbstractNode.class, SecurityContext.class, ErrorBuffer.class);
			final Object result = method.invoke(null, this, securityContext, errorBuffer);

			if (result instanceof Boolean) {

				return ((Boolean)result) && super.onCreation(securityContext, errorBuffer);

			}

		} catch (Throwable t) { }

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		try {

			final Class helper  = NodeExtender.getClass("org.structr.dynamic._FileHelper");
			final Method method = helper.getDeclaredMethod("onModification", AbstractNode.class, SecurityContext.class, ErrorBuffer.class);
			final Object result = method.invoke(null, this, securityContext, errorBuffer);

			if (result instanceof Boolean) {

				return ((Boolean)result) && super.onModification(securityContext, errorBuffer);

			}

		} catch (Throwable t) { }

		return super.onModification(securityContext, errorBuffer);
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		try {

			final Class helper  = NodeExtender.getClass("org.structr.dynamic._FileHelper");
			final Method method = helper.getDeclaredMethod("onDeletion", AbstractNode.class, SecurityContext.class, ErrorBuffer.class, PropertyMap.class);
			final Object result = method.invoke(null, this, securityContext, errorBuffer, properties);

			if (result instanceof Boolean) {

				return ((Boolean)result) && super.onDeletion(securityContext, errorBuffer, properties);

			}

		} catch (Throwable t) { }

		return super.onDeletion(securityContext, errorBuffer, properties);
	}
}
