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
package org.structr.web.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;

public interface ComponentConfiguration extends NodeInterface {

	void setDataSource(final NodeInterface dataSourceNode) throws FrameworkException;
	DataSource getDataSource();

	void setComponentType(final String componentType) throws FrameworkException;
	void setItemType(final String itemType) throws FrameworkException;
	void setDimensions(final Integer dimensions) throws FrameworkException;
	void setIsComponentRoot(boolean b) throws FrameworkException;

	Boolean isComponentRoot();
	String getComponentType();
	String getItemType();
	String getRepeaterType();
	Integer getDimensions();
	Integer getColumns();
	String getDisplayMode();
	String getFieldSet();
	String getSaveMode();
	String getRole();
	String getReloadBehaviour();
	Boolean showLabels();
}
