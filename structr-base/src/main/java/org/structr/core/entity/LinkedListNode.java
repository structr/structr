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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;

/**
 * Abstract base class for a linked list data structure.
 *
 * @param <T>
 */
public interface LinkedListNode<T extends NodeInterface> extends NodeInterface {

	public <R extends Relation<T, T, OneStartpoint<T>, OneEndpoint<T>>> Class<R> getSiblingLinkType();

	public  T listGetPrevious(final T currentElement);
	public T listGetNext(final T currentElement);
	public void listInsertBefore(final T currentElement, final T newElement) throws FrameworkException;
	public void listInsertAfter(final T currentElement, final T newElement) throws FrameworkException;
	public void listRemove(final T currentElement) throws FrameworkException;
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException;
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode, final PropertyMap properties) throws FrameworkException;
}
