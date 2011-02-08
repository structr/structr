/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.ServletContext;
import org.apache.click.Context;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.PageLink;
import org.apache.click.control.Panel;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.service.ConfigService;
import org.apache.click.util.Bindable;
import org.apache.commons.lang.RandomStringUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.SearchOperator;
//import org.structr.core.ClasspathEntityLocator;
import org.structr.context.SessionMonitor;
import org.structr.context.SessionMonitor.Session;
import org.structr.core.Command;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.core.entity.Image;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.GetEntityPackagesCommand;
import org.structr.core.module.ListModulesCommand;
import org.structr.core.module.ReloadModulesCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.SearchAttribute;
import org.structr.core.node.SearchNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.ui.config.StructrConfigService;
import org.structr.ui.page.admin.CreateNode.NodeType;

/**
 *
 * @author axel
 */
public class Maintenance extends Admin {

    protected final static String DATABASE_OPEN_KEY = "databaseOpen";
    protected final static String SERVICES_KEY = "services";
    @Bindable
    protected PageLink rootNodeLink = new PageLink("rootNodeLink", "Root Node", DefaultEdit.class);
    @Bindable
    protected PageLink reportLink = new PageLink("reportLink", "Reports", Report.class);
    @Bindable
    protected Table sessionsTable = new Table("sessionsTable");
    @Bindable
    protected Table servicesTable = new Table("servicesTable");
    @Bindable
    protected Table initValuesTable = new Table("initValuesTable");
    @Bindable
    protected Table runtimeValuesTable = new Table("runtimeValuesTable");
    @Bindable
    protected Table modulesTable = new Table("modulesTable");
    @Bindable
    protected Table registeredClassesTable = new Table("registeredClassesTable");
    @Bindable
    protected Table allNodesTable = new Table("allNodesTable");
    @Bindable
    protected ActionLink removeThumbnailsLink = new ActionLink("removeThumbnailsLink", "Remove Thumbnails", this, "onRemoveThumbnails");
    @Bindable
    protected ActionLink createAdminLink = new ActionLink("createAdminLink", "Create admin user", this, "onCreateAdminUser");
    @Bindable
    protected ActionLink reloadModules = new ActionLink("reloadModules", "Reload modules", this, "onReloadModules");
    @Bindable
    protected Panel maintenancePanel;
    private List<StructrNode> allNodes;
    protected Map<String, Long> nodesHistogram = new HashMap<String, Long>();
//    @Bindable
//    protected FieldSet statsFields = new FieldSet("statsFields", "Statistics");

    public Maintenance() {

        maintenancePanel = new Panel("maintenancePanel", "/panel/maintenance-panel.htm");

        sessionsTable.addColumn(new Column("id"));
        sessionsTable.addColumn(new Column("state"));
        sessionsTable.addColumn(new Column("userName"));
        sessionsTable.addColumn(new Column("remoteHost"));
        sessionsTable.addColumn(new Column("remoteAddr"));
        sessionsTable.addColumn(new Column("remoteUser"));
        Column loginTimestampColumn = new Column("loginTimestamp", "Login");
        loginTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        sessionsTable.addColumn(loginTimestampColumn);
        sessionsTable.addColumn(new Column("logoutTimestamp", "Logout"));
        sessionsTable.addColumn(new Column("lastActivity"));
        sessionsTable.addColumn(new Column("lastActivityUri"));
        sessionsTable.addColumn(new Column("inactiveSince", "Inactive (s)"));
        sessionsTable.setSortable(true);
        sessionsTable.setSortedColumn("inactiveSince");
        sessionsTable.setSortedAscending(true);
        sessionsTable.setClass(Table.CLASS_COMPLEX);

        servicesTable.addColumn(new Column("Name"));
        servicesTable.addColumn(new Column("isRunning", "Running"));
        servicesTable.setSortable(true);
        servicesTable.setClass(Table.CLASS_COMPLEX);

        initValuesTable.addColumn(new Column("key", "Parameter"));
        initValuesTable.addColumn(new Column("value", "Value"));
        initValuesTable.setSortable(true);
        initValuesTable.setClass(Table.CLASS_COMPLEX);

        runtimeValuesTable.addColumn(new Column("key", "Parameter"));
        runtimeValuesTable.addColumn(new Column("value", "Value"));
        runtimeValuesTable.setSortable(true);
        runtimeValuesTable.setClass(Table.CLASS_COMPLEX);

        modulesTable.addColumn(new Column("toString", "Name"));
        modulesTable.setSortable(true);
        modulesTable.setClass(Table.CLASS_COMPLEX);

        Column iconCol = new Column("iconSrc", "Icon");
        iconCol.setDecorator(new Decorator() {

            @Override
            public String render(Object row, Context context) {
                NodeClassEntry nce = (NodeClassEntry) row;
                String iconSrc = contextPath + nce.getIconSrc();
                return "<img src=\"" + iconSrc + "\" alt=\"" + iconSrc + "\" width=\"16\" height=\"16\">";
            }
        });

        registeredClassesTable.addColumn(iconCol);
        registeredClassesTable.addColumn(new Column("name", "Name"));
        registeredClassesTable.addColumn(new Column("count", "Count"));
        registeredClassesTable.setSortable(true);
        registeredClassesTable.setSortedColumn("name");
        registeredClassesTable.setClass(Table.CLASS_COMPLEX);

        allNodesTable.addColumn(new Column(StructrNode.NODE_ID_KEY));
        allNodesTable.addColumn(new Column(StructrNode.NAME_KEY));
        allNodesTable.addColumn(new Column(StructrNode.TYPE_KEY));
        allNodesTable.addColumn(new Column(StructrNode.POSITION_KEY));
        allNodesTable.addColumn(new Column(StructrNode.PUBLIC_KEY));
        allNodesTable.addColumn(new Column(StructrNode.OWNER_KEY));
        allNodesTable.addColumn(new Column(StructrNode.CREATED_BY_KEY));
        allNodesTable.addColumn(new Column(StructrNode.CREATED_DATE_KEY));
        allNodesTable.addColumn(new Column("allProperties"));
        allNodesTable.setSortable(true);
        allNodesTable.setPageSize(25);
        allNodesTable.setClass(Table.CLASS_COMPLEX);

    }

