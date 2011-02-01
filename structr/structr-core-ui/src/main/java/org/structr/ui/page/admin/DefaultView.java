/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.Panel;
import org.apache.click.util.Bindable;
import org.apache.click.util.ClickUtils;
import org.structr.core.entity.Image;

/**
 *
 * @author amorgner
 */
public class DefaultView extends Nodes {

    @Bindable
    protected Panel renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");
    @Bindable
    protected String externalViewUrl;
    @Bindable
    protected String localViewUrl;
    @Bindable
    protected String rendition;
    @Bindable
    protected String source;
    // use template for backend pages

    @Override
    public String getTemplate() {
        return "/admin-view-template.htm";
    }

    @Override
    public void onRender() {

        super.onRender();

        externalViewUrl = node.getNodeURL(user, contextPath);
        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(
                node.getNodePath(user).replace("&", "%26")));

        if (!(node instanceof Image)) {
            // render node's default view
            StringBuilder out = new StringBuilder();
            node.renderView(out, node, null, null, user);

            // provide rendition's source
            rendition = out.toString();
            source = ClickUtils.escapeHtml(rendition);
        } else {
            source = "Cannot show source of Image";
        }

    }
}
