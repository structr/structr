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
package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.DataField;

import java.util.List;
import java.util.Map;

public interface DataSource extends NodeInterface {

	Iterable<GraphObject> getValues(final SecurityContext securityContext) throws FrameworkException;
	Map<String, DataField> getFields(final SecurityContext securityContext) throws FrameworkException;
	Map<String, List<String>> getFieldSets(final SecurityContext securityContext) throws FrameworkException;
	List<String> getFieldSet(final SecurityContext securityContext, String name) throws FrameworkException;
	String getSelectedId(final ActionContext actionContext) throws FrameworkException;
	Object getSelectedValue(final ActionContext actionContext) throws FrameworkException;
	Object getCurrentValue(final ActionContext actionContext) throws FrameworkException;
	String getDataType(final SecurityContext securityContext) throws FrameworkException;
	String getChannel();
	String getDataKey();

	DataProvider getDataProvider();

	Object evaluate(final ActionContext actionContext, final String key) throws FrameworkException;
}
