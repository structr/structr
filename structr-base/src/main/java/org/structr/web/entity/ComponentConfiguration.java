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

import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

public interface ComponentConfiguration extends NodeInterface {

	DOMNode getComponent();
	DataAdapter getDataAdapter();
	Channel<GraphObject> getDataSource() throws FrameworkException;
	ChannelInput getChannelInput(final RenderContext renderContext) throws FrameworkException;

	String getDataSourceName() throws FrameworkException;
	String getSelectionChannel() throws FrameworkException;
	Integer getColumns();
	String getDisplayMode();
	String getFieldSet();
	String getSaveMode();
	String getRole();
	String getReloadBehaviour();
	String getTransform();
	String getExpectedDataType();
	Boolean showLabels();
	String getBindingMode();

	/**
	 * @return {@code true} iff this component's data binding is owned by a
	 * process-side declaration (the BPMN UserTask's subject type). When
	 * {@code false} (the default), the UI designer owns the dataSource and
	 * field configuration directly. See the process / UI separation-of-
	 * concerns pillar for the design rationale.
	 */
	default boolean isProcessBound() {

		return "processBound".equals(getBindingMode());
	}

	void setFieldSet(final String s) throws FrameworkException;
	void updateFieldSetForChildren() throws FrameworkException;

	int getPageSize();
	int getPaginationWindowSize();

	void checkCompatibility() throws FrameworkException;

}
