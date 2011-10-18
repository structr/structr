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
package org.structr.ui.page.admin;

import org.apache.click.Page;
import org.structr.common.AccessMode;
import org.structr.ui.page.StructrPage;

/**
 * This page is a router, forwarding requests to the right edit page
 * depending on parameters like e.g. node id.
 * 
 * Usually, edit requests are given in the form
 * <p>
 *      /admin/edit.htm?nodeId=123
 * </p>
 * If the node with the id 123 is a Page, the request will be forwarded to
 * <p>
 *      /admin/edit-page.htm?nodeId=123
 * </p>
 * @author amorgner
 */
public class Edit extends StructrPage {

	public Edit() {
		super();

		securityContext.setAccessMode(AccessMode.Backend);

	}

	@Override
	public void onInit() {

		super.onInit();

		Class<? extends Page> pageClass = getRedirectPage(node);
		if(pageClass == null) {
			pageClass = DefaultEdit.class;
		}
		StructrPage editPage = (StructrPage)getContext().createPage(pageClass);

		setForward(editPage);

	}
}
