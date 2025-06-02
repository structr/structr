/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;

/**
 * Abstract base class for a multi-dimensional linked list data structure.
 */
public interface LinkedListNode extends NodeInterface {

	String getSiblingLinkType();
	NodeInterface listGetPrevious(final NodeInterface currentElement);
	NodeInterface listGetNext(final NodeInterface currentElement);
	void listInsertBefore(final NodeInterface currentElement, final NodeInterface newElement) throws FrameworkException;
	void listInsertAfter(final NodeInterface currentElement, final NodeInterface newElement) throws FrameworkException;
	void listRemove(final NodeInterface currentElement) throws FrameworkException;
	void linkSiblings(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException;
	void linkSiblings(final NodeInterface startNode, final NodeInterface endNode, final PropertyMap properties) throws FrameworkException;
	void unlinkSiblings(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException;
}
