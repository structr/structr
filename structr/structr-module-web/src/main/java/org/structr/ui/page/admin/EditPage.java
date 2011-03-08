/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.click.control.FieldSet;
import org.structr.core.entity.AbstractNode;

import org.apache.click.control.Option;
import org.apache.click.control.Panel;
import org.apache.click.control.Select;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.util.ClickUtils;
import org.structr.core.search.SearchOperator;
import org.structr.core.entity.web.Page;
import org.structr.core.Services;
import org.structr.core.entity.Template;
import org.structr.core.search.TextualSearchAttribute;
import org.structr.core.search.SearchNodeCommand;

/**
 * Edit a page
 * 
 * @author amorgner
 */
public class EditPage extends DefaultEdit {

    protected Page page;
    protected Select templateSelect = new Select(Page.TEMPLATE_KEY);

    public EditPage() {

        super();

        FieldSet templateFields = new FieldSet("Template");
        templateFields.add(templateSelect);

        editPropertiesForm.add(templateFields);
    }

    @Override
    public void onInit() {

        super.onInit();

        page = (Page) node;

        if (page == null) {
            return;
        }

        final Template templateNode = page.getTemplate(user);

        templateSelect.setDataProvider(new DataProvider() {

            @Override
            public List<Option> getData() {
                List<Option> options = new ArrayList<Option>();
                List<AbstractNode> nodes = null;
                if (templateNode != null) {
                    nodes = templateNode.getSiblingNodes(user);
                } else {
                    List<TextualSearchAttribute> searchAttrs = new ArrayList<TextualSearchAttribute>();
                    searchAttrs.add(new TextualSearchAttribute(AbstractNode.TYPE_KEY, Template.class.getSimpleName(), SearchOperator.OR));
                    nodes = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(user, null, false, false, searchAttrs);
                }
                if (nodes != null) {
                    Collections.sort(nodes);
                    options.add(Option.EMPTY_OPTION);
                    for (AbstractNode n : nodes) {
                        if (n instanceof Template) {
                            Option opt = new Option(n.getId(), n.getName());
                            options.add(opt);
                        }
                    }
                }
                return options;
            }
        });

        externalViewUrl = node.getNodeURL(user, contextPath);
        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(node.getNodePath(user).replace("&", "%26")));

        // render node's default view
        StringBuilder out = new StringBuilder();
        node.renderView(out, node, null, null, user);
        rendition = out.toString();
//        // only pages and files may be rendered
//        if (node instanceof org.structr.core.entity.web.Page || node instanceof File) {
//
//            externalViewUrl = node.getNodeURL(user, contextPath);
//            //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
//            localViewUrl = getContext().getRequest().getContextPath().concat(
//                    "/view".concat(
//                    node.getNodePath(user).replace("&", "%26")));
//
//            if (node instanceof org.structr.core.entity.web.Page) {
//
//                // render node's default view
//                StringBuilder out = new StringBuilder();
//                node.renderView(out, node, null, null, user);
//                rendition = out.toString();
//
//            } else {
//
//                // FIXME: find a good solution for file download
////                ByteArrayOutputStream out = new ByteArrayOutputStream();
////                node.renderDirect(out, rootNode, redirect, editNodeId, user);
////                rendition = out.toString();
//            }
            // provide rendition's source
            source = ClickUtils.escapeHtml(rendition);

            renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");
//        }
    }
}
