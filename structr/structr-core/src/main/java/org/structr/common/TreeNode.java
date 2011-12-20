/*
 *  Copyright (C) 2011 Axel Morgner
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

import java.util.LinkedList;
import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class TreeNode {

	private List<TreeNode> children = new LinkedList<TreeNode>();
	private AbstractNode data       = null;
	private int depth               = -1;
	private TreeNode parent         = null;

	//~--- constructors ---------------------------------------------------

	public TreeNode(TreeNode parent, AbstractNode data) {

		this.parent = parent;
		this.data   = data;
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

	public List<TreeNode> getChildren() {
		return children;
	}
}
