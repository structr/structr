/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.click.Context;
import org.apache.click.Page;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Column;
import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.PageLink;
import org.apache.click.control.Panel;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.control.TextField;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.DateField;
import org.apache.click.extras.control.FormTable;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.extras.control.PickList;
import org.apache.click.util.Bindable;
import org.apache.click.util.HtmlStringBuffer;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.Image;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.StructrNode.Title;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class DefaultEdit extends Nodes {

    /**
     * The main form for editing node parameter.
     * Child pages should just append fields to this form.
     */
    @Bindable
    protected Form editPropertiesForm = new Form("editPropertiesForm");
    @Bindable
    protected Form editVisibilityForm = new Form("editVisibilityForm");
    @Bindable
    protected Table incomingRelationshipsTable = new Table();
    protected ActionLink incomingRelsControl = incomingRelationshipsTable.getControlLink();
    @Bindable
    protected Table outgoingRelationshipsTable = new Table();
    protected ActionLink outgoingRelsControl = outgoingRelationshipsTable.getControlLink();
    @Bindable
    protected FormTable childNodesTable = new FormTable();
    @Bindable
    protected ActionLink deleteRelationshipLink = new ActionLink("Delete Relationship", this, "onDeleteRelationship");
//    @Bindable
//    protected ActionLink editNodeLink = new ActionLink("Edit", this, "onEditNode");
    @Bindable
    protected ActionLink deleteNodeLink = new ActionLink("Delete", this, "onDeleteNode");
//    @Bindable
//    protected ActionLink viewNodeLink = new ActionLink("View", this, "onViewNode");
    protected Table titlesTable = new Table(StructrNode.TITLES_KEY);
    protected FormTable securityTable = new FormTable("Security");
    @Bindable
    protected Form securityForm = new Form("securityForm");
    protected Select userSelect = new Select("selectUser", "User");
    protected PickList allowed = new PickList(StructrRelationship.ALLOWED_KEY, "Allowed");
    protected Checkbox recursive = new Checkbox("recursive");
    @Bindable
    protected Panel editPropertiesPanel;
    @Bindable
    protected Panel editRelationshipsPanel;
    @Bindable
    protected Panel editChildNodesPanel;
    @Bindable
    protected Panel editSecurityPanel;
    @Bindable
    protected Panel editVisibilityPanel;

    // use template for backend pages
    @Override
    public String getTemplate() {
        return "/admin-edit-template.htm";
    }

    public DefaultEdit() {
        super();
    }

    @Override
    public void onInit() {

        super.onInit();

        FieldSet nodeInfo = new FieldSet("Node Information");
        nodeInfo.setColumns(3);

        // add common fields
        nodeInfo.add(new TextField(StructrNode.TYPE_KEY, true));
        nodeInfo.add(new TextField(StructrNode.NAME_KEY, true));
        nodeInfo.add(new IntegerField(StructrNode.POSITION_KEY));
//        nodeInfo.add(new TextField(StructrNode.NODE_ID_KEY));

        TextField createdBy = new TextField(StructrNode.CREATED_BY_KEY);
        createdBy.setReadonly(true);
        nodeInfo.add(createdBy);

        DateField createdDate = new DateField(StructrNode.CREATED_DATE_KEY);
        createdDate.setFormatPattern(dateFormat.toPattern());
        createdDate.setShowTime(true);
        createdDate.setReadonly(true);
        nodeInfo.add(createdDate);

        DateField lastModifiedDate = new DateField(StructrNode.LAST_MODIFIED_DATE_KEY);
        lastModifiedDate.setFormatPattern(dateFormat.toPattern());
        lastModifiedDate.setShowTime(true);
        lastModifiedDate.setReadonly(true);
        nodeInfo.add(lastModifiedDate);

        titlesTable.addColumn(new Column(StructrNode.TITLE_KEY, "Title"));
        titlesTable.addColumn(new Column(Title.LOCALE_KEY, "Locale"));
        titlesTable.setClass(Table.CLASS_SIMPLE);
        nodeInfo.add(titlesTable);

        editPropertiesForm.add(nodeInfo);

        FieldSet visibilityFields = new FieldSet("Visibility");
        visibilityFields.setColumns(2);

        DateField visibilityStartDate = new DateField(StructrNode.VISIBILITY_START_DATE_KEY);
        visibilityStartDate.setFormatPattern(dateFormat.toPattern());
        visibilityStartDate.setShowTime(true);
        visibilityFields.add(visibilityStartDate);

        DateField visibilityEndDate = new DateField(StructrNode.VISIBILITY_END_DATE_KEY);
        visibilityEndDate.setFormatPattern(dateFormat.toPattern());
        visibilityEndDate.setShowTime(true);
        visibilityFields.add(visibilityEndDate);

        Checkbox hidden = new Checkbox(StructrNode.HIDDEN_KEY);
        visibilityFields.add(hidden);

        Checkbox publicCheckbox = new Checkbox(StructrNode.PUBLIC_KEY);
        visibilityFields.add(publicCheckbox);

        editVisibilityForm.add(visibilityFields);
        editVisibilityForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
        editVisibilityForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        editVisibilityForm.setActionURL(editVisibilityForm.getActionURL().concat("#visibility-tab"));
        if (editVisibilityAllowed) {
            editVisibilityForm.add(new Submit("saveVisibility", " Save Visibility ", this, "onSaveVisibility"));
            editVisibilityForm.add(new Submit("saveVisibilityRecursively", " Save Visibility (including direct children) ", this, "onSaveVisibilityWithSubnodes"));
            editVisibilityForm.add(new Submit("cancel", " Cancel ", this, "onCancel"));
        }

        editVisibilityPanel = new Panel("editVisibilityPanel", "/panel/edit-visibility-panel.htm");

        editPropertiesForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
        editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        editPropertiesForm.setActionURL(editPropertiesForm.getActionURL().concat("#properties-tab"));
        if (editPropertiesAllowed) {
            editPropertiesForm.add(new Submit("saveProperties", " Save Properties ", this, "onSaveProperties"));
//            editPropertiesForm.add(new Submit("savePropertiesAndReturn", " Save and Return ", this, "onSaveAndReturn"));
            editPropertiesForm.add(new Submit("cancel", " Cancel ", this, "onCancel"));
        }
        //editPropertiesForm.add(new Submit("saveAndView", " Save And View ", this, "onSaveAndView"));
        editPropertiesPanel = new Panel("editPropertiesPanel", "/panel/edit-properties-panel.htm");

        Column nameColumn, typeColumn;
        PageLink viewRelLink = new PageLink("viewRel", DefaultEdit.class);
        LinkDecorator nameDec, iconDec;

        // ------------------ child nodes start --------------------------------

        if (node != null && node.hasChildren()) {

            Column actionColumnNodes = new Column("Actions");
            actionColumnNodes.setTextAlign("center");
            actionColumnNodes.setDecorator(new LinkDecorator(childNodesTable, new PageLink(), StructrNode.NODE_ID_KEY) {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrNode n = (StructrNode) row;
                    link = new PageLink(StructrNode.NODE_ID_KEY, getEditPageClass(n)) {

                        @Override
                        public String getHref() {
                            return super.getHref().concat("#properties-tab");
                        }
                    };

                    link.setParameter(NODE_ID_KEY, n.getId());
                    link.setImageSrc("/images/table-edit.png");

                    super.renderActionLink(buffer, link, context, row, value);

                }
            });

            typeColumn = new Column(StructrNode.TYPE_KEY);

            iconDec = new LinkDecorator(childNodesTable, new PageLink(), StructrNode.NODE_ID_KEY) {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrNode n = (StructrNode) row;
                    link = new PageLink(StructrNode.NODE_ID_KEY, getEditPageClass(n));
                    link.setParameter(NODE_ID_KEY, n.getId());

                    boolean hasThumbnail = false;

                    if (n instanceof Image) {
                        Image image = (Image) n;
                        Image thumbnail = image.getScaledImage(user, 100, 100);

                        if (thumbnail != null) {
                            String thumbnailSrc = "/view.htm?nodeId=" + thumbnail.getId();
                            link.setImageSrc(thumbnailSrc);
                            //link.setRenderLabelAndImage(true);
                            hasThumbnail = true;
                        }
                    }

                    if (!hasThumbnail) {
                        link.setImageSrc(getIconSrc(n));
                    }

                    link.setLabel(n.getName());

                    super.renderActionLink(buffer, link, context, row, value);
                }
            };
            typeColumn.setDecorator(iconDec);
            childNodesTable.addColumn(typeColumn);

            nameColumn = new Column(StructrNode.NAME_KEY);
            nameDec = new LinkDecorator(childNodesTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrNode n = (StructrNode) row;

                    PageLink pageLink = new PageLink("id", getEditPageClass(n)) {

                        @Override
                        public String getHref() {
                            return super.getHref().concat("#childnodes-tab");
                        }
                    };

                    pageLink.setParameter(NODE_ID_KEY, n.getId());
                    pageLink.setLabel(n.getName());
                    super.renderActionLink(buffer, pageLink, context, row, value);
                }
            };
            nameColumn.setDecorator(nameDec);
            childNodesTable.addColumn(nameColumn);
            childNodesTable.addColumn(new Column(StructrNode.LAST_MODIFIED_DATE_KEY));
            childNodesTable.addColumn(new Column(StructrNode.OWNER_KEY));
            childNodesTable.addColumn(new Column(StructrNode.CREATED_BY_KEY));
            childNodesTable.addColumn(new Column(StructrNode.CREATED_DATE_KEY));
            childNodesTable.addColumn(new Column(StructrNode.POSITION_KEY));
            childNodesTable.addColumn(new Column(StructrNode.PUBLIC_KEY));
            childNodesTable.addColumn(actionColumnNodes);
            childNodesTable.setSortable(true);
            childNodesTable.setShowBanner(true);
            childNodesTable.setPageSize(DEFAULT_PAGESIZE);
            childNodesTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
            childNodesTable.setClass(Table.CLASS_SIMPLE);

            editChildNodesPanel = new Panel("editChildNodesPanel", "/panel/edit-child-nodes-panel.htm");
        }
        // ------------------ child nodes end --------------------------------

        // ------------------ incoming relationships start ---------------------
        if (removeRelationshipAllowed) {

            deleteRelationshipLink.setImageSrc("/images/delete.png");
            deleteRelationshipLink.setTitle("Delete relationship");
            deleteRelationshipLink.setAttribute("onclick", "return window.confirm('Please confirm delete', value);");

            Column actionColumnIn = new Column("Action");
            actionColumnIn.setTextAlign("center");
            AbstractLink[] linksIn = new AbstractLink[]{deleteRelationshipLink};
            actionColumnIn.setDecorator(new LinkDecorator(incomingRelationshipsTable, linksIn, RELATIONSHIP_ID_KEY));
            actionColumnIn.setSortable(false);

            typeColumn = new Column(StructrNode.TYPE_KEY);
            viewRelLink = new PageLink("view", DefaultEdit.class);
//            viewRelLink = new PageLink("view", DefaultView.class);
            iconDec = new LinkDecorator(incomingRelationshipsTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrRelationship r = (StructrRelationship) row;

                    StructrNode startNode = r.getStartNode();

//                    link = new PageLink("id", getViewPageClass(startNode));
                    link = new PageLink("id", getEditPageClass(startNode));
                    link.setParameter(NODE_ID_KEY, startNode.getId());
                    link.setImageSrc(startNode.getIconSrc());

                    super.renderActionLink(buffer, link, context, row, value);

                }
            };
            typeColumn.setDecorator(iconDec);
            incomingRelationshipsTable.addColumn(typeColumn);
            nameColumn = new Column(StructrNode.NAME_KEY);
            nameDec = new LinkDecorator(incomingRelationshipsTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrRelationship r = (StructrRelationship) row;

                    StructrNode startNode = r.getStartNode();
                    PageLink pageLink = new PageLink("id", getEditPageClass(startNode)) {

                        @Override
                        public String getHref() {
                            return super.getHref().concat("#relationships-tab");
                        }
                    };

                    pageLink.setParameter(NODE_ID_KEY, startNode.getId());
                    pageLink.setLabel(startNode.getName());

                    super.renderActionLink(buffer, pageLink, context, row, value);

                }
            };
            nameColumn.setDecorator(nameDec);
            incomingRelationshipsTable.addColumn(nameColumn);

            incomingRelationshipsTable.addColumn(new Column(RELATIONSHIP_ID_KEY));
            incomingRelationshipsTable.addColumn(new Column(START_NODE_KEY));
            incomingRelationshipsTable.addColumn(new Column(END_NODE_KEY));
            incomingRelationshipsTable.addColumn(new Column(REL_TYPE_KEY));
            incomingRelationshipsTable.addColumn(actionColumnIn);
            incomingRelationshipsTable.setPageSize(DEFAULT_PAGESIZE);
            incomingRelationshipsTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
            incomingRelationshipsTable.setClass(Table.CLASS_SIMPLE);
            // ------------------ incoming relationships end ---------------------


            // ------------------ outgoing relationships start ---------------------

            Column actionColumnOut = new Column("Action");
            actionColumnOut.setTextAlign("center");
            AbstractLink[] linksOut = new AbstractLink[]{deleteRelationshipLink};
            actionColumnOut.setDecorator(new LinkDecorator(outgoingRelationshipsTable, linksOut, RELATIONSHIP_ID_KEY));
            actionColumnOut.setSortable(false);

            typeColumn = new Column(StructrNode.TYPE_KEY);

            iconDec = new LinkDecorator(outgoingRelationshipsTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrRelationship r = (StructrRelationship) row;
                    StructrNode endNode = r.getEndNode();
                    link = new PageLink("id", getEditPageClass(endNode));
                    link.setParameter(NODE_ID_KEY, endNode.getId());
                    link.setImageSrc(endNode.getIconSrc());

                    super.renderActionLink(buffer, link, context, row, value);

                }
            };
            typeColumn.setDecorator(iconDec);
            outgoingRelationshipsTable.addColumn(typeColumn);

            nameColumn = new Column(StructrNode.NAME_KEY);
            nameDec = new LinkDecorator(outgoingRelationshipsTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrRelationship r = (StructrRelationship) row;

                    StructrNode endNode = r.getEndNode();
                    PageLink pageLink = new PageLink("id", getEditPageClass(endNode)) {

                        @Override
                        public String getHref() {
                            return super.getHref().concat("#relationships-tab");
                        }
                    };

                    pageLink.setParameter(NODE_ID_KEY, endNode.getId());
                    pageLink.setLabel(endNode.getName());

                    super.renderActionLink(buffer, pageLink, context, row, value);

                }
            };
            nameColumn.setDecorator(nameDec);
            outgoingRelationshipsTable.addColumn(nameColumn);
            outgoingRelationshipsTable.addColumn(new Column(RELATIONSHIP_ID_KEY));
            outgoingRelationshipsTable.addColumn(new Column(START_NODE_KEY));
            outgoingRelationshipsTable.addColumn(new Column(END_NODE_KEY));
            outgoingRelationshipsTable.addColumn(new Column(REL_TYPE_KEY));
            outgoingRelationshipsTable.addColumn(actionColumnOut);
            outgoingRelationshipsTable.setPageSize(DEFAULT_PAGESIZE);
            outgoingRelationshipsTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
            outgoingRelationshipsTable.setClass(Table.CLASS_SIMPLE);

            editRelationshipsPanel = new Panel("editRelationshipsPanel", "/panel/edit-relationships-panel.htm");
        }

        // ------------------ outgoing relationships end ---------------------

        // ------------------ security begin ---------------------

        if (accessControlAllowed) {

            nameColumn = new Column(StructrNode.NAME_KEY);
            nameDec = new LinkDecorator(securityTable, viewRelLink, "id") {

                @Override
                protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                    StructrRelationship r = (StructrRelationship) row;

                    StructrNode startNode = r.getStartNode();
                    PageLink pageLink = new PageLink("id", getEditPageClass(startNode)) {

                        @Override
                        public String getHref() {
                            return super.getHref().concat("#security-tab");
                        }
                    };

                    pageLink.setParameter(NODE_ID_KEY, startNode.getId());
                    pageLink.setLabel(startNode.getName());

                    super.renderActionLink(buffer, pageLink, context, row, value);

                }
            };
            nameColumn.setDecorator(nameDec);
            securityTable.addColumn(nameColumn);
            securityTable.addColumn(new Column(StructrRelationship.ALLOWED_KEY));
            securityTable.setClass(Table.CLASS_SIMPLE);

            securityForm.add(securityTable);


            FieldSet setPermissionFields = new FieldSet("Set Permissions");

            setPermissionFields.add(userSelect);
            setPermissionFields.add(recursive);
            List<Option> optionList = new ArrayList<Option>();
            Option readOption = new Option(StructrRelationship.READ_KEY, "Read");
            optionList.add(readOption);
            Option showTreeOption = new Option(StructrRelationship.SHOW_TREE_KEY, "Show Tree");
            optionList.add(showTreeOption);
            Option editPropsOption = new Option(StructrRelationship.EDIT_PROPERTIES_KEY, "Edit Properties");
            optionList.add(editPropsOption);
            Option addRelOption = new Option(StructrRelationship.ADD_RELATIONSHIP_KEY, "Add Relationship");
            optionList.add(addRelOption);
            Option removeRelOption = new Option(StructrRelationship.REMOVE_RELATIONSHIP_KEY, "Remove Relationship");
            optionList.add(removeRelOption);
            Option deleteNodeOption = new Option(StructrRelationship.DELETE_NODE_KEY, "Delete Node");
            optionList.add(deleteNodeOption);
            Option createSubnodeOption = new Option(StructrRelationship.CREATE_SUBNODE_KEY, "Create Subnode");
            optionList.add(createSubnodeOption);
            Option executeOption = new Option(StructrRelationship.EXECUTE_KEY, "Execute");
            optionList.add(executeOption);
            Option accessControlOption = new Option(StructrRelationship.ACCESS_CONTROL_KEY, "Access Control");
            optionList.add(accessControlOption);
            allowed.addAll(optionList);
            setPermissionFields.add(allowed);
            setPermissionFields.add(new Submit("setPermissions", " Set Permissions ", this, "onSetPermissions"));
            securityForm.add(setPermissionFields);
            securityForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            securityForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
            securityForm.setActionURL(securityForm.getActionURL().concat("#security-tab"));
            editSecurityPanel = new Panel("editSecurityPanel", "/panel/edit-security-panel.htm");

        }
        // ------------------ security end ---------------------

        if (!(editPropertiesAllowed)) {

            // make all property fields read-only
            List<Field> propertyFields = editPropertiesForm.getFieldList();
            for (Field f : propertyFields) {
                f.setReadonly(true);
            }

            // remove delete relationship link from relationship tables
            incomingRelationshipsTable.removeColumn("Action");
            outgoingRelationshipsTable.removeColumn("Action");
        }
    }

    /**
     * @see Page#onRender()
     */
    @Override
    public void onRender() {

        super.onRender();

        if (node != null) {

            editVisibilityForm.copyFrom(node);
            editVisibilityForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            editVisibilityForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
            editVisibilityForm.add(new HiddenField(RETURN_URL_KEY, returnUrl != null ? returnUrl : ""));

            editPropertiesForm.copyFrom(node);
            editPropertiesForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
            editPropertiesForm.add(new HiddenField(RETURN_URL_KEY, returnUrl != null ? returnUrl : ""));

            deleteRelationshipLink.setParameter(NODE_ID_KEY, nodeId);

            securityForm.copyFrom(node);
            securityForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            securityForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
            securityForm.add(new HiddenField(RETURN_URL_KEY, returnUrl != null ? returnUrl : ""));

            childNodesTable.setDataProvider(new DataProvider() {

                @Override
                public List<StructrNode> getData() {

                    List<StructrNode> result = new ArrayList<StructrNode>();
                    result.addAll(node.getSortedDirectChildAndLinkNodes(user));
                    return result;
                }
            });


            // populate titlesTable table
            titlesTable.setDataProvider(new DataProvider() {

                @Override
                public List<Title> getData() {

                    return node.getTitles();

                }
            });

            incomingRelationshipsTable.setDataProvider(new DataProvider() {

                @Override
                public List<StructrRelationship> getData() {
                    return node.getIncomingRelationships();
                }
            });

            outgoingRelationshipsTable.setDataProvider(new DataProvider() {

                @Override
                public List<StructrRelationship> getData() {
                    return node.getOutgoingRelationships();
                }
            });


            userSelect.setDataProvider(new DataProvider() {

                @Override
                public List<Option> getData() {

                    List<Option> optionList = new ArrayList<Option>();

//                List<StructrNode> principals =  node.getSecurityPrincipals();
                    List<User> users = getAllUsers();
                    if (users != null) {
                        for (User u : users) {
                            Option o = new Option(u.getName());
                            optionList.add(o);
                        }
                    }
                    return optionList;
                }
            });

            // populate security table
            securityTable.setDataProvider(new DataProvider() {

                @Override
                public List<StructrRelationship> getData() {

                    List<StructrRelationship> rels = node.getIncomingRelationships();

                    List<StructrRelationship> result = new ArrayList<StructrRelationship>();
                    for (StructrRelationship r : rels) {

                        RelationshipType rt = r.getRelType();
                        boolean isSecurityRel = rt.equals(RelType.SECURITY);

                        if (isSecurityRel) {
                            result.add(r);
                        }
                    }
                    return result;
                }
            });
        }
    }

    /**
     * Save form data and stay in edit mode
     *
     * @return
     */
    public boolean onSaveProperties() {

        if (editPropertiesForm.isValid()) {

            save();
            okMsg = "Node parameter successfully saved.";
            return redirect();

        } else {
            return true;
        }
    }

    /**
     * Don't save form data. May redirect to previous action
     * {@see redirect()}
     *
     * @return
     */
    public boolean onCancel() {

        return redirect();

//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put(OK_MSG_KEY, okMsg);
//
//        StructrNode targetNode = node.getParentNode(user);
//        if (targetNode == null) {
//            // if no parent available, keep node
//            targetNode = node;
//        }
//
//        parameters.put(NODE_ID_KEY, Long.toString(targetNode.getId()));
//
//        Class<? extends Page> c = DefaultEdit.class;
//        try {
//            c = (Class<? extends Page>) Class.forName(targetNode.getEditPageName());
//        } catch (ClassNotFoundException e) {
//            System.out.println("No edit page found for " + targetNode.getEditPageName());
//        }
//
//        setRedirect(c, parameters);
//
//        return false;
    }

    /**
     * Save form data
     */
    private void save() {
        final Command transactionCommand = Services.createCommand(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                StructrNode s = getNodeByIdOrPath(getNodeId());

                if (editPropertiesForm.isValid()) {
                    editPropertiesForm.copyTo(s, true);
                    transactionCommand.setExitCode(Command.exitCode.SUCCESS);
                    okMsg = "Form data saved successfully";
                } else {
                    transactionCommand.setExitCode(Command.exitCode.FAILURE);
                    errorMsg = "Properties form is invalid";
                    transactionCommand.setErrorMessage(errorMsg);
                }
                return (null);
            }
        });
    }

    /**
     * Delete a property
     */
    public boolean onDeleteRelationship() {

        final Command transaction = Services.createCommand(TransactionCommand.class);

        String localNodeId = deleteRelationshipLink.getParameter(NODE_ID_KEY);
        final String relationshipId = deleteRelationshipLink.getValue();

        final Map<String, String> parameters = new HashMap<String, String>();

        if (relationshipId != null) {

            final Long id = Long.parseLong(relationshipId);

            transaction.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    Command deleteRelationship = Services.createCommand(DeleteRelationshipCommand.class);
                    deleteRelationship.execute(id);
                    transaction.setErrorMessage(deleteRelationship.getErrorMessage());
                    transaction.setExitCode(deleteRelationship.getExitCode());
                    return (null);
                }
            });

            if (Command.exitCode.FAILURE.equals(transaction.getExitCode())) {
                errorMsg = transaction.getErrorMessage();
                parameters.put(ERROR_MSG_KEY, errorMsg);
            } else {
                okMsg = "Relationship successfully removed!"; // TODO: localize
                parameters.put(OK_MSG_KEY, okMsg);
            }

        } else {
            errorMsg = "No Relationship ID!";
            parameters.put(ERROR_MSG_KEY, errorMsg);

        }
        parameters.put(NODE_ID_KEY, localNodeId);
        parameters.put(RENDER_MODE_KEY, renderMode);
        setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId()), this), parameters);

        return false;
    }

    /**
     * Save form data
     */
    public boolean onSetPermissions() {

        final Map<String, String> parameters = new HashMap<String, String>();

        if (securityForm.isValid()) {

            final String selectedUserName = securityForm.getFieldValue(userSelect.getName());
            final List<String> selectedValues = allowed.getSelectedValues();
            final boolean rec = recursive.isChecked();

            node = getNodeByIdOrPath(nodeId);
            Command transaction = Services.createCommand(TransactionCommand.class);

            transaction.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    Command findUser = Services.createCommand(FindUserCommand.class);
                    User selectedUser = (User) findUser.execute(selectedUserName);

                    if (selectedUser != null) {

                        List<StructrNode> nodes = new ArrayList<StructrNode>();


                        if (rec) {

                            Command findNode = Services.createCommand(FindNodeCommand.class);
                            List<StructrNode> result = (List<StructrNode>) findNode.execute(user, node);

                            for (StructrNode s : result) {

                                // superuser can always change access control
                                if (user instanceof SuperUser || s.accessControlAllowed(user)) {
                                    nodes.add(s);
                                }

                            }
                        } else {
                            // not recursive, change only this node
                            nodes.add(node);
                        }

                        for (StructrNode n : nodes) {

                            if (n.equals(selectedUser)) {
                                // don't try to set a relationship with user node itself
                                continue;
                            }

                            StructrRelationship r = n.getSecurityRelationship(selectedUser);

                            if (r == null) {
                                Command createRel = Services.createCommand(CreateRelationshipCommand.class);

                                r = (StructrRelationship) createRel.execute(selectedUser, n, RelType.SECURITY);

                            }

                            if (selectedValues != null && selectedValues.size() > 0) {

                                r.setAllowed(selectedValues);

                            } else {

                                Command deleteRel = Services.createCommand(DeleteRelationshipCommand.class);
                                deleteRel.execute(r);

                            }
                        }

                        okMsg = "Permissions successfully set";
                        parameters.put(OK_MSG_KEY, okMsg);
                    }

                    return (null);
                }
            });
        }

        parameters.put(NODE_ID_KEY, nodeId.toString());
        setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId()), this), parameters);

        return false;
    }

    /**
     * Save visibility data
     */
    public boolean onSaveVisibility() {

        if (editVisibilityForm.isValid()) {

            final Command transactionCommand = Services.createCommand(TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    StructrNode s = getNodeByIdOrPath(getNodeId());

                    if (editVisibilityForm.isValid()) {
                        editVisibilityForm.copyTo(s, true);
                        transactionCommand.setExitCode(Command.exitCode.SUCCESS);
                    } else {
                        transactionCommand.setExitCode(Command.exitCode.FAILURE);
                        transactionCommand.setErrorMessage("Visibility form is invalid");
                    }
                    return (null);
                }
            });
            okMsg = "Node visibility parameter successfully saved.";

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(NODE_ID_KEY, nodeId.toString());
            setRedirect(getPath(), parameters);

            return false;

        } else {
            return true;
        }


    }

    /**
     * Save visibility data on given node and subnodes (all direct children)
     */
    public boolean onSaveVisibilityWithSubnodes() {

        if (editVisibilityForm.isValid()) {

            final Command transactionCommand = Services.createCommand(TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    StructrNode root = getNodeByIdOrPath(getNodeId());

                    List<StructrNode> childNodes = root.getDirectChildren(RelType.HAS_CHILD, user);

                    // include node itself
                    childNodes.add(root);

                    for (StructrNode s : childNodes) {

                        if (editVisibilityForm.isValid()) {
                            editVisibilityForm.copyTo(s, true);
                            transactionCommand.setExitCode(Command.exitCode.SUCCESS);
                        } else {
                            transactionCommand.setExitCode(Command.exitCode.FAILURE);
                            transactionCommand.setErrorMessage("Visibility form is invalid");
                        }
                    }
                    return (null);
                }
            });
            okMsg = "Node visibility parameter successfully saved.";

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(NODE_ID_KEY, nodeId.toString());
            setRedirect(getPath(), parameters);

            return false;

        } else {
            return true;
        }


    }
}
