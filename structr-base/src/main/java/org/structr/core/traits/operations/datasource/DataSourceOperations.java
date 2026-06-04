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
package org.structr.core.traits.operations.datasource;

import org.structr.api.util.ResultStream;
import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DataSource;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.Map;

public abstract class DataSourceOperations<T> extends FrameworkMethod<DataSourceOperations<T>> {

	public abstract ResultStream<T> getValues(final ActionContext actionContext, final DataSource provider, final ChannelInput input) throws FrameworkException;
	public abstract Map<String, FieldDefinition> getFields(final ActionContext actionContext, final DataSource provider) throws FrameworkException;
	public abstract String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException;
	public abstract int getDimension(final DataSource provider);
}
