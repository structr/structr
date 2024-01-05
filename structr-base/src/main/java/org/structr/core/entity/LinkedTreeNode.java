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
import org.structr.core.property.PropertyKey;

import java.util.List;
import java.util.Set;

/**
 * Abstract base class for a multi-dimensional ordered tree datastructure.
 */
public interface LinkedTreeNode<T extends NodeInterface> extends LinkedListNode<T> {

	public <R extends Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> Class<R> getChildLinkType();
	public PropertyKey<Integer> getPositionProperty();

	public T treeGetParent();
	public void treeAppendChild(final T childElement) throws FrameworkException;
	public void treeInsertBefore(final T newChild, final T refChild) throws FrameworkException;
	public void treeInsertAfter(final T newChild, final T refChild) throws FrameworkException;
	public void treeRemoveChild(final T childToRemove) throws FrameworkException;
	public void treeReplaceChild(final T newChild, final T oldChild) throws FrameworkException;
	public T treeGetFirstChild();
	public T treeGetLastChild();
	public T treeGetChild(final int position);
	public int treeGetChildPosition(final T child);
	public List<T> treeGetChildren();
	public int treeGetChildCount();
	public <R extends Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> List<R> treeGetChildRelationships();

	public Set<T> getAllChildNodes();
}
