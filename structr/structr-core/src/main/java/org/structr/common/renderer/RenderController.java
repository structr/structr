/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
 * By implementing this interface, nodes can define how they behave in a
 * given rendering context.
 * 
 * @author axel
 */
public interface RenderController {
    
    /**
     * Return an appropriate boolean value to indicate if rendering this
     * node is allowed in the given context.
     * 
     * @param context
     * @return 
     */
    public boolean renderingAllowed(final RenderContext context);   
    
}
