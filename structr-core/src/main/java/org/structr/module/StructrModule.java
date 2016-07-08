/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.module;

import java.util.Set;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.schema.action.Actions;

/**
 */
public interface StructrModule {

	/**
	 * Called when the module is loaded.
	 */
	void onLoad();

	/**
	 * Returns the name of this module, with an optional version number.
	 *
	 * @return the name of this module
	 */
	String getName();

	/**
	 * Returns a set of module names (as returned by getName()) of modules
	 * this module depends on, or null if no dependencies exist.
	 *
	 * @return a set of module names or null
	 */
	Set<String> getDependencies();

	/**
	 * Returns the set of feature keys this module provides, or null if
	 * not applicable.
	 *
	 * @return a set of feature names or null
	 */
	Set<String> getFeatures();


	void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf);
	void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf);
	void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type);
	Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode);
}