    @Override
    public void onInit() {
        super.onInit();
        initHistogram();
    }

    @Override
    public void onRender() {

        rootNodeLink.setParameter(StructrNode.NODE_ID_KEY, "0");

        if (allNodes == null) {
            return;
        }

        // fill table with all known services
        sessionsTable.setDataProvider(new DataProvider() {

            @Override
            public List<Session> getData() {
                return (List<Session>) SessionMonitor.getSessions();
            }
        });

        // fill table with all known services
        servicesTable.setDataProvider(new DataProvider() {

            @Override
            public List<Service> getData() {
                return Services.getServices();
            }
        });

        // assemble data for parameter tables
        initValuesTable.setDataProvider(new DataProvider() {

            @Override
            public List<Map.Entry<String, Object>> getData() {

                List<Map.Entry<String, Object>> params = new ArrayList<Map.Entry<String, Object>>();
                Set<String> entityPackages = ((Set<String>) Services.createCommand(GetEntityPackagesCommand.class).execute());

                params.add(new AbstractMap.SimpleEntry<String, Object>("Configuration File Path", Services.getConfigFilePath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Application Title", Services.getApplicationTitle()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Database Path", Services.getDatabasePath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Files Path", Services.getFilesPath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Modules Path", Services.getModulesPath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Entity Packages", entityPackages));

                return params;

            }
        });

        runtimeValuesTable.setDataProvider(new DataProvider() {

            @Override
            public List<Map.Entry<String, Object>> getData() {

                List<Map.Entry<String, Object>> params = new ArrayList<Map.Entry<String, Object>>();

                //params.add(new HashMap.Entry<String, Object>("Number of Nodes", numberOfNodes));
//                Command findNode = Services.createCommand(FindNodeCommand.class);

                for (StructrNode s : allNodes) {

                    String type = s.getType();
                    long value = 0L;
                    if (nodesHistogram.containsKey(type)) {
                        value = (Long) nodesHistogram.get(type);
                    }

                    value++;

                    // increase counter
                    nodesHistogram.put(type, value);
                }

                params.add(new AbstractMap.SimpleEntry<String, Object>("Nodes", allNodes.size()));

                return params;

            }
        });

        modulesTable.setDataProvider(new DataProvider() {

            @Override
            public Set<String> getData() {

                Command listModules = Services.createCommand(ListModulesCommand.class);
                return (Set<String>) listModules.execute();

            }
        });

        registeredClassesTable.setDataProvider(new DataProvider() {

            @Override
            public Set<NodeClassEntry> getData() {

                SortedSet<NodeClassEntry> nodeClassList = new TreeSet<NodeClassEntry>();

                Map<String, Class> entities = (Map<String, Class>) Services.createCommand(GetEntitiesCommand.class).execute();

                for (Entry<String, Class> entry : entities.entrySet()) {
                    String n = entry.getKey();
                    Class c = entry.getValue();

                    NodeType type = new NodeType(n, c);

                    String iconSrc = type.getIconSrc();
                    String shortName = type.getKey();

                    nodeClassList.add(new NodeClassEntry(c.getName(), iconSrc, nodesHistogram.get(shortName)));

                }
//                Set<String> types = Services.getCachedEntityTypes();
//                for (String type : types) {
//                    Class c = Services.getEntityClass(type);
//                    String name = c.getName();
//                    StructrNode s;
//                    try {
//                        s = (StructrNode) c.newInstance();
//                        String iconSrc = s.getIconSrc();
//                        String shortName = c.getSimpleName();
//                        nodeClassList.add(new NodeClassEntry(name, iconSrc, nodesHistogram.get(shortName)));
//
//                    } catch (InstantiationException ex) {
//                        Logger.getLogger(Maintenance.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (IllegalAccessException ex) {
//                        Logger.getLogger(Maintenance.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                }
                return nodeClassList;
            }
        });

        allNodesTable.setDataProvider(new DataProvider() {

            @Override
            public List<StructrNode> getData() {

                return allNodes;

            }
        });

    }

    public class NodeClassEntry implements Comparable {

        private String name;
        private String iconSrc;
        private Long count;

        public NodeClassEntry(final String name, final String iconSrc, final Long count) {
            this.name = name;
            this.iconSrc = iconSrc;
            this.count = count;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the iconSrc
         */
        public String getIconSrc() {
            return iconSrc;
        }

        /**
         * @param iconSrc the iconSrc to set
         */
        public void setIconSrc(String iconSrc) {
            this.iconSrc = iconSrc;
        }

        @Override
        public int compareTo(Object t) {
            return ((NodeClassEntry) t).getName().compareTo(this.getName());
        }

        /**
         * @return the count
         */
        public Long getCount() {
            return count;
        }

        /**
         * @param count the count to set
         */
        public void setCount(Long count) {
            this.count = count;
        }
    }

    private void initHistogram() {

        Command search = Services.createCommand(SearchNodeCommand.class);
        allNodes = (List<StructrNode>) search.execute(null, null, true, false, new SearchAttribute("name", "*", SearchOperator.OR));

        if (allNodes == null) {
            return;
        }

        for (StructrNode s : allNodes) {

            String type = s.getType();
            long value = 0L;
            if (nodesHistogram.containsKey(type)) {
                value = (Long) nodesHistogram.get(type);
            }

            value++;

            // increase counter
            nodesHistogram.put(type, value);
        }
    }

    public boolean onReloadModules() {
        ServletContext servletContext = this.getContext().getServletContext();

        try {
            // reload modules
            Services.createCommand(ReloadModulesCommand.class).execute();

            // create new config service
            StructrConfigService newConfigService = new StructrConfigService();
            newConfigService.onInit(servletContext);

            // replace existing config service when refresh was successful
            synchronized (servletContext) {
                servletContext.setAttribute(ConfigService.CONTEXT_NAME, newConfigService);
            }

        } catch (Throwable t) {
        }

        return redirect();
    }

    public boolean onCreateAdminUser() {

        Command transactionCommand = Services.createCommand(TransactionCommand.class);
        User admin = null;
        admin = (User) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.createCommand(CreateNodeCommand.class);
                Command createRel = Services.createCommand(CreateRelationshipCommand.class);

                // create a new contact person node
                StructrNode node = (StructrNode) createNode.execute(
                        new NodeAttribute(StructrNode.TYPE_KEY, User.class.getSimpleName()),
                        new NodeAttribute(StructrNode.NAME_KEY, "admin"),
                        new SuperUser());

                StructrNode rootNode = getRootNode();

                // link new admin node to contact root node
                createRel.execute(rootNode, node, RelType.HAS_CHILD);

                User user = new User();
                user.init(node);

                String password = RandomStringUtils.randomAlphanumeric(8);
                user.setPassword(password);

                okMsg = "New " + user.getType() + " node " + user.getName() + " has been created with password " + password + ".";

                StructrRelationship securityRel = (StructrRelationship) createRel.execute(user, rootNode, RelType.SECURITY);
                securityRel.setAllowed(Arrays.asList(StructrRelationship.ALL_PERMISSIONS));

                return user;
            }
        });

        return redirect();

    }

