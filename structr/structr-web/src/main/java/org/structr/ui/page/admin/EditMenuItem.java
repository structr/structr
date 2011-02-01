/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.click.control.FieldSet;
import org.structr.core.entity.StructrNode;

import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.dataprovider.DataProvider;
import org.structr.common.SearchOperator;
import org.structr.core.entity.web.Page;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.web.MenuItem;
import org.structr.core.entity.Template;
import org.structr.core.node.SearchAttribute;
import org.structr.core.node.SearchNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.ui.page.admin.DefaultEdit;
import org.structr.ui.page.admin.DefaultEdit;

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
                List<StructrNode> nodes = null;
                if (linkTargetNode != null) {
                    nodes = linkTargetNode.getSiblingNodes(user);
                } else {
                    Command searchNode = Services.createCommand(SearchNodeCommand.class);

                    List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();
                    searchAttrs.add(new SearchAttribute(StructrNode.TYPE_KEY, Page.class.getSimpleName(), SearchOperator.OR));
                    nodes = (List<StructrNode>) searchNode.execute(null, null, true, false, searchAttrs);
                }
                if (nodes != null) {
                    Collections.sort(nodes);
                    options.add(Option.EMPTY_OPTION);
                    for (StructrNode n : nodes) {
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
//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//                StructrNode s = getNodeByIdOrPath(getNodeId());
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
//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        StructrNode s = null;
//        s = (StructrNode) transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//
//                Command createNode = Services.createCommand(CreateNodeCommand.class);
//                Command linkNode = Services.createCommand(CreateRelationshipCommand.class);
//
//                // create a new template node
//                StructrNode newTemplate = (StructrNode) createNode.execute(
//                        new NodeAttribute(StructrNode.TYPE_KEY, Template.class.getSimpleName()),
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
