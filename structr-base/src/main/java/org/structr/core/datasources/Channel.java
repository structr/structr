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
package org.structr.core.datasources;

import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.Map;

/**
 * We need an interface that does not extend NodeInterface to be able
 * to provide both graph-based and non-graph implementations of the
 * same interface for CurrentDataSource and SchemaNode etc.
 *
 * Note: all data sources that implement this interface must resolve
 * the strings "values", "dataType" and "currentValue" to the
 * correct values in their evaluate() method.
 */
public interface Channel {

	Iterable<GraphObject> getValues(final RenderContext renderContext, final ChannelInput input) throws FrameworkException;
	Map<String, FieldDefinition> getFields(final RenderContext renderContext) throws FrameworkException;

	String getDataType(final RenderContext renderContext) throws FrameworkException;
	String getName();

	Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException;
}
