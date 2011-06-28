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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.click.Context;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.service.ConfigService;
import org.apache.click.util.Bindable;
import org.apache.commons.lang.RandomStringUtils;
import org.structr.common.RelType;
import org.structr.context.SessionMonitor;
import org.structr.context.SessionMonitor.Session;
import org.structr.core.Command;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.core.agent.CleanUpFilesTask;
import org.structr.core.agent.ClearLogsTask;
import org.structr.core.agent.ProcessTaskCommand;
import org.structr.core.agent.RebuildIndexTask;
import org.structr.core.agent.UpdateImageMetadataTask;
import org.structr.core.entity.Image;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.entity.log.Activity;
import org.structr.core.entity.log.LogNodeList;
import org.structr.core.entity.log.PageRequest;
import org.structr.core.log.GetGlobalLogCommand;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.ListModulesCommand;
import org.structr.core.module.ReloadModulesCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.GetAllNodes;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.ui.config.StructrConfigService;
import org.structr.ui.page.admin.CreateNode.NodeType;

/**
 *
 * @author axel
 */
public class Maintenance extends Admin {

    private static final Logger logger = Logger.getLogger(Maintenance.class.getName());
    protected final static String DATABASE_OPEN_KEY = "databaseOpen";
    protected final static String SERVICES_KEY = "services";
    @Bindable
    protected PageLink rootNodeLink = new PageLink("rootNodeLink", "Root Node", Edit.class);
    @Bindable
    protected PageLink reportLink = new PageLink("reportLink", "Reports", Report.class);
    @Bindable
    protected Table sessionsTable = new Table("sessionsTable");
    @Bindable
    protected Table activitiesTable = new Table("activitiesTable");
    @Bindable
    protected Table servicesTable = new Table("servicesTable");
//    @Bindable
//    protected Table taskQueueTable = new Table("taskQueueTable");
    @Bindable
    protected Table initValuesTable = new Table("initValuesTable");
    @Bindable
    protected Table runtimeValuesTable = new Table("runtimeValuesTable");
    @Bindable
    protected Table modulesTable = new Table("modulesTable");
//    @Bindable
//    protected Table registeredClassesTable = new Table("registeredClassesTable");
//    @Bindable
//    protected Table allNodesTable = new Table("allNodesTable");
    @Bindable
    protected ActionLink startupLink = new ActionLink("startupLink", "Startup", this, "onStartup");
    @Bindable
    protected ActionLink shutdownLink = new ActionLink("shutdownLink", "Shutdown", this, "onShutdown");
    @Bindable
    protected ActionLink rebuildIndexLink = new ActionLink("rebuildIndexLink", "Rebuild Index (Background)", this, "onRebuildIndex");
    @Bindable
    protected ActionLink cleanUpFilesLink = new ActionLink("cleanUpFilesLink", "Clean-up Files (Background)", this, "onCleanUpFiles");
    @Bindable
    protected ActionLink clearLogsLink = new ActionLink("clearLogsLink", "Clear Logs (Background)", this, "onClearLogs");
    @Bindable
    protected ActionLink removeThumbnailsLink = new ActionLink("removeThumbnailsLink", "Remove Thumbnails (Ad-hoc)", this, "onRemoveThumbnails");
    @Bindable
    protected ActionLink setImageDimensionsLink = new ActionLink("setImageDimensionsLink", "Set image dimensions on all Image nodes (Background)", this, "onSetImageDimensions");
    @Bindable
    protected ActionLink createAdminLink = new ActionLink("createAdminLink", "Create admin user", this, "onCreateAdminUser");
    @Bindable
    protected ActionLink reloadModules = new ActionLink("reloadModules", "Reload modules", this, "onReloadModules");
    @Bindable
//    protected Panel maintenancePanel;
    private List<AbstractNode> allNodes;
//    protected Map<String, Long> nodesHistogram = new HashMap<String, Long>();
//    @Bindable
//    protected FieldSet statsFields = new FieldSet("statsFields", "Statistics");
    @Override
    public String getTemplate() {
        return "/admin/maintenance.htm";
    }

