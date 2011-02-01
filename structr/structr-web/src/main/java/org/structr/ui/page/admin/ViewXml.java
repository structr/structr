/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.util.Bindable;
import org.apache.click.util.ClickUtils;
import org.structr.core.entity.web.Page;
import org.structr.core.entity.web.Xml;
import org.structr.ui.page.admin.DefaultView;

/**
 * View xml node.
 * 
 * @author amorgner
 */
public class ViewXml extends DefaultView {

    @Bindable
    protected String xml;

    /**
     * @see Page#onRender()
     */
    @Override
    public void onRender() {

        super.onRender();
        Xml node = (Xml) getNodeByIdOrPath(getNodeId());
        xml = ClickUtils.escapeHtml(node.getXml());

    }
}
