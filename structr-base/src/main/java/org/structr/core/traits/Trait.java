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
package org.structr.core.traits;

import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.ComposableOperation;
import org.structr.core.traits.operations.OverwritableOperation;

import java.util.Set;

/**
 * A named collection of properties and methods with a
 * factory that can instantiate implementations.
 */
public interface Trait {

	boolean hasKey(final String name);
	PropertyKey key(final String name);

	Set<PropertyKey> getPropertyKeys(final String view);

	Set<ComposableOperation> getComposableOperations();
	Set<OverwritableOperation> getOverwritableOperations();
	Set<PropertyKey> getPropertyKeys();
}