    public Maintenance() {

        super();
//        maintenancePanel = new Panel("maintenancePanel", "/panel/maintenance-panel.htm");

        sessionsTable.addColumn(new Column("id"));
        sessionsTable.addColumn(new Column("uid"));
        sessionsTable.addColumn(new Column("state"));
        sessionsTable.addColumn(new Column("userName"));
        Column loginTimestampColumn = new Column("loginTimestamp", "Login");
        loginTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        sessionsTable.addColumn(loginTimestampColumn);
        Column logoutTimestampColumn = new Column("logoutTimestamp", "Logout");
        logoutTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        sessionsTable.addColumn(logoutTimestampColumn);
        sessionsTable.addColumn(new Column("lastActivityText"));


        Column inactiveSinceColumn = new Column("inactiveSince", "Inactive");
        inactiveSinceColumn.setDecorator(new Decorator() {

            @Override
            public String render(Object row, Context context) {
                long ms = ((Session) row).getInactiveSince();
                if (ms < 1000) {
                    return ms + " ms";
                } else if (ms < 60 * 1000) {
                    return ms / 1000 + " s";
                } else if (ms < 60 * 60 * 1000) {
                    long min = ms / (60 * 1000);
                    long sec = (ms - (min * 60 * 1000)) / 1000;
                    return min + " m " + sec + " s";
                } else if (ms < 24 * 60 * 60 * 1000) {
                    long hrs = ms / (60 * 60 * 1000);
                    long min = (ms - (hrs * 60 * 60 * 1000)) / (60 * 1000);
                    long sec = (ms - (hrs * 60 * 60 * 1000) - (min * 60 * 1000)) / 1000;
                    return hrs + " h " + min + " m " + sec + " s";
                } else {
                    return "more than a day";
                }
            }
        });
        sessionsTable.addColumn(inactiveSinceColumn);

        sessionsTable.setSortable(true);
        sessionsTable.setSortedColumn("inactiveSince");
        sessionsTable.setSortedAscending(true);
        sessionsTable.setPageSize(15);
        sessionsTable.setHoverRows(true);
        sessionsTable.setShowBanner(true);
        sessionsTable.setClass(TABLE_CLASS);

        activitiesTable.addColumn(new Column(Activity.OWNER_KEY, "User"));
        activitiesTable.addColumn(new Column(Activity.NODE_ID_KEY));
        activitiesTable.addColumn(new Column(Activity.NAME_KEY));
        activitiesTable.addColumn(new Column(Activity.SESSION_ID_KEY));
        activitiesTable.addColumn(new Column(Activity.TYPE_KEY));
        Column startTimestampColumn = new Column(Activity.START_TIMESTAMP_KEY, "Start");
        startTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        activitiesTable.addColumn(startTimestampColumn);
        Column endTimestampColumn = new Column(Activity.END_TIMESTAMP_KEY, "End");
        endTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        activitiesTable.addColumn(endTimestampColumn);
        activitiesTable.addColumn(new Column(Activity.ACTIVITY_TEXT_KEY));
        activitiesTable.setSortedColumn(Activity.START_TIMESTAMP_KEY);
        activitiesTable.setSortable(true);
        activitiesTable.setSortedAscending(false);
        activitiesTable.setPageSize(15);
        activitiesTable.setHoverRows(true);
        activitiesTable.setShowBanner(true);
        activitiesTable.setClass(TABLE_CLASS);

        servicesTable.addColumn(new Column("Name"));
        servicesTable.addColumn(new Column("isRunning", "Running"));
        servicesTable.setSortable(true);
        servicesTable.setClass(TABLE_CLASS);

//        taskQueueTable.addColumn(new Column("type"));
//        taskQueueTable.addColumn(new Column("user"));
//        taskQueueTable.addColumn(new Column("priority"));
//        taskQueueTable.addColumn(new Column("creationTime"));
//        taskQueueTable.addColumn(new Column("scheduledTime"));
//        taskQueueTable.setSortable(true);
//        taskQueueTable.setPageSize(15);
//        taskQueueTable.setHoverRows(true);
//        taskQueueTable.setShowBanner(true);
//        taskQueueTable.setClass(TABLE_CLASS);

        initValuesTable.addColumn(new Column("key", "Parameter"));
        initValuesTable.addColumn(new Column("value", "Value"));
        initValuesTable.setSortable(true);
        initValuesTable.setClass(TABLE_CLASS);

        runtimeValuesTable.addColumn(new Column("key", "Parameter"));
        runtimeValuesTable.addColumn(new Column("value", "Value"));
        runtimeValuesTable.setSortable(true);
        runtimeValuesTable.setClass(TABLE_CLASS);

        modulesTable.addColumn(new Column("toString", "Name"));
        modulesTable.setSortable(true);
        modulesTable.setClass(TABLE_CLASS);

        Column iconCol = new Column("iconSrc", "Icon");
        iconCol.setDecorator(new Decorator() {

            @Override
            public String render(Object row, Context context) {
                NodeClassEntry nce = (NodeClassEntry) row;
                String iconSrc = contextPath + nce.getIconSrc();
                return "<img src=\"" + iconSrc + "\" alt=\"" + iconSrc + "\" width=\"16\" height=\"16\">";
            }
        });

//        registeredClassesTable.addColumn(iconCol);
//        registeredClassesTable.addColumn(new Column("name", "Name"));
//        registeredClassesTable.addColumn(new Column("count", "Count"));
//        registeredClassesTable.setSortable(true);
//        registeredClassesTable.setSortedColumn("name");
//        registeredClassesTable.setPageSize(15);
//        registeredClassesTable.setHoverRows(true);
//        registeredClassesTable.setShowBanner(true);
//        registeredClassesTable.setClass(TABLE_CLASS);

//        allNodesTable.addColumn(new Column(AbstractNode.NODE_ID_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.NAME_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.TYPE_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.POSITION_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.PUBLIC_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.OWNER_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.CREATED_BY_KEY));
//        allNodesTable.addColumn(new Column(AbstractNode.CREATED_DATE_KEY));
//        allNodesTable.addColumn(new Column("allProperties"));
//        allNodesTable.setSortable(true);
//        allNodesTable.setPageSize(15);
//        allNodesTable.setHoverRows(true);
//        allNodesTable.setShowBanner(true);
//        allNodesTable.setClass(TABLE_CLASS);

    }

