/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

import java.util.Date;

/**
 * Typed view of a {@code ProcessTimer} node. Obtain via
 * {@code node.as(ProcessTimer.class)}.
 */
public interface ProcessTimer extends NodeInterface {

	String getStatus();
	void setStatus(String status) throws FrameworkException;
	boolean isPending();

	String getTimerType();

	Boolean getCancelActivity();

	ProcessInstance getInstance();
	ProcessToken getToken();
	BpmnElement getElement();

	void setFiredAt(Date firedAt) throws FrameworkException;
	void setErrorMessage(String message) throws FrameworkException;
}
