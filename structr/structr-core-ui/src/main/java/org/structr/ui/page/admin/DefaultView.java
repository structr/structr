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

import org.apache.click.util.ClickUtils;
import org.structr.core.entity.Image;

/**
 *
 * @author amorgner
 */
public class DefaultView extends Nodes {

//    @Bindable
//    protected Panel renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");
//    @Bindable
//    protected String externalViewUrl;
//    @Bindable
//    protected String localViewUrl;
//    @Bindable
//    protected String rendition;
//    @Bindable
//    protected String source;
    // use template for backend pages

    @Override
    public String getTemplate() {
        return "/admin-view-template.htm";
    }

    @Override
    public void onRender() {

        super.onRender();

        externalViewUrl = node.getNodeURL(contextPath);
        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(
                node.getNodePath().replace("&", "%26")));

        if (!(node instanceof Image)) {
            // render node's default view
            StringBuilder out = new StringBuilder();
            node.renderView(out, node, null, null);

            // provide rendition's source
            rendition = out.toString();
            source = ClickUtils.escapeHtml(rendition);
        } else {
            source = "Cannot show source of Image";
        }

    }
}
