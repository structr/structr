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
import org.apache.click.control.Select;
import org.apache.click.dataprovider.DataProvider;
import org.structr.common.SearchOperator;
import org.structr.core.entity.web.Page;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.web.MenuItem;
import org.structr.core.search.SingleSearchAttribute;
import org.structr.core.search.SearchNodeCommand;

/**
 * Edit text.
 *
 * @author amorgner
 */
public class EditMenuItem extends DefaultEdit {

    protected MenuItem menuItem;
    protected Select linkTargetSelect = new Select(MenuItem.LINK_TARGET_KEY);


    public EditMenuItem() {

        super();

        FieldSet linkTargetFields = new FieldSet("Link Target");
        linkTargetFields.add(linkTargetSelect);
        editPropertiesForm.add(linkTargetFields);
    }

    @Override
    public void onInit() {

        super.onInit();

        menuItem = (MenuItem) node;

        final Page linkTargetNode = menuItem.getLinkedPage();

        linkTargetSelect.setDataProvider(new DataProvider() {

            @Override
            public List<Option> getData() {
                List<Option> options = new ArrayList<Option>();
                List<AbstractNode> nodes = null;
                if (linkTargetNode != null) {
                    nodes = linkTargetNode.getSiblingNodes(user);
                } else {
                    List<SingleSearchAttribute> searchAttrs = new ArrayList<SingleSearchAttribute>();
                    searchAttrs.add(new SingleSearchAttribute(AbstractNode.TYPE_KEY, Page.class.getSimpleName(), SearchOperator.OR));
                    nodes = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(user, null, false, false, searchAttrs);
                }
                if (nodes != null) {
                    Collections.sort(nodes);
                    options.add(Option.EMPTY_OPTION);
                    for (AbstractNode n : nodes) {
                        if (n instanceof Page) {
                            Option opt = new Option(n.getId(), n.getName());
                            options.add(opt);
                        }
                    }
                }
                return options;
            }
        });

    }
//
//    /**
//     * @see Page#onRender()
//     */
//    @Override
//    public void onRender() {
//
//        super.onRender();
//
//        Command transactionCommand = Services.command(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//                AbstractNode s = getNodeByIdOrPath(getNodeId());
//
//                if (editPropertiesForm.isValid()) {
//                    editPropertiesForm.copyFrom(s);
//                }
//
//                return (null);
//            }
//        });
//
//        // set node id
//        if (editPropertiesForm.isValid()) {
//            editPropertiesForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
//            editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
//        }
//
//    }
//
//    /**
//     * Add a new contact person
//     *
//     */
//    public boolean onAddTemplate() {
//
//        Command transactionCommand = Services.command(TransactionCommand.class);
//        AbstractNode s = null;
//        s = (AbstractNode) transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//
//                Command createNode = Services.command(CreateNodeCommand.class);
//                Command linkNode = Services.command(CreateRelationshipCommand.class);
//
//                // create a new template node
//                AbstractNode newTemplate = (AbstractNode) createNode.execute(
//                        new NodeAttribute(AbstractNode.TYPE_KEY, Template.class.getSimpleName()),
//                        new NodeAttribute(Template.NAME_KEY, "New Name"),
//                        user);
//
//                // link new template node to this menuItem
//                linkNode.execute(newTemplate, menuItem, RelType.LINK);
//
//                return newTemplate;
//            }
//        });
//
//
//        // avoid NullPointerException when no node was created..
//        if (s != null) {
//            okMsg = "New " + s.getType() + " node " + s.getName() + " has been created.";
//        }
//
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put(NODE_ID_KEY, String.valueOf(s.getId()));
//        parameters.put(RENDER_MODE_KEY, renderMode);
//        parameters.put(OK_MSG_KEY, okMsg);
//        parameters.put(RETURN_URL_KEY, getReturnUrl());
//
//        setRedirect(getEditPageClass(s), parameters);
//
//        return false;
//
//    }
}
