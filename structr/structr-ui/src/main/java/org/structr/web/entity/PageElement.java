/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.entity;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.web.common.PageHelper;

/**
 * Bean class for a page child element
 *
 * @author Axel Morgner
 */
public abstract class PageElement extends AbstractNode implements Element {
	
	private static final Logger logger                                             = Logger.getLogger(PageElement.class.getName());

	/**
	 * Do necessary updates on all containing pages
	 * 
	 * @throws FrameworkException 
	 */
	private void updatePages(SecurityContext securityContext) throws FrameworkException {
		
		List<Page> pages = PageHelper.getPages(securityContext, this);
		
		for (Page page : pages) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();
			
		}
	}
	
	@Override
	public void afterModification(SecurityContext securityContext) {
		try {
			
			updatePages(securityContext);
			
		} catch (FrameworkException ex) {
			logger.log(Level.WARNING, "Updating page versions failed", ex);
		}
		
	}

}