    /**
     * Remove all thumbnails in the system
     *
     * @return
     */
    public boolean onRemoveThumbnails() {

        // Find all image nodes
        Command searchNode = Services.createCommand(SearchNodeCommand.class);
        final List<StructrNode> images = (List<StructrNode>) searchNode.execute(null, null, true, false, new SearchAttribute(StructrNode.TYPE_KEY, Image.class.getSimpleName(), SearchOperator.OR));

        final Command transactionCommand = Services.createCommand(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command deleteNode = Services.createCommand(DeleteNodeCommand.class);
                Command deleteRel = Services.createCommand(DeleteRelationshipCommand.class);

                // Loop through all images
                for (StructrNode s : images) {

                    // Does it have thumbnails?
                    if (s.hasRelationship(RelType.THUMBNAIL, Direction.OUTGOING)) {

                        // Remove any thumbnail and incoming thumbnail relationship
                        List<StructrRelationship> rels = (List<StructrRelationship>) s.getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);
                        for (StructrRelationship r : rels) {

                            StructrNode t = r.getEndNode();

                            // delete relationship
                            deleteRel.execute(r, new SuperUser());

                            // remove node with super user rights
                            deleteNode.execute(t, new SuperUser());

                        }
                    }
                }
                return null;

            }
        });


        okMsg = "Thumbnails successfully removed.";
        return false;

    }
}
