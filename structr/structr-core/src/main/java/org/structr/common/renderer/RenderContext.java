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

package org.structr.common.renderer;

/**
 * <p>Enumeration for a list of possible rendering contexts.</p>
 * 
 * <ul>
 * <li>AsSubnode: node is rendered as a subnode
 * <li>AsTopNode: node is the first (top) node in a rendering process
 * <li>AsSoleNode: node is the only node to be rendered
 * </ul>
 * 
 * @author axel
 */
public enum RenderContext {
    
    AsSubnode,
    AsTopNode,
    AsSoleNode;
    
}
