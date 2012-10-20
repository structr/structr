/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Set;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class TreeNode {

	private Set<TreeNode> children = new LinkedHashSet<TreeNode>();
	private AbstractNode data      = null;
	private int depth              = -1;
	private TreeNode parent        = null;

	//~--- constructors ---------------------------------------------------

	public TreeNode(AbstractNode data) {
		this.data = data;
	}

	//~--- methods --------------------------------------------------------

	public void addChild(TreeNode treeNode) {
		children.add(treeNode);
	}

	public int depth() {
		return depth;
	}

	public void depth(final int depth) {
		this.depth = depth;
	}

	//~--- get methods ----------------------------------------------------

	public AbstractNode getData() {
		return data;
	}

	public TreeNode getParent() {
		return parent;
	}

	public Set<TreeNode> getChildren() {
		return children;
	}

	public TreeNode getNode(final String uuid) {

		if ((data != null) && data.getStringProperty(AbstractNode.uuid).equals(uuid)) {

			return this;

		}

		for (TreeNode treeNode : getChildren()) {

			TreeNode found = treeNode.getNode(uuid);
			if (found != null) return found;

		}

		return null;
	}
	
	//~--- set methods ----------------------------------------------------

	public void setParent(final TreeNode parent) {
		this.parent = parent;
	}
}
