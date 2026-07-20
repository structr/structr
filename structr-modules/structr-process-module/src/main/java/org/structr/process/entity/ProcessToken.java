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

/**
 * Typed view of a {@code ProcessToken} node. Obtain via
 * {@code node.as(ProcessToken.class)}.
 */
public interface ProcessToken extends NodeInterface {

	String getStatus();
	void setStatus(String status) throws FrameworkException;

	boolean isActive();
	boolean isWaiting();
	boolean isCompleted();

	void markActive() throws FrameworkException;
	void markWaiting() throws FrameworkException;
	void markCompleted() throws FrameworkException;

	/** The element this token currently sits on. */
	NodeInterface getAtElement();
	void setAtElement(NodeInterface element) throws FrameworkException;

	ProcessInstance getProcessInstance();
	void setProcessInstance(NodeInterface instance) throws FrameworkException;
}
