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

import ch.qos.logback.core.joran.node.ComponentNode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.Channel;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.ComponentConfiguration;

import java.util.Map;

public interface DataAdapterField extends NodeInterface {

	String getRenderTemplate();
	String getEditTemplate();
	String getValue();
	String getDataType();
	String getLabel();
	String getSortKey();
	Boolean isSearchable();
	Integer getRows();
	Integer getColumns();
	String getColumnDataSource();
	Object getColumnKey();

	Map<String, Object> getConfig();

	void setConfig(final Map<String, Object> detailConfig) throws FrameworkException;

}
