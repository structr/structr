/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.FrameworkMethod;

/**
 * Overrides what happens when several properties are written at once, as REST and the websocket do.
 *
 * The default implementation writes each entry through PropertyKey.setProperty() directly, so it does
 * NOT go through {@link SetProperty}. The two are separate paths to the same end, and code that must see
 * every write of a property belongs on the PropertyKey rather than on either of them.
 */
public abstract class SetProperties extends FrameworkMethod<SetProperties> {

	public abstract void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;
}
