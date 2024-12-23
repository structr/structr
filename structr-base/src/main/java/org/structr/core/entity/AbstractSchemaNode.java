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

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;

import java.util.List;

public interface AbstractSchemaNode extends NodeTrait {

	Iterable<SchemaProperty> getSchemaProperties();
	Iterable<SchemaView> getSchemaViews();
	Iterable<SchemaMethod> getSchemaMethods();
	Iterable<SchemaGrant> getSchemaGrants();

	Iterable<SchemaMethod> getSchemaMethodsIncludingInheritance();
	SchemaMethod getSchemaMethod(final String name);
	List<SchemaMethod> getSchemaMethodsByName(final String name);
	SchemaProperty getSchemaProperty(final String name);
	SchemaView getSchemaView(final String name);

	SchemaNode getExtendsClass();

	String getExtendsClassInternal();
	String getImplementsInterfaces();

	String getSummary();
	String getIcon();
	String getDescription();
	String getCategory();
	String getClassName();
	String getDefaultSortKey();
	String getDefaultSortOrder();

	boolean isInterface();
	boolean isAbstract();
	boolean isBuiltinType();
	boolean changelogDisabled();
	boolean defaultVisibleToPublic();
	boolean defaultVisibleToAuth();
	boolean includeInOpenAPI();

	String[] getTags();

	void setExtendsClass(final SchemaNode schemaNode) throws FrameworkException;
}
