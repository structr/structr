/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import org.structr.common.PropertyView;

/**
 * Shadow document.
 * 
 * The sole purpose of this class is to have a node to append reused elements
 * (aka) components to.
 *
 * @author Axel Morgner
 */
public class ShadowDocument extends Page {
	
	public static final org.structr.common.View publicView = new org.structr.common.View(ShadowDocument.class, PropertyView.Public, type, name, id);

	public ShadowDocument() {
		
	}

}
