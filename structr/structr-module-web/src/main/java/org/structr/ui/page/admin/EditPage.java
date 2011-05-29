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

import org.apache.click.control.Panel;
import org.apache.click.util.ClickUtils;
import org.structr.core.entity.web.Page;

/**
 * Edit a page
 * 
 * @author amorgner
 */
public class EditPage extends DefaultEdit {

    protected Page page;

    public EditPage() {
        super();
    }

    @Override
    public void onInit() {

        super.onInit();

        if (node instanceof Page) {

            page = (Page) node;

            externalViewUrl = page.getNodeURL(contextPath);
            //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
            localViewUrl = getContext().getRequest().getContextPath().concat(
                    "/view".concat(page.getNodePath().replace("&", "%26")));

            // render node's default view
            StringBuilder out = new StringBuilder();
            node.renderView(out, page, null, null);
            rendition = out.toString();
            // provide rendition's source
            source = ClickUtils.escapeHtml(rendition);

            renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");

        }
    }
}
