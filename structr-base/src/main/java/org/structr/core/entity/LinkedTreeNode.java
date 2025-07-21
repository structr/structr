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
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.List;
import java.util.Set;

/**
 * Abstract base class for a multi-dimensional ordered tree datastructure.
 */
public interface LinkedTreeNode {

	String getChildLinkType();
	PropertyKey<Integer> getPositionProperty();
	NodeInterface treeGetParent();
	void treeAppendChild(final NodeInterface childElement) throws FrameworkException;
	void treeInsertBefore(final NodeInterface newChild, final NodeInterface refChild) throws FrameworkException;
	void treeInsertAfter(final NodeInterface newChild, final NodeInterface refChild) throws FrameworkException;
	void treeRemoveChild(final NodeInterface childToRemove) throws FrameworkException;
	void treeReplaceChild(final NodeInterface newChild, final NodeInterface oldChild) throws FrameworkException;
	NodeInterface treeGetFirstChild();
	NodeInterface treeGetLastChild();
	NodeInterface treeGetChild(final int position);
	int treeGetChildPosition(final NodeInterface child);
	List<NodeInterface> treeGetChildren();
	int treeGetChildCount();
	List<RelationshipInterface> treeGetChildRelationships();
	void ensureCorrectChildPositions() throws FrameworkException;
	void linkChildren(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException;
	void linkChildren(final NodeInterface startNode, final NodeInterface endNode, final PropertyMap properties) throws FrameworkException;
	void unlinkChildren(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException;
	Set<NodeInterface> getAllChildNodes();
}