    @Override
    public void onInit() {
        super.onInit();
//        initHistogram();
    }

    @Override
    public void onRender() {

        rootNodeLink.setParameter(AbstractNode.NODE_ID_KEY, "0");

        if (allNodes == null) {
            return;
        }

        // fill table with logged activities
        activitiesTable.setDataProvider(new DataProvider() {

            @Override
            public List<Activity> getData() {

                List<Activity> result = new LinkedList<Activity>();

                LogNodeList<AbstractNode> globalLog = (LogNodeList<AbstractNode>) Services.command(GetGlobalLogCommand.class).execute();

                if (globalLog != null) {

                    for (AbstractNode s : globalLog) {

                        if (s instanceof PageRequest) {
//                            PageRequest p = new PageRequest();
//                            p.init(s);
                            result.add((PageRequest) s);
                        } else {
//                            Activity a = new Activity();
//                            a.init(s);
                            result.add((Activity) s);
                        }
                    }
                }
                return result;

            }
        });

        // fill table with sessions
        sessionsTable.setDataProvider(new DataProvider() {

            @Override
            public List<Session> getData() {
                return (List<Session>) SessionMonitor.getSessions();
            }
        });

//        taskQueueTable.setDataProvider(new DataProvider() {
//
//            @Override
//            public List<Task> getData() {
//
//                List<Task> taskList = new LinkedList<Task>();
//                Queue<Task> queue = (Queue<Task>) Services.command(ListTasksCommand.class).execute();
//
//                for (Task t : queue) {
//                    taskList.add(t);
//                }
//                return taskList;
//            }
//        });

        // fill table with all known agents
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

                List<Map.Entry<String, Object>> params = new LinkedList<Map.Entry<String, Object>>();

                params.add(new AbstractMap.SimpleEntry<String, Object>("Configuration File Path", Services.getConfigFilePath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Application Title", Services.getApplicationTitle()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Database Path", Services.getDatabasePath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Files Path", Services.getFilesPath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Modules Path", Services.getModulesPath()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("TCP Port", Services.getTcpPort()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("UDP Port", Services.getUdpPort()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("SMTP Host", Services.getSmtpHost()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("SMTP Port", Services.getSmtpPort()));
                params.add(new AbstractMap.SimpleEntry<String, Object>("Server IP", Services.getServerIP()));

                return params;

            }
        });

//        runtimeValuesTable.setDataProvider(new DataProvider() {
//
//            @Override
//            public List<Map.Entry<String, Object>> getData() {
//
//                List<Map.Entry<String, Object>> params = new LinkedList<Map.Entry<String, Object>>();
//
//                //params.add(new HashMap.Entry<String, Object>("Number of Nodes", numberOfNodes));
////                Command findNode = Services.command(FindNodeCommand.class);
//
//                for (AbstractNode s : allNodes) {
//
//                    String type = s.getType();
//                    long value = 0L;
//                    if (nodesHistogram.containsKey(type)) {
//                        value = (Long) nodesHistogram.get(type);
//                    }
//
//                    value++;
//
//                    // increase counter
//                    nodesHistogram.put(type, value);
//                }
//
//                params.add(new AbstractMap.SimpleEntry<String, Object>("Nodes", allNodes.size()));
//
//                return params;
//
//            }
//        });

        modulesTable.setDataProvider(new DataProvider() {

            @Override
            public Set<String> getData() {

                Command listModules = Services.command(ListModulesCommand.class);
                return (Set<String>) listModules.execute();

            }
        });

//        registeredClassesTable.setDataProvider(new DataProvider() {
//
//            @Override
//            public Set<NodeClassEntry> getData() {
//
//                SortedSet<NodeClassEntry> nodeClassList = new TreeSet<NodeClassEntry>();
//
//                Map<String, Class> entities = (Map<String, Class>) Services.command(GetEntitiesCommand.class).execute();
//
//                for (Entry<String, Class> entry : entities.entrySet()) {
//                    String n = entry.getKey();
//                    Class c = entry.getValue();
//
//                    NodeType type = new NodeType(n, c);
//
//                    String iconSrc = type.getIconSrc();
//                    String shortName = type.getKey();
//
//                    nodeClassList.add(new NodeClassEntry(c.getName(), iconSrc, nodesHistogram.get(shortName)));
//
//                }
////                Set<String> types = Services.getCachedEntityTypes();
////                for (String type : types) {
////                    Class c = Services.getEntityClass(type);
////                    String name = c.getName();
////                    AbstractNode s;
////                    try {
////                        s = (AbstractNode) c.newInstance();
////                        String iconSrc = s.getIconSrc();
////                        String shortName = c.getSimpleName();
////                        nodeClassList.add(new NodeClassEntry(name, iconSrc, nodesHistogram.get(shortName)));
////
////                    } catch (InstantiationException ex) {
////                        Logger.getLogger(Maintenance.class.getName()).log(Level.SEVERE, null, ex);
////                    } catch (IllegalAccessException ex) {
////                        Logger.getLogger(Maintenance.class.getName()).log(Level.SEVERE, null, ex);
////                    }
////
////                }
//                return nodeClassList;
//            }
//        });

//        allNodesTable.setDataProvider(new DataProvider() {
//
//            @Override
//            public List<AbstractNode> getData() {
//
//                return getAllNodes();
//
//            }
//        });

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

//    private void initHistogram() {
//
//        for (AbstractNode s : getAllNodes()) {
//
//            String type = s.getType();
//            long value = 0L;
//            if (nodesHistogram.containsKey(type)) {
//                value = (Long) nodesHistogram.get(type);
//            }
//
//            value++;
//
//            // increase counter
//            nodesHistogram.put(type, value);
//        }
//    }

    public boolean onReloadModules() {
        ServletContext servletContext = this.getContext().getServletContext();

        try {
            // reload modules
            Services.command(ReloadModulesCommand.class).execute();

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

        Command transactionCommand = Services.command(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.command(CreateNodeCommand.class);
                Command createRel = Services.command(CreateRelationshipCommand.class);

                // create a new user node
                User adminUser = (User) createNode.execute(
                        new NodeAttribute(AbstractNode.TYPE_KEY, User.class.getSimpleName()),
                        new NodeAttribute(AbstractNode.NAME_KEY, "admin"),
                        new SuperUser());

                AbstractNode rootNode = getRootNode();

                // link new admin node to contact root node
                createRel.execute(rootNode, adminUser, RelType.HAS_CHILD);

//                User user = new User();
//                user.init(node);

                String password = RandomStringUtils.randomAlphanumeric(8);
                adminUser.setPassword(password);

                okMsg = "New " + adminUser.getType() + " node " + adminUser.getName() + " has been created with password " + password;

                StructrRelationship securityRel = (StructrRelationship) createRel.execute(adminUser, rootNode, RelType.SECURITY);
                securityRel.setAllowed(Arrays.asList(StructrRelationship.ALL_PERMISSIONS));

                return adminUser;
            }
        });

        return redirect();

    }

    private List<AbstractNode> getAllNodes() {
        if (allNodes == null) {
            allNodes = (List<AbstractNode>) Services.command(GetAllNodes.class).execute();
        }
        return allNodes;
    }

    /**
     * Remove all thumbnails in the system
     *
     * @return
     */
    public boolean onRemoveThumbnails() {

        Long numberOfRemovedThumbnails = (Long) Services.command(TransactionCommand.class).execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Long numberOfThumbnails = 0L;

                // Find all image nodes
                //List<Image> images = (List<Image>) Services.command(SearchNodeCommand.class).execute(null, null, true, false, Search.andExactType(Image.class.getSimpleName()));
                List<Image> images = new LinkedList<Image>();

                for (AbstractNode s : getAllNodes()) {
                    if (s instanceof Image) {
                        images.add((Image) s);
                    }
                }

                Command deleteNode = Services.command(DeleteNodeCommand.class);
                Command deleteRel = Services.command(DeleteRelationshipCommand.class);

                // Loop through all images
                for (Image image : images) {

                    // Remove any thumbnail and incoming thumbnail relationship
                    List<StructrRelationship> rels = (List<StructrRelationship>) image.getThumbnailRelationships();
                    for (StructrRelationship r : rels) {

                        AbstractNode t = r.getEndNode();

                        // delete relationship
                        deleteRel.execute(r);

                        // remove node with super user rights
                        deleteNode.execute(t, new SuperUser());

                        numberOfThumbnails++;

                    }
                }
                return numberOfThumbnails;

            }
        });


        okMsg = numberOfRemovedThumbnails + " thumbnails successfully removed.";
        return false;

    }

    /**
     * Set image dimensions and content-type
     *
     * @return
     */
    public boolean onSetImageDimensions() {
        Command processTask = Services.command(ProcessTaskCommand.class);
        processTask.execute(new UpdateImageMetadataTask());
        return redirect();
    }

    /**
     * Rebuild index, running in background
     * 
     * @return
     */
    public boolean onRebuildIndex() {
        Command processTask = Services.command(ProcessTaskCommand.class);
        processTask.execute(new RebuildIndexTask());
        return redirect();
    }

    /**
     * Remove unused files, running in background
     * 
     * @return
     */
    public boolean onCleanUpFiles() {
        Command processTask = Services.command(ProcessTaskCommand.class);
        processTask.execute(new CleanUpFilesTask());
        return redirect();
    }

    /**
     * Remove all log nodes, running in background
     * 
     * @return
     */
    public boolean onClearLogs() {
        Command processTask = Services.command(ProcessTaskCommand.class);
        processTask.execute(new ClearLogsTask());
        return redirect();
    }

    public boolean onStartup() {
        Services.initialize();
        return false;
    }

    public boolean onShutdown() {
        Services.shutdown();
        return false;
    }

}
