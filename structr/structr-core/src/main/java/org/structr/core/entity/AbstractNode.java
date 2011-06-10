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
package org.structr.core.entity;

import freemarker.template.Configuration;
import org.structr.core.node.TransactionCommand;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.NodeRelationshipsCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.common.RelType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.CurrentRequest;
import org.structr.common.TemplateHelper;
import org.structr.core.NodeSource;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.XPath;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

/**
 * 
 * @author amorgner
 * 
 */
public abstract class AbstractNode implements Comparable<AbstractNode> {

    private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());
    private static final boolean updateIndexDefault = true;
    // request parameters
    //private HttpServletRequest request = null;
    //private HttpSession session = null;
    private Map<Long, StructrRelationship> securityRelationships = null;
    private List<StructrRelationship> incomingLinkRelationships = null;
    private List<StructrRelationship> outgoingLinkRelationships = null;
    private List<StructrRelationship> incomingChildRelationships = null;
    private List<StructrRelationship> outgoingChildRelationships = null;
    private List<StructrRelationship> outgoingDataRelationships = null;
    private List<StructrRelationship> incomingDataRelationships = null;
    private List<StructrRelationship> incomingRelationships = null;
    private List<StructrRelationship> outgoingRelationships = null;
    private List<StructrRelationship> allRelationships = null;

    // ----- abstract methods ----
    public abstract void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId);

    public abstract String getIconSrc();

    public abstract void onNodeCreation();

    public abstract void onNodeInstantiation();
    // reference to database node
    protected Node dbNode;
    // dirty flag, true means that some changes are not yet written to the database
    protected boolean isDirty;
    protected Map<String, Object> properties;
    // keys for basic properties (any node should have at least all of the following properties)
    public final static String TYPE_KEY = "type";
    public final static String NAME_KEY = "name";
    public final static String CATEGORIES_KEY = "categories";
    public final static String TITLE_KEY = "title";
//    public final static String LOCALE_KEY = "locale";
    public final static String TITLES_KEY = "titles";
    public final static String POSITION_KEY = "position";
    public final static String NODE_ID_KEY = "nodeId";
    public final static String OWNER_KEY = "owner";
    public final static String CREATED_DATE_KEY = "createdDate";
    public final static String CREATED_BY_KEY = "createdBy";
    public final static String LAST_MODIFIED_DATE_KEY = "lastModifiedDate";
    public final static String VISIBILITY_START_DATE_KEY = "visibilityStartDate";
    public final static String VISIBILITY_END_DATE_KEY = "visibilityEndDate";
    public final static String PUBLIC_KEY = "public";
    public final static String VISIBLE_TO_AUTHENTICATED_USERS_KEY = "visibleToAuthenticatedUsers";
    public final static String HIDDEN_KEY = "hidden";
    public final static String DELETED_KEY = "deleted";
//    public final static String ACL_KEY = "acl";
    //private final static String keyPrefix = "${";
    //private final static String keySuffix = "}";
    private final static String NODE_KEY_PREFIX = "%{";
    private final static String NODE_KEY_SUFFIX = "}";
//    private final static String REQUEST_KEY_PREFIX = "$[";
//    private final static String REQUEST_KEY_SUFFIX = "]";
    private final static String CALLING_NODE_SUBNODES_KEY = "*";
    private final static String CALLING_NODE_SUBNODES_AND_LINKED_NODES_KEY = "#";
    public final static String TEMPLATE_ID_KEY = "templateId";
    public final static String TYPE_NODE_ID_KEY = "typeNodeId";
    //public final static String TEMPLATES_KEY = "templates";
    protected Template template;
    protected User user;

    /*
     * Helper class for multilanguage titles
     */
    public class Title {

        public final static String LOCALE_KEY = "locale";
        private Locale locale;
        private String title;

        public Title(final Locale locale, final String title) {
            this.locale = locale;
            this.title = title;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(final Locale locale) {
            this.locale = locale;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }
    }

    public AbstractNode() {
        this.properties = new HashMap<String, Object>();
        isDirty = true;
    }

    public AbstractNode(final Map<String, Object> properties) {
        this.properties = properties;
        isDirty = true;
    }

    public AbstractNode(final NodeDataContainer data) {
        if (data != null) {
            this.properties = data.getProperties();
            isDirty = true;
        }
    }

    public AbstractNode(final Node dbNode) {
        init(dbNode);
    }

    public void init(final Node dbNode) {
        this.dbNode = dbNode;
        isDirty = false;
        user = CurrentRequest.getCurrentUser();
    }

    private void init(final AbstractNode node) {
        this.dbNode = node.dbNode;
        isDirty = false;
    }

    public void init(final NodeDataContainer data) {
        if (data != null) {
            this.properties = data.getProperties();
            isDirty = true;
        }
    }

    @Override
    public boolean equals(final Object o) {

        if (o == null) {
            return false;
        }
        
        if (!(o instanceof AbstractNode)) {
            return false;
        }
        
        /*
        return ((AbstractNode) o).equals(this);
        }
        
        private boolean equals(final AbstractNode node) {
        if (node == null) {
        return false;
        }
        return (this.getId() == (node.getId()));
         */
        return (new Integer(this.hashCode()).equals(new Integer(o.hashCode())));
    }

    @Override
    public int hashCode() {
        if (this.dbNode == null) {
            return (super.hashCode());
        }

        return (new Long(dbNode.getId()).hashCode());
    }

    @Override
    public int compareTo(final AbstractNode node) {
        // TODO: implement finer compare methods, e.g. taking title and position into account
        if (node == null || node.getName() == null || this.getName() == null) {
            return -1;
        }
        return (this.getName().compareTo(node.getName()));
    }

//    public void setSession(final HttpSession session) {
//        this.session = session;
//    }
//
//    public void setRequest(final HttpServletRequest request) {
//        this.request = request;
//    }
//
//    public HttpSession getSession() {
//        return session;
//    }
//
//    public HttpServletRequest getRequest() {
//        return request;
//    }
    public void setTemplate(final Template template) {
        this.template = template;
    }

    public void createTemplateRelationship(final Template template) {

        // create a relationship to the given template node
        Command createRel = Services.command(CreateRelationshipCommand.class);
        createRel.execute(this, template, RelType.USE_TEMPLATE);

    }

    public Long getTemplateId() {
        Template n = getTemplate();
        return (n != null ? n.getId() : null);
    }

    public void setTemplateId(final Long value) {

        // find template node
        Command findNode = Services.command(FindNodeCommand.class);
        Template templateNode = (Template) findNode.execute(new SuperUser(), value);

        // delete existing template relationships
        List<StructrRelationship> templateRels = this.getOutgoingRelationships(RelType.USE_TEMPLATE);
        Command delRel = Services.command(DeleteRelationshipCommand.class);
        if (templateRels != null) {
            for (StructrRelationship r : templateRels) {
                delRel.execute(r);
            }
        }

        // create new link target relationship
        Command createRel = Services.command(CreateRelationshipCommand.class);
        createRel.execute(this, templateNode, RelType.USE_TEMPLATE);
    }

    /**
     * Render a node-specific inline edit view as html
     * 
     * @param out
     * @param node
     * @param editUrl
     * @param editNodeId
     */
    public void renderEditView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (getId() == editNodeId.longValue()) {
            renderEditFrame(out, editUrl);
        }
    }

    /**
     * Render an IFRAME element with the given editor URL inside
     *
     * @param out
     * @param editUrl
     */
    protected void renderEditFrame(StringBuilder out, final String editUrl) {
        // create IFRAME with given URL
        out.append("<iframe style=\"border: 1px solid #ccc; background-color: #fff\" src=\"").append(editUrl).append("\" width=\"100%\" height=\"100%\"").append("></iframe>");
    }

    /**
     * Wrapper for toString method
     * @return
     */
    public String getAllProperties() {
        return toString();
    }

    /**
     * Implement standard toString() method
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        out.append(getName()).append(" [").append(getId()).append("]: ");

        List<String> props = new LinkedList<String>();

        for (String key : getPropertyKeys()) {

            Object value = getProperty(key);

            if (value != null) {


                String displayValue = "";

                if (value.getClass().isPrimitive()) {
                    displayValue = value.toString();
                } else if (value.getClass().isArray()) {

                    if (value instanceof byte[]) {

                        displayValue = new String((byte[]) value);

                    } else if (value instanceof char[]) {

                        displayValue = new String((char[]) value);

                    } else if (value instanceof double[]) {

                        Double[] values = ArrayUtils.toObject((double[]) value);
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof float[]) {

                        Float[] values = ArrayUtils.toObject((float[]) value);
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof short[]) {

                        Short[] values = ArrayUtils.toObject((short[]) value);
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof long[]) {

                        Long[] values = ArrayUtils.toObject((long[]) value);
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof int[]) {

                        Integer[] values = ArrayUtils.toObject((int[]) value);
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof boolean[]) {

                        Boolean[] values = (Boolean[]) value;
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                    } else if (value instanceof byte[]) {

                        displayValue = new String((byte[]) value);

                    } else {

                        Object[] values = (Object[]) value;
                        displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
                    }


                } else {
                    displayValue = value.toString();
                }

                props.add("\"" + key + "\"" + " : " + "\"" + displayValue + "\"");

            }
        }
        out.append("{ ").append(StringUtils.join(props.toArray(), " , ")).append(" }");

        return out.toString();

    }

    /**
     * Write this node as an array of strings
     */
    public String[] toStringArray() {

        List<String> props = new LinkedList<String>();

        for (String key : getPropertyKeys()) {

            Object value = getProperty(key);
            String displayValue = "";

            if (value.getClass().isPrimitive()) {
                displayValue = value.toString();

            } else if (value.getClass().isArray()) {

                if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else if (value instanceof char[]) {

                    displayValue = new String((char[]) value);

                } else if (value instanceof double[]) {

                    Double[] values = ArrayUtils.toObject((double[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof float[]) {

                    Float[] values = ArrayUtils.toObject((float[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof short[]) {

                    Short[] values = ArrayUtils.toObject((short[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof long[]) {

                    Long[] values = ArrayUtils.toObject((long[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof int[]) {

                    Integer[] values = ArrayUtils.toObject((int[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof boolean[]) {

                    Boolean[] values = (Boolean[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else {

                    Object[] values = (Object[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
                }


            } else {
                displayValue = value.toString();
            }

            props.add(displayValue);

        }

        return (String[]) props.toArray(new String[props.size()]);
    }

    /**
     * Render a node-specific view (binary)
     *
     * Should be overwritten by any node which holds binary content
     */
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        try {
            if (isVisible()) {
                out.write(getName().getBytes());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write node name to output stream: {0}", e.getStackTrace());
        }
    }

    /**
     * Get this node's template
     *
     * @return
     */
    public Template getTemplate() {

        long t0 = System.currentTimeMillis();

        if (this instanceof Template) {
            template = (Template) this;
            return template;
        }

        if (template != null) {
//            long t1 = System.currentTimeMillis();
            logger.log(Level.FINE, "Cached template found");
            return template;
        }

        // TODO: move to command and avoid to use the graph db interface directly
//        Iterable<Node> nodes = Traversal.description().relationships(RelType.HAS_CHILD, Direction.INCOMING).traverse(dbNode).nodes();

        AbstractNode startNode = this;

        while (startNode != null && !(startNode.isRootNode())) {
            List<StructrRelationship> templateRelationships = startNode.getRelationships(RelType.USE_TEMPLATE, Direction.OUTGOING);

            if (templateRelationships != null && !(templateRelationships.isEmpty())) {
                template = (Template) templateRelationships.get(0).getEndNode();
                return template;
            }

            if (template == null) {
                startNode = startNode.getParentNode();
                continue;
            }
        }
        long t1 = System.currentTimeMillis();
        logger.log(Level.FINE, "No template found in {0} ms", (t1 - t0));

        return null;

    }

    public boolean hasTemplate() {
        return (getTemplate() != null);
    }

    /**
     * Get type from underlying db node If no type property was found, return
     * info
     */
    public String getType() {
        return (String) getProperty(TYPE_KEY);
    }

    /**
     * Return node's title, or if title is null, name
     */
    public String getTitleOrName() {
        String title = getTitle();
        return title != null ? title : getName();
    }

    /**
     * Get name from underlying db node
     *
     * If name is null, return node id as fallback
     */
    public String getName() {
        Object nameProperty = getProperty(NAME_KEY);
        if (nameProperty != null) {
            return (String) nameProperty;
        } else {
            return getNodeId().toString();
        }
    }

    /**
     * Get categories
     */
    public String[] getCategories() {
        return (String[]) getProperty(CATEGORIES_KEY);
    }

    /**
     * Return title dependend of locale
     * 
     * @param locale
     * @return
     */
    public String getTitle(final Locale locale) {
        return (String) getProperty(getTitleKey(locale));
    }

    /**
     * Return title
     */
    public String getTitle() {
//        logger.log(Level.FINE, "Title without locale requested.");
//        return getTitle(new Locale("en"));
        return getStringProperty(TITLE_KEY);
    }

    public static String getTitleKey(final Locale locale) {
        return TITLE_KEY + "_" + locale;
    }

    /**
     * Get titles from underlying db node
     */
    public List<Title> getTitles() {

        List<Title> titleList = new LinkedList<Title>();

        for (Locale locale : Locale.getAvailableLocales()) {

            String title = (String) getProperty(getTitleKey(locale));

            if (title != null) {
                titleList.add(new Title(locale, title));
            }

        }

        return titleList;
    }

    /**
     * Get id from underlying db
     */
    public long getId() {
        if (isDirty) {
            return -1;
        }
        return dbNode.getId();
    }

    public Long getNodeId() {
        return getId();
    }

    public String getIdString() {
        return Long.toString(getId());
    }

//    public Long getId() {
//        return getId();
//    }
    public Date getDateProperty(final String key) {
        Object propertyValue = getProperty(key);
        if (propertyValue != null) {
            if (propertyValue instanceof Date) {
                return (Date) propertyValue;
            } else if (propertyValue instanceof Long) {
                return new Date((Long) propertyValue);
            } else if (propertyValue instanceof String) {
                try {

                    // try to parse as a number
                    return new Date(Long.parseLong((String) propertyValue));
                } catch (NumberFormatException nfe) {

                    try {
                        Date date = DateUtils.parseDate(((String) propertyValue), new String[]{"yyyymmdd", "yyyymm", "yyyy"});
                        return date;
                    } catch (ParseException ex2) {
                        logger.log(Level.WARNING, "Could not parse " + propertyValue + " to date", ex2);
                    }

                    logger.log(Level.WARNING, "Can''t parse String {0} to a Date.", propertyValue);
                    return null;
                }
            } else {
                logger.log(Level.WARNING, "Date property is not null, but type is neither Long nor String, returning null");
                return null;
            }
        }
        return null;
    }

    public String getCreatedBy() {
        return (String) getProperty(CREATED_BY_KEY);
    }

    public void setCreatedBy(final String createdBy) {
        setProperty(CREATED_BY_KEY, createdBy);
    }

    public Date getCreatedDate() {
        return getDateProperty(CREATED_DATE_KEY);
    }

    public void setCreatedDate(final Date date) {
        setProperty(CREATED_DATE_KEY, date);
    }

    public Date getLastModifiedDate() {
        return getDateProperty(LAST_MODIFIED_DATE_KEY);
    }

    public void setLastModifiedDate(final Date date) {
        setProperty(LAST_MODIFIED_DATE_KEY, date);
    }

    public Date getVisibilityStartDate() {
        return getDateProperty(VISIBILITY_START_DATE_KEY);
    }

    public void setVisibilityStartDate(final Date date) {
        setProperty(VISIBILITY_START_DATE_KEY, date);
    }

    public Date getVisibilityEndDate() {
        return getDateProperty(VISIBILITY_END_DATE_KEY);
    }

    public void setVisibilityEndDate(final Date date) {
        setProperty(VISIBILITY_END_DATE_KEY, date);
    }

    public Long getPosition() {

        Object p = getProperty(POSITION_KEY);
        Long pos;

        if (p != null) {

            if (p instanceof Long) {
                return (Long) p;
            } else if (p instanceof Integer) {

                try {
                    pos = Long.parseLong(p.toString());
                    // convert old String-based position property
                    setPosition(pos);
                } catch (NumberFormatException e) {
                    pos = getId();
                    return pos;
                }

            } else if (p instanceof String) {
                try {
                    pos = Long.parseLong(((String) p));
                    // convert old String-based position property
                    setPosition(pos);
                } catch (NumberFormatException e) {
                    pos = getId();
                    return pos;
                }
            } else {
                logger.log(Level.SEVERE, "Position property not stored as Integer or String: {0}", p.getClass().getName());
            }

        }
        return getId();

    }

    public void setPosition(final Long position) {
        setProperty(POSITION_KEY, position);
    }

    public boolean isPublic() {
        return getBooleanProperty(PUBLIC_KEY);
    }

    public boolean getPublic() {
        return getBooleanProperty(PUBLIC_KEY);
    }

    public void setPublic(final boolean publicFlag) {
        setProperty(PUBLIC_KEY, publicFlag);
    }

    public boolean isVisibleToAuthenticatedUsers() {
        return getBooleanProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY);
    }

    public boolean getVisibleToAuthenticatedUsers() {
        return getBooleanProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY);
    }

    public void setVisibleToAuthenticatedUsers(final boolean flag) {
        setProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY, flag);
    }

    public boolean isNotHidden() {
        return !getHidden();
    }

    public boolean isHidden() {
        return getHidden();
    }

    public boolean getHidden() {
        return getBooleanProperty(HIDDEN_KEY);
    }

    public void setHidden(final boolean hidden) {
        setProperty(HIDDEN_KEY, hidden);
    }

    public boolean isNotDeleted() {
        return !getDeleted();
    }

    public boolean isDeleted() {
        boolean hasDeletedFlag = getDeleted();
        boolean isInTrash = isInTrash();

        return hasDeletedFlag || isInTrash;
    }

    public boolean getDeleted() {

        return getBooleanProperty(DELETED_KEY);
    }

    public void setDeleted(final boolean deleted) {
        setProperty(DELETED_KEY, deleted);
    }

    public void setType(final String type) {
        setProperty(TYPE_KEY, type);
    }

    public void setName(final String name) {
        setProperty(NAME_KEY, name);
    }

    public void setCategories(final String[] categories) {
        setProperty(CATEGORIES_KEY, categories);
    }

    public void setTitle(final String title) {
        setProperty(TITLE_KEY, title);
    }

    /**
     * Multiple titles (one for each language)
     * 
     * @param title
     */
    public void setTitles(final String[] titles) {
        setProperty(TITLES_KEY, titles);
    }

    public void setId(final Long id) {
        //setProperty(NODE_ID_KEY, id);
        // not allowed
    }

    public void setNodeId(final Long id) {
        //setProperty(NODE_ID_KEY, id);
        // not allowed
    }

    /**
     * Return a map with all properties of this node
     * 
     * @return 
     */
    public Map<String, Object> getPropertyMap() {
        return properties;
    }

    /**
     * Return the property signature of a node
     * 
     * @return 
     */
    public Map<String, Class> getSignature() {
        Map<String, Class> signature = new HashMap<String, Class>();
        for (String key : getPropertyKeys()) {
            Object prop = getProperty(key);
            if (prop != null) {
                signature.put(key, prop.getClass());
            }
        }
        return signature;
    }

    /**
     * Return all property keys of the underlying database node
     * 
     * @return 
     */
    public Iterable<String> getPropertyKeys() {
        return dbNode.getPropertyKeys();
    }

    public Object getProperty(final String key) {

        if (isDirty) {
            return properties.get(key);
        }

        if (key != null && dbNode.hasProperty(key)) {
            return dbNode.getProperty(key);
        } else {
            return null;
        }
    }

    public String getStringProperty(final String key) {
        Object propertyValue = getProperty(key);
        String result = null;
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue instanceof String) {
            result = ((String) propertyValue);
        }
        return result;
    }

    public List<String> getStringListProperty(final String key) {
        Object propertyValue = getProperty(key);
        List<String> result = new LinkedList<String>();
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue instanceof String) {

            // Split by carriage return / line feed
            String[] values = StringUtils.split(((String) propertyValue), "\r\n");
            result = Arrays.asList(values);
        } else if (propertyValue instanceof String[]) {

            String[] values = (String[]) propertyValue;
            result = Arrays.asList(values);
        }
        return result;
    }

    public String getStringArrayPropertyAsString(final String key) {
        Object propertyValue = getProperty(key);
        StringBuilder result = new StringBuilder();
        if (propertyValue instanceof String[]) {
            int i = 0;
            String[] values = (String[]) propertyValue;
            for (String value : values) {
                result.append(value);
                if (i < values.length - 1) {
                    result.append("\r\n");
                }
            }
        }
        return result.toString();
    }

    public int getIntProperty(final String key) {
        Object propertyValue = getProperty(key);
        Integer result = null;
        if (propertyValue == null) {
            return 0;
        }
        if (propertyValue instanceof Integer) {
            result = ((Integer) propertyValue).intValue();
        } else if (propertyValue instanceof String) {
            if ("".equals((String) propertyValue)) {
                return 0;
            }
            result = Integer.parseInt(((String) propertyValue));
        }
        return result;
    }

    public long getLongProperty(final String key) {
        Object propertyValue = getProperty(key);
        Long result = null;
        if (propertyValue == null) {
            return 0L;
        }
        if (propertyValue instanceof Long) {
            result = ((Long) propertyValue).longValue();
        } else if (propertyValue instanceof String) {
            if ("".equals((String) propertyValue)) {
                return 0L;
            }
            result = Long.parseLong(((String) propertyValue));
        }
        return result;
    }

    public double getDoubleProperty(final String key) {
        Object propertyValue = getProperty(key);
        Double result = null;
        if (propertyValue == null) {
            return 0.0d;
        }
        if (propertyValue instanceof Double) {
            result = ((Double) propertyValue).doubleValue();
        } else if (propertyValue instanceof String) {
            if ("".equals((String) propertyValue)) {
                return 0.0d;
            }
            result = Double.parseDouble(((String) propertyValue));
        }
        return result;
    }

    public boolean getBooleanProperty(final String key) {
        Object propertyValue = getProperty(key);
        Boolean result = false;
        if (propertyValue == null) {
            return Boolean.FALSE;
        }
        if (propertyValue instanceof Boolean) {
            result = ((Boolean) propertyValue).booleanValue();
        } else if (propertyValue instanceof String) {
            result = Boolean.parseBoolean(((String) propertyValue));
        }
        return result;
    }

    /**
     * Set a property in database backend without updating index
     *
     * Set property only if value has changed
     * 
     * @param key
     * @param value
     */
    public void setProperty(final String key, final Object value) {
        setProperty(key, value, updateIndexDefault);
    }

    /**
     * Split String value and set as String[] property in database backend
     *
     * @param key
     * @param stringList
     *
     */
    public void setPropertyAsStringArray(final String key, final String value) {
        String[] values = StringUtils.split(((String) value), "\r\n");
        setProperty(key, values, updateIndexDefault);
    }

    /**
     * Set a property in database backend
     *
     * Set property only if value has changed
     *
     * Update index only if updateIndex is true
     *
     * @param key
     * @param value
     * @param updateIndex
     */
    public void setProperty(final String key, final Object value, final boolean updateIndex) {

        if (key == null) {
            logger.log(Level.SEVERE, "Tried to set property with null key (action was denied)");
            return;
        }


        if (isDirty) {

            // Don't write directly to database, but store property values
            // in a map for later use
            properties.put(key, value);

        } else {

            // Commit value directly to database

            Object oldValue = getProperty(key);

            // don't make any changes if
            // - old and new value both are null
            // - old and new value are not null but equal
            if ((value == null && oldValue == null)
                    || (value != null && oldValue != null && value.equals(oldValue))) {
                return;
            }

            Command transactionCommand = Services.command(TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    // save space
                    if (value == null) {
                        dbNode.removeProperty(key);
                    } else {

                        // Setting last modified date explicetely is not allowed
                        if (!key.equals(AbstractNode.LAST_MODIFIED_DATE_KEY)) {

                            if (value instanceof Date) {
                                dbNode.setProperty(key, ((Date) value).getTime());
                            } else {
                                dbNode.setProperty(key, value);

                                // set last modified date if not already happened
                                dbNode.setProperty(AbstractNode.LAST_MODIFIED_DATE_KEY, (new Date()).getTime());
                            }
                        } else {
                            logger.log(Level.FINE, "Tried to set lastModifiedDate explicitely (action was denied)");
                        }
                    }

                    // Don't automatically update index
                    // TODO: Implement something really fast to keep the index automatically in sync
                    if (updateIndex && dbNode.hasProperty(key)) {
                        Services.command(IndexNodeCommand.class).execute(getId(), key);
                    }

                    return null;
                }
            });

        }
    }

    /**
     * Discard changes and overwrite the properties map with the values
     * from database
     */
    public void discard() {
        // TODO: Implement the full pattern with commit(), discard(), init() etc.
    }

    /**
     * Commit unsaved property values to the database node.
     */
    public void commit(final User user) {

        isDirty = false;

        // Create an outer transaction to combine any inner neo4j transactions
        // to one single transaction
        Command transactionCommand = Services.command(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.command(CreateNodeCommand.class);
                AbstractNode s = (AbstractNode) createNode.execute(user);

                init(s);

                Set<String> keys = properties.keySet();
                for (String key : keys) {
                    Object value = properties.get(key);
                    if (key != null && value != null) {
                        setProperty(key, value, false); // Don't update index now!
                    }
                }
                return null;
            }
        });

    }

    /**
     * Return database node
     *
     * @return
     */
    public Node getNode() {
        return dbNode;
    }

    /**
     * Render a minimal html header
     *
     * @param out
     */
    protected void renderHeader(StringBuilder out) {
        out.append("<html><head><title>").append(getName()).append(" (Domain)</title></head><body>");
    }

    /**
     * Render a minimal html footer
     *
     * @param out
     */
    protected void renderFooter(StringBuilder out) {
        out.append("</body></html>");
    }
    /*
    @Override
    public int compareTo(AbstractNode otherNode) {
    return this.getPosition().compareTo(otherNode.getPosition());
    }
     */

    /**
     * Get path relative to given node
     * 
     * @param node
     * @return
     */
    public String getNodePath(final AbstractNode node) {

        // clicked node as reference
        String refPath = node.getNodePath();

        // currently rendered node, the link target
        String thisPath = this.getNodePath();

        String[] refParts = refPath.split("/");
        String[] thisParts = thisPath.split("/");

        int level = refParts.length - thisParts.length;

        if (level == 0) {
            // paths are identical, return last part
            return thisParts[thisParts.length - 1];

        } else if (level < 0) {
            // link down
//            return thisPath.substring(refPath.length());
            // Bug fix: Don't include the leading "/", this is a relative path!
            return thisPath.substring(refPath.length() + 1);

        } else {
            // link up
            int i = 0;
            String ret = "";
            do {
                ret = ret.concat("../");
            } while (++i < level);

            return ret.concat(thisParts[thisParts.length - 1]);

        }

    }

    /**
     * Assemble path for given node.
     *
     * This is an inverse method of @getNodeByIdOrPath.
     *
     * @param node
     * @param renderMode
     * @return
     *//*
    public String getNodePath(final AbstractNode node, final Enum renderMode) {
    
    Command nodeFactory = Services.command(NodeFactoryCommand.class);
    AbstractNode n = (AbstractNode) nodeFactory.execute(node);
    return n.getNodePath();
    }*/

    /**
     * Assemble absolute path for given node.
     *
     * @return
     */
    public String getNodePath() {

        String path = "";

        // get actual database node
        AbstractNode node = this;

        // create bean node
//        Command nodeFactory = Services.command(NodeFactoryCommand.class);
//        AbstractNode n = (AbstractNode) nodeFactory.execute(node);

        // stop at root node
        while (node != null && node.getId() > 0) {

            path = node.getName() + (!("".equals(path)) ? "/" + path : "");

            node = node.getParentNode();

            // check parent nodes
//            Relationship r = node.getSingleRelationship(RelType.HAS_CHILD, Direction.INCOMING);
//            if (r != null) {
//                node = r.getStartNode();
//                n = (AbstractNode) nodeFactory.execute(node);
//            }

        }

        return "/".concat(path); // add leading slash, because we always include the root node
    }

    /**
     * Assemble absolute path for given node.
     *
     * @return
     */
    public String getNodeXPath() {

        String xpath = "";

        // get actual database node
        AbstractNode node = this;

        // create bean node
//        Command nodeFactory = Services.command(NodeFactoryCommand.class);
//        AbstractNode n = (AbstractNode) nodeFactory.execute(node);

        // stop at root node
        while (node != null && node.getId() > 0) {

            xpath = node.getType() + "[@name='" + node.getName() + "']" + (!("".equals(xpath)) ? "/" + xpath : "");

            // check parent nodes
            node = node.getParentNode();

        }

        return "/".concat(xpath); // add leading slash, because we always include the root node
    }

//
//    /**
//     * Default: Return this node's name
//     * 
//     * @param user
//     * @param renderMode
//     * @param contextPath
//     * @return
//     */
//    public String getUrlPart() {
//        return getName();
//    }
    /**
     * Return null mime type. Method has to be overwritten,
     * returning real mime type
     */
    public String getContentType() {
        return null;
    }

    /**
     * Test: Evaluate BeanShell script in this text node.
     *
     * @return the output
     */
    public String evaluate() {
        return ("");
    }

    /**
     * Return true if this node has a relationship of given type and direction.
     *
     * @param type
     * @param dir
     * @return
     */
    public boolean hasRelationship(final RelType type, final Direction dir) {

        List<StructrRelationship> rels = this.getRelationships(type, dir);
        return (rels != null && !(rels.isEmpty()));
    }

    /**
     * Return the (cached) incoming relationship between this node and the
     * given principal which holds the security information.
     *
     * @param principal
     * @return incoming security relationship
     */
    public StructrRelationship getSecurityRelationship(final Principal principal) {

        long userId = principal.getId();

        if (securityRelationships == null) {
            securityRelationships = new HashMap<Long, StructrRelationship>();
        }

        if (!(securityRelationships.containsKey(userId))) {
            populateSecurityRelationshipCacheMap();
        }

        return securityRelationships.get(userId);

    }

    /**
     * Populate the security relationship cache map
     */
    private void populateSecurityRelationshipCacheMap() {

        if (securityRelationships == null) {
            securityRelationships = new HashMap<Long, StructrRelationship>();
        }
        // Fill cache map
        for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {
            securityRelationships.put(r.getStartNode().getId(), r);
        }

    }

    /**
     * Return all relationships of given type and direction
     *
     * @return list with relationships
     */
    public List<StructrRelationship> getRelationships(RelationshipType type, Direction dir) {
        return (List<StructrRelationship>) Services.command(NodeRelationshipsCommand.class).execute(this, type, dir);
    }

    /**
     * Return true if node is the root node
     * 
     * @return
     */
    public boolean isRootNode() {
        return getId() == 0;
    }

    /**
     * Return true if this node has child nodes
     * 
     * @return
     */
    public boolean hasChildren() {
        return (hasRelationship(RelType.HAS_CHILD, Direction.OUTGOING)
                || hasRelationship(RelType.LINK, Direction.OUTGOING));
    }
//
//    /**
//     * Return true if this node has child nodes visible for current user
//     *
//     * @return
//     */
//    public boolean hasChildren() {
//        List<StructrRelationship> childRels = getOutgoingChildRelationships();
//        List<StructrRelationship> linkRels = getOutgoingLinkRelationships();
//        return (linkRels != null && !(linkRels.isEmpty())
//                && childRels != null && !(childRels.isEmpty()));
////        return (hasRelationship(RelType.HAS_CHILD, Direction.OUTGOING)
////                || hasRelationship(RelType.LINK, Direction.OUTGOING));
//    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getDirectChildNodes() {

        return getDirectChildren(RelType.HAS_CHILD);

    }

    /**
     * Return the first parent node found.
     * 
     * @return
     */
    public AbstractNode getParentNode() {
        List<AbstractNode> parentNodes = getParentNodes();
        if (parentNodes != null && !(parentNodes.isEmpty())) {
            return parentNodes.get(0);
        } else {
            return null;
        }
    }

    /**
     * Return sibling nodes. Follows the HAS_CHILD relationship
     *
     * @return
     */
    public List<AbstractNode> getSiblingNodes() {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        AbstractNode parentNode = getParentNode();

        if (parentNode != null) {


            Command nodeFactory = Services.command(NodeFactoryCommand.class);

            Command relsCommand = Services.command(NodeRelationshipsCommand.class);
            List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(parentNode, RelType.HAS_CHILD, Direction.OUTGOING);

            for (StructrRelationship r : rels) {

                AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());
                if (s.readAllowed()) {
                    nodes.add(s);
                }

            }
        }
        return nodes;

    }

    /**
     * 
     * Returns true if an ancestor node is a Trash node
     */
    public boolean isInTrash() {
        List<AbstractNode> ancestors = getAncestorNodes();
        for (AbstractNode node : ancestors) {
            if (node instanceof Trash) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return all ancestor nodes. Follows the INCOMING HAS_CHILD relationship
     * and stops at the root node.
     *
     * @return
     */
    public List<AbstractNode> getAncestorNodes() {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        List<StructrRelationship> rels = getIncomingChildRelationships();

        for (StructrRelationship r : rels) {

            AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());
            if (s.readAllowed()) {
                nodes.add(s);
            }

        }

        return nodes;

    }

    /**
     * Return parent nodes. Follows the INCOMING HAS_CHILD relationship
     * 
     * @return
     */
    public List<AbstractNode> getParentNodes() {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        List<StructrRelationship> rels = getIncomingChildRelationships();

        for (StructrRelationship r : rels) {

            AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());
            if (s.readAllowed()) {
                nodes.add(s);
            }

        }

        return nodes;

    }

    /**
     * Cached list of all relationships
     *
     * @return
     */
    public List<StructrRelationship> getRelationships() {

        if (allRelationships == null) {
            allRelationships = getRelationships(null, Direction.BOTH);
        }
        return allRelationships;
    }

    /**
     * Get all relationships of given direction
     *
     * @return
     */
    public List<StructrRelationship> getRelationships(Direction dir) {
        return getRelationships(null, dir);
    }

    /**
     * Cached list of incoming relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingRelationships() {

        if (incomingRelationships == null) {
            incomingRelationships = getRelationships(null, Direction.INCOMING);
        }
        return incomingRelationships;
    }

    /**
     * Cached list of outgoing relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingRelationships() {

        if (outgoingRelationships == null) {
            outgoingRelationships = getRelationships(null, Direction.OUTGOING);
        }
        return outgoingRelationships;
    }

    /**
     * Non-cached list of outgoing relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingRelationships(final RelationshipType type) {
        return getRelationships(type, Direction.OUTGOING);
    }

    /**
     * Cached list of incoming link relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingLinkRelationships() {

        if (incomingLinkRelationships == null) {
            incomingLinkRelationships = getRelationships(RelType.LINK, Direction.INCOMING);
        }
        return incomingLinkRelationships;
    }

    /**
     * Cached list of outgoing data relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingDataRelationships() {

        if (outgoingDataRelationships == null) {
            outgoingDataRelationships = getRelationships(RelType.DATA, Direction.OUTGOING);
        }
        return outgoingDataRelationships;
    }

    /**
     * Cached list of incoming data relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingDataRelationships() {

        if (incomingDataRelationships == null) {
            incomingDataRelationships = getRelationships(RelType.DATA, Direction.INCOMING);
        }
        return incomingDataRelationships;
    }

    /**
     * Cached list of outgoing link relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingLinkRelationships() {

        if (outgoingLinkRelationships == null) {
            outgoingLinkRelationships = getRelationships(RelType.LINK, Direction.OUTGOING);
        }
        return outgoingLinkRelationships;
    }

    /**
     * Cached list of incoming child relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingChildRelationships() {

        if (incomingChildRelationships == null) {
            incomingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.INCOMING);
        }
        return incomingChildRelationships;
    }

    /**
     * Cached list of outgoing child relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingChildRelationships() {

        if (outgoingChildRelationships == null) {
            outgoingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.OUTGOING);
        }
        return outgoingChildRelationships;
    }

    /**
     * Return unordered list of all directly linked nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getLinkedNodes() {

        return getDirectChildren(RelType.LINK);

    }

    /**
     * Return ordered list of all directly linked nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getSortedLinkedNodes() {

        return getSortedDirectChildren(RelType.LINK);

    }

    /**
     * Return unordered list of all child nodes (recursively)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getAllChildren() {
        return getAllChildren(null);
    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     * with given relationship type
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getDirectChildren(final RelationshipType relType) {
        return getDirectChildren(relType, null);
    }

    /**
     * Return ordered list of all direct child nodes (no recursion)
     * with given relationship type
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getSortedDirectChildren(final RelationshipType relType) {
        List<AbstractNode> nodes = getDirectChildren(relType, null);
        Collections.sort(nodes);
        return nodes;
    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     * with given relationship type and given node type.
     *
     * Given user must have read access.
     *
     * @return list with structr nodes
     */
    private List<AbstractNode> getDirectChildren(final RelationshipType relType, final String nodeType) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

//        Command nodeFactory = null;
//        if (relType.equals(RelType.LINK)) {
//            nodeFactory = Services.command(LinkNodeFactoryCommand.class);
//        } else {
//            nodeFactory = Services.command(NodeFactoryCommand.class);
//        }

//        Command relsCommand = Services.command(NodeRelationshipsCommand.class);
//        List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(this, relType, Direction.OUTGOING);

        List<StructrRelationship> rels = this.getOutgoingRelationships(relType);

        for (StructrRelationship r : rels) {

            AbstractNode s = r.getEndNode();

            if (s.readAllowed() && (nodeType == null || nodeType.equals(s.getType()))) {
                nodes.add(s);
            }

        }
        return nodes;
    }

    /**
     * Get child nodes and sort them before returning
     *
     * @return
     */
    public List<AbstractNode> getSortedDirectChildNodes() {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();
        nodes.addAll(getDirectChildNodes());

        // sort by position
        Collections.sort(nodes, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return nodes;
    }

    /**
     * Get child nodes and sort them before returning
     *
     * @return
     */
    public List<AbstractNode> getSortedDirectChildNodes(final String sortKey, final String sortOrder) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();
        nodes.addAll(getDirectChildNodes());

        // sort by key, order by order {@see AbstractNodeComparator.ASCENDING} or {@see AbstractNodeComparator.DESCENDING}
        Collections.sort(nodes, new AbstractNodeComparator(sortKey, sortOrder));

        return nodes;
    }

    /**
     * Get direct child nodes, link nodes, and sort them before returning
     *
     * @return
     */
    public List<AbstractNode> getDirectChildAndLinkNodes() {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();
        nodes.addAll(getDirectChildNodes());

        // get linked child nodes
        nodes.addAll(getLinkedNodes());

        return nodes;
    }

    /**
     * Get direct child nodes, link nodes, and sort them before returning
     * 
     * @return
     */
    public List<AbstractNode> getSortedDirectChildAndLinkNodes() {

        List<AbstractNode> nodes = getDirectChildAndLinkNodes();

        // sort by position
        Collections.sort(nodes, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return nodes;
    }

    /**
     * Get menu items and sort them before returning.
     *
     * @return
     */
    public List<AbstractNode> getSortedMenuItems() {

        List<AbstractNode> menuItems = new LinkedList<AbstractNode>();

        // add direct children of type MenuItem
        menuItems.addAll(getDirectChildren(RelType.HAS_CHILD, "MenuItem"));

        // add linked children, f.e. direct links to pages
        menuItems.addAll(getDirectChildren(RelType.LINK));

        // sort by position
        Collections.sort(menuItems, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return menuItems;
    }

    /**
     * Return unordered list of all child nodes (recursively)
     * with given relationship type and given node type.
     *
     * Given user must have read access.
     *
     * @param nodeType node type filter, can be null
     * @param user
     * @return list with structr nodes
     */
    protected List<AbstractNode> getAllChildren(final String nodeType) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command findNode = Services.command(FindNodeCommand.class);
        List<AbstractNode> result = (List<AbstractNode>) findNode.execute(user, this);

        for (AbstractNode s : result) {

            if (s.readAllowed() && (nodeType == null || nodeType.equals(s.getType()))) {
                nodes.add(s);
            }

        }
        return nodes;
    }

    /**
     * Check visibility of given node, used for rendering in view mode
     *
     * @return
     */
    public boolean isVisible() {

        if (user instanceof SuperUser) {
            // Super user may always see it
            return true;
        }

        // check hidden flag (see STRUCTR-12)
        if (isHidden()) {
            return false;
        }

        boolean visibleByTime = false;

        // check visibility period of time (see STRUCTR-13)
        Date visStartDate = getVisibilityStartDate();

        long effectiveStartDate = 0L;
        Date createdDate = getCreatedDate();
        if (createdDate != null) {
            effectiveStartDate = Math.max(createdDate.getTime(), 0L);
        }

        // if no start date for visibility is given,
        // take the maximum of 0 and creation date.
        visStartDate = (visStartDate == null ? new Date(effectiveStartDate) : visStartDate);

        // if no end date for visibility is given,
        // take the Long.MAX_VALUE
        Date visEndDate = getVisibilityEndDate();
        visEndDate = (visEndDate == null ? new Date(Long.MAX_VALUE) : visEndDate);

        Date now = new Date();

        visibleByTime = (now.after(visStartDate) && now.before(visEndDate));


        if (user == null) {

            // No logged-in user

            if (isPublic()) {
                return visibleByTime;
            } else {
                return false;
            }

        } else {

            // Logged-in users

            if (isVisibleToAuthenticatedUsers()) {
                return visibleByTime;
            } else {
                return false;
            }

        }

    }

    /**
     * Return true if principal has the given permission
     * 
     * @param permission
     * @param principal
     * @return
     */
    private boolean hasPermission(final String permission, final Principal principal) {

        // just in case ...
        if (principal == null || permission == null) {
            return false;
        }

        // superuser
        if (principal instanceof SuperUser) {
            return true;
        }

        // user has full control over his/her own user node
        if (this.equals(principal)) {
            return true;
        }


        StructrRelationship r = getSecurityRelationship(principal);

        if (r != null && r.isAllowed(permission)) {
            return true;
        }

        // Check group

        // We cannot use getParent() here because it uses hasPermission itself,
        // that would lead to an infinite loop
        List<StructrRelationship> rels = principal.getIncomingChildRelationships();
        for (StructrRelationship sr : rels) {
            AbstractNode node = sr.getStartNode();

            if (!(node instanceof Group)) {
                continue;
            }

            Group group = (Group) node;

            r = getSecurityRelationship(group);

            if (r != null && r.isAllowed(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if given node may be read by current user.
     *
     * @return
     */
    public boolean readAllowed() {

        // Check global settings first
        if (isVisible()) {
            return true;
        }

        // Then check per-user permissions
        return hasPermission(StructrRelationship.READ_KEY, user);
    }

    /**
     * Check if given node may see the navigation tree
     *
     * @return
     */
    public boolean showTreeAllowed() {
        return hasPermission(StructrRelationship.SHOW_TREE_KEY, user);
    }

    /**
     * Check if given node may be written by current user.
     *
     * @return
     */
    public boolean writeAllowed() {
        return hasPermission(StructrRelationship.WRITE_KEY, user);
    }

    /**
     * Check if given user may create new sub nodes.
     *
     * @return
     */
    public boolean createSubnodeAllowed() {
        return hasPermission(StructrRelationship.CREATE_SUBNODE_KEY, user);
    }

    /**
     * Check if given user may delete this node
     *
     * @return
     */
    public boolean deleteNodeAllowed() {
        return hasPermission(StructrRelationship.DELETE_NODE_KEY, user);
    }

    /**
     * Check if given user may add new relationships to this node
     *
     * @return
     */
    public boolean addRelationshipAllowed() {
        return hasPermission(StructrRelationship.ADD_RELATIONSHIP_KEY, user);
    }

    /**
     * Check if given user may edit (set) properties of this node
     *
     * @return
     */
    public boolean editPropertiesAllowed() {
        return hasPermission(StructrRelationship.EDIT_PROPERTIES_KEY, user);
    }

    /**
     * Check if given user may remove relationships to this node
     *
     * @return
     */
    public boolean removeRelationshipAllowed() {
        return hasPermission(StructrRelationship.REMOVE_RELATIONSHIP_KEY, user);
    }

    /**
     * Check if access of given node may be controlled by current user.
     *
     * @return
     */
    public boolean accessControlAllowed() {

        // just in case ...
        if (user == null) {
            return false;
        }

        // superuser
        if (user instanceof SuperUser) {
            return true;
        }

        // node itself
        if (this.equals(user)) {
            return true;
        }

        StructrRelationship r = null;


        // owner has always access control
        if (user.equals(getOwnerNode())) {
            return true;
        }

        r = getSecurityRelationship(user);

        if (r != null && r.isAllowed(StructrRelationship.ACCESS_CONTROL_KEY)) {
            return true;
        }

        return false;
    }

    /**
     * Return owner node
     *
     * @return
     */
    public User getOwnerNode() {
        for (StructrRelationship s : getRelationships(RelType.OWNS, Direction.INCOMING)) {
            AbstractNode n = s.getStartNode();
            if (n instanceof User) {
                return (User) n;
            }
            logger.log(Level.SEVERE, "Owner node is not a user: {0}[{1}]", new Object[]{n.getName(), n.getId()});
        }
        return null;
    }

    public Long getTypeNodeId() {
        NodeType n = getTypeNode();
        return (n != null ? n.getId() : null);
    }

    /**
     * Return type node
     *
     * @return
     */
    public NodeType getTypeNode() {
        for (StructrRelationship s : getRelationships(RelType.TYPE, Direction.OUTGOING)) {
            AbstractNode n = s.getEndNode();
            if (n instanceof NodeType) {
                return (NodeType) n;
            }
        }
        return null;
    }

    public void setTypeNodeId(final Long value) {

        // find type node
        Command findNode = Services.command(FindNodeCommand.class);
        NodeType typeNode = (NodeType) findNode.execute(new SuperUser(), value);

        // delete existing type node relationships
        List<StructrRelationship> templateRels = this.getOutgoingRelationships(RelType.TYPE);
        Command delRel = Services.command(DeleteRelationshipCommand.class);
        if (templateRels != null) {
            for (StructrRelationship r : templateRels) {
                delRel.execute(r);
            }
        }

        // create new link target relationship
        Command createRel = Services.command(CreateRelationshipCommand.class);
        createRel.execute(this, typeNode, RelType.TYPE);
    }

    /**
     * Return owner
     *
     * @return
     */
    public String getOwner() {
        User ownner = getOwnerNode();
        return (ownner != null ? ownner.getRealName() + " (" + ownner.getName() + ")" : null);
    }

    /**
     * Return a list with the connected principals (user, group, role)
     * @return
     */
    public List<AbstractNode> getSecurityPrincipals() {

        List<AbstractNode> principalList = new LinkedList<AbstractNode>();

        // check any security relationships
        for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {

            // check security properties
            AbstractNode principalNode = r.getEndNode();

            principalList.add(principalNode);

        }
        return principalList;
    }

    /**
     * Replace $(key) by the content rendered by the subnode with name "key"
     *
     * @param content
     * @param node
     * @param editUrl
     * @param editNodeId
     */
    protected void replaceBySubnodes(StringBuilder content, final AbstractNode startNode, final String editUrl, final Long editNodeId) {

//        List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes(user);
        List<AbstractNode> callingNodeSubnodes = null;
        List<AbstractNode> callingNodeSubnodesAndLinkedNodes = null;

        template = startNode.getTemplate();
        AbstractNode callingNode = null;
        if (template != null) {

            callingNode = template.getCallingNode();
            if (callingNode != null) {
                callingNodeSubnodesAndLinkedNodes = callingNode.getSortedDirectChildAndLinkNodes();
                callingNodeSubnodes = callingNode.getSortedDirectChildNodes();
            }
        }

        Command findNode = Services.command(FindNodeCommand.class);

        int start = content.indexOf(NODE_KEY_PREFIX);
        while (start > -1) {

            int end = content.indexOf(NODE_KEY_SUFFIX, start + NODE_KEY_PREFIX.length());

            if (end < 0) {
                logger.log(Level.WARNING, "Node key suffix {0} not found in template {1}", new Object[]{NODE_KEY_SUFFIX, template.getName()});
                break;
            }

            String key = content.substring(start + NODE_KEY_PREFIX.length(), end);

            int indexOfComma = key.indexOf(",");
            int indexOfDot = key.indexOf(".");

            String templateKey = null;
            String methodKey = null;
            Template customTemplate = null;

            if (indexOfComma > 0) {
                String[] splitted = StringUtils.split(key, ",");
                key = splitted[0];
                templateKey = splitted[1];

                if (StringUtils.isNotEmpty(templateKey)) {
                    customTemplate = (Template) findNode.execute(user, this, new XPath(templateKey));
                }

            } else if (indexOfDot > 0) {
                String[] splitted = StringUtils.split(key, ".");
                key = splitted[0];
                methodKey = splitted[1];
            }

            StringBuilder replacement = new StringBuilder();

            if (callingNode != null && key.equals(CALLING_NODE_SUBNODES_KEY)) {

                // render subnodes in correct order
                for (AbstractNode s : callingNodeSubnodes) {

                    // propagate request and template
//                    s.setRequest(request);
                    s.renderView(replacement, startNode, editUrl, editNodeId);
                }

            } else if (callingNode != null && key.equals(CALLING_NODE_SUBNODES_AND_LINKED_NODES_KEY)) {

                // render subnodes in correct order
                for (AbstractNode s : callingNodeSubnodesAndLinkedNodes) {

                    // propagate request and template
//                    s.setRequest(request);
                    s.renderView(replacement, startNode, editUrl, editNodeId);
                }

            } else { //if (key.startsWith("/") || key.startsWith("count(")) {
                // use XPath notation


                // search relative to calling node
                //List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, callingNode, new XPath(key));

//                Object result = findNode.execute(user, this, new XPath(key));
                Object result = findNode.execute(user, this, key);

                if (result instanceof List) {

                    // get referenced nodes relative to the template
                    List<AbstractNode> nodes = (List<AbstractNode>) result;

                    if (nodes != null) {
                        for (AbstractNode s : nodes) {

                            if (customTemplate != null) {
                                s.setTemplate(customTemplate);
                            }

                            // propagate request
//                            s.setRequest(getRequest());
                            s.renderView(replacement, startNode, editUrl, editNodeId);
                        }
                    }
                } else if (result instanceof AbstractNode) {

                    AbstractNode s = (AbstractNode) result;

                    if (customTemplate != null) {
                        s.setTemplate(customTemplate);
                    }

                    // propagate request
//                    s.setRequest(getRequest());
                    if (StringUtils.isNotEmpty(methodKey)) {

                        methodKey = toGetter(methodKey);

                        Method getter = null;
                        try {
                            getter = s.getClass().getMethod(methodKey);
                            Object value = null;
                            try {
                                value = getter.invoke(s);
                                replacement.append(value);
                            } catch (Exception ex) {
                                logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[]{getter, s});
                            }

                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Cannot invoke method {0}", methodKey);
                        }

                    } else {
                        s.renderView(replacement, startNode, editUrl, editNodeId);
                    }

                } else {
                    replacement.append(result);

                }

            }
//            else {
//
//                // subnodes of this object
//                for (AbstractNode s : subnodes) {
//
//                    if (key.equals(s.getName())) {
//
//                        // propagate request
//                        s.setRequest(getRequest());
//                        s.renderView(replacement, startNode, editUrl, editNodeId, user);
//
//                    }
//                }
//            }
            String replaceBy = replacement.toString();

            content.replace(start, end
                    + NODE_KEY_SUFFIX.length(), replaceBy);
            // avoid replacing in the replacement again
            start = content.indexOf(NODE_KEY_PREFIX, start + replaceBy.length() + 1);
        }

    }

    /**
     * Generic getter for use with Freemarker template language
     * 
     * @param key
     * @return
     */
    public Object get(final String key) {

        if (key == null) {
            return null;
        }

        Object propertyValue = this.getProperty(key);

        if (propertyValue != null) {
            return propertyValue;
        }

        List<AbstractNode> subnodes = this.getDirectChildAndLinkNodes();

        for (AbstractNode node : subnodes) {
            if (key.equals(node.getName())) {
                return node;
            }
        }

        // nothing found
        return null;

    }

    public Set<AbstractNode> getRelatedNodes(int maxDepth) {
        return (getRelatedNodes(maxDepth, Integer.MAX_VALUE, null));
    }

    public Set<AbstractNode> getRelatedNodes(int maxDepth, String relTypes) {
        return (getRelatedNodes(maxDepth, Integer.MAX_VALUE, relTypes));
    }

    public Set<AbstractNode> getRelatedNodes(int maxDepth, int maxNum) {
        return (getRelatedNodes(maxDepth, maxNum, null));
    }

    public Set<AbstractNode> getRelatedNodes(int maxDepth, int maxNum, String relTypes) {
        Set<AbstractNode> visitedNodes = new LinkedHashSet<AbstractNode>();
        Set<AbstractNode> nodes = new LinkedHashSet<AbstractNode>();
        Set<StructrRelationship> rels = new LinkedHashSet<StructrRelationship>();

        collectRelatedNodes(nodes, rels, visitedNodes, this, 0, maxDepth, maxNum, splitRelationshipTypes(relTypes));

        return (nodes);
    }

    public Set<StructrRelationship> getRelatedRels(int maxDepth) {
        return (getRelatedRels(maxDepth, Integer.MAX_VALUE, null));
    }

    public Set<StructrRelationship> getRelatedRels(int maxDepth, String relTypes) {
        return (getRelatedRels(maxDepth, Integer.MAX_VALUE, relTypes));
    }

    public Set<StructrRelationship> getRelatedRels(int maxDepth, int maxNum) {
        return (getRelatedRels(maxDepth, maxNum, null));
    }

    public Set<StructrRelationship> getRelatedRels(int maxDepth, int maxNum, String relTypes) {
        Set<AbstractNode> visitedNodes = new LinkedHashSet<AbstractNode>();
        Set<AbstractNode> nodes = new LinkedHashSet<AbstractNode>();
        Set<StructrRelationship> rels = new LinkedHashSet<StructrRelationship>();

        collectRelatedNodes(nodes, rels, visitedNodes, this, 0, maxDepth, maxNum, splitRelationshipTypes(relTypes));

        return (rels);
    }

    private RelationshipType[] splitRelationshipTypes(String relTypes) {
        if (relTypes != null) {
            List<RelationshipType> relTypeList = new ArrayList<RelationshipType>(10);
            for (String type : relTypes.split("[, ]+")) {
                relTypeList.add(DynamicRelationshipType.withName(type));
            }

            return (relTypeList.toArray(new RelationshipType[0]));
        }

        return (null);
    }

    /**
     * Recursively add Html / JavaScript nodes to the springy graph
     *
     * @param buffer
     * @param currentNode
     * @param depth
     * @param maxDepth
     */
    private void collectRelatedNodes(Set<AbstractNode> nodes, Set<StructrRelationship> rels, Set<AbstractNode> visitedNodes, AbstractNode currentNode, int depth, int maxDepth, int maxNum, RelationshipType... relTypes) {
        if (depth >= maxDepth) {
            return;
        }

        if (nodes.size() < maxNum) {
            nodes.add(currentNode);

            // collect incoming relationships
            List<StructrRelationship> inRels = new LinkedList<StructrRelationship>();
            if (relTypes != null && relTypes.length > 0) {
                for (RelationshipType type : relTypes) {
                    inRels.addAll(currentNode.getRelationships(type, Direction.INCOMING));
                }

            } else {
                inRels = currentNode.getIncomingRelationships();
            }

            for (StructrRelationship rel : inRels) {
                AbstractNode startNode = rel.getStartNode();
                if (startNode != null) {
                    nodes.add(startNode);
                    rels.add(rel);

                    collectRelatedNodes(nodes, rels, visitedNodes, startNode, depth + 1, maxDepth, maxNum, relTypes);
                }
            }

            // collect outgoing relationships
            List<StructrRelationship> outRels = new LinkedList<StructrRelationship>();
            if (relTypes != null && relTypes.length > 0) {
                for (RelationshipType type : relTypes) {
                    outRels.addAll(currentNode.getRelationships(type, Direction.OUTGOING));
                }

            } else {
                outRels = currentNode.getOutgoingRelationships();
            }

            for (StructrRelationship rel : outRels) {
                AbstractNode endNode = rel.getEndNode();
                if (endNode != null) {
                    nodes.add(endNode);
                    rels.add(rel);

                    collectRelatedNodes(nodes, rels, visitedNodes, endNode, depth + 1, maxDepth, maxNum, relTypes);
                }
            }

            // visitedNodes.add(currentNode);
        }
    }

    // ----- protected methods -----
    protected static String toGetter(String name) {
        return "get".concat(name.substring(0, 1).toUpperCase()).concat(name.substring(1));
    }

    /**
     * Replace ${key} by the value of calling node's property with the name "key".
     *
     * Alternatively, propertyName can be
     *
     * @param content
     * @param node
     * @param editUrl
     * @param editNodeId
     */
//    protected void replaceByPropertyValues(StringBuilder content, final AbstractNode startNode, final String editUrl, final Long editNodeId) {
//        // start with first occurrence of key prefix
//        int start = content.indexOf(keyPrefix);
//
//        while (start > -1) {
//
//            int end = content.indexOf(keySuffix, start + keyPrefix.length());
//            String key = content.substring(start + keyPrefix.length(), end);
//
//            StringBuilder replacement = new StringBuilder();
//
//            // special placeholder for calling node's child nodes
//            if (key.equals(CALLING_NODE_KEY)) {
//
//                List<AbstractNode> subnodes = callingNode.getSortedDirectChildAndLinkNodes();
//
//                // render subnodes in correct order
//                for (AbstractNode s : subnodes) {
//
//                    // propagate request
//                    s.setRequest(getRequest());
//                    s.renderView(replacement, startNode, editUrl, editNodeId);
//                }
//
//            } else if (callingNode.getNode() != null && callingNode.getNode().hasProperty(key)) {
//                // then, look for a property with name=key
//                replacement.append(callingNode.getProperty(key));
//            }
//            // moved to replaceBySubnodes
////            } else {
////
////                // use XPath notation
////                Command findNode = Services.command(FindNodeCommand.class);
////
////                // search relative to calling node
////                //List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, callingNode, new XPath(key));
////
////                // get referenced nodes relative to the template
////                List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, this, new XPath(key));
////
////                if (nodes != null) {
////                    for (AbstractNode s : nodes) {
////                        // propagate request
////                        s.setRequest(getRequest());
////                        s.renderView(replacement, startNode, editUrl, editNodeId);
////                    }
////                }
////
////            }
//            String replaceBy = replacement.toString();
//            content.replace(start, end + keySuffix.length(), replaceBy);
//            // avoid replacing in the replacement again
//            start = content.indexOf(keyPrefix, start + replaceBy.length() + 1);
//        }
//    }
    protected void replaceByFreeMarker(final String templateString, Writer out, final AbstractNode startNode, final String editUrl, final Long editNodeId) {

        Configuration cfg = new Configuration();

        // TODO: enable access to content tree, see below (Content variable)
        //cfg.setSharedVariable("Tree", new StructrTemplateNodeModel(this));

        try {

            AbstractNode callingNode = null;

            if (getTemplate() != null) {

                callingNode = template.getCallingNode();

            } else {

                callingNode = startNode;
            }

            Map root = new HashMap();

            root.put("this", this);

            if (callingNode != null) {
                root.put(callingNode.getType(), callingNode);
                root.put("CallingNode", callingNode);
            }

            HttpServletRequest request = CurrentRequest.getRequest();
            if (request != null) {
                //root.put("Request", new freemarker.template.SimpleHash(request.getParameterMap().));
                root.put("Request", new freemarker.ext.servlet.HttpRequestParametersHashModel(request));

                String searchString = request.getParameter("search");
                String searchInContent = request.getParameter("searchInContent");

                boolean inContent = StringUtils.isNotEmpty(searchInContent) && Boolean.parseBoolean(searchInContent) ? true : false;

                // if search string is given, put search results into freemarker model
                if (searchString != null && !(searchString.isEmpty())) {

                    List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
                    searchAttrs.add(Search.orName(searchString)); // search in name

                    if (inContent) {
                        searchAttrs.add(Search.orContent(searchString)); // search in name
                    }

                    Command search = Services.command(SearchNodeCommand.class);
                    List<AbstractNode> result = (List<AbstractNode>) search.execute(
                            null, // user => null means super user
                            null, // top node => null means search all
                            false, // include hidden
                            true, // public only
                            searchAttrs);

                    root.put("SearchResults", result);
                }
            }

            if (user != null) {
                root.put("User", user);
            }

            // Add a generic helper
            root.put("Helper", new TemplateHelper());

            // Add error and ok message if present
            HttpSession session = CurrentRequest.getSession();
            if (session != null) {
                if (session.getAttribute("errorMessage") != null) {
                    root.put("ErrorMessage", session.getAttribute("errorMessage"));
                }

                if (session.getAttribute("okMessage") != null) {
                    root.put("OkMessage", session.getAttribute("okMessage"));
                }
            }

            // add geo info if available
            // TODO: add geo node information


            //root.put("Content", new StructrTemplateNodeModel(this, startNode, editUrl, editNodeId, user));
            //root.put("ContextPath", callingNode.getNodePath(startNode));

            String name = template != null ? template.getName() : getName();

            freemarker.template.Template t = new freemarker.template.Template(name, new StringReader(templateString), cfg);
            t.process(root, out);


            out.flush();

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error: {0}", t.getMessage());
        }

    }

    protected static void staticReplaceByFreeMarker(final String templateString, Writer out, final AbstractNode node, final String editUrl, final Long editNodeId) {

        Configuration cfg = new Configuration();

        // TODO: enable access to content tree, see below (Content variable)
        //cfg.setSharedVariable("Tree", new StructrTemplateNodeModel(this));

        try {

            AbstractNode callingNode = null;

            if (templateString != null) {

                Map root = new HashMap();
                root.put("Template", node);

                if (callingNode != null) {
                    root.put(callingNode.getType(), callingNode);
                }

                HttpServletRequest request = CurrentRequest.getRequest();
                if (request != null) {
                    //root.put("Request", new freemarker.template.SimpleHash(request.getParameterMap().));
                    root.put("Request", new freemarker.ext.servlet.HttpRequestParametersHashModel(request));

                    // if search string is given, put search results into freemarker model
                    String searchString = request.getParameter("search");
                    if (searchString != null && !(searchString.isEmpty())) {
                        Command search = Services.command(SearchNodeCommand.class);
                        List<AbstractNode> result = (List<AbstractNode>) search.execute(
                                null, // user => null means super user
                                null, // top node => null means search all
                                false, // include hidden
                                true, // public only
                                Search.orName(searchString)); // search in name
                        root.put("SearchResults", result);
                    }
                }

                //if (user != null) {
                root.put("User", CurrentRequest.getCurrentUser());
                //}

                // Add a generic helper
                root.put("Helper", new TemplateHelper());

                // Add error and ok message if present
                HttpSession session = CurrentRequest.getSession();
                if (session != null) {
                    if (session.getAttribute("errorMessage") != null) {
                        root.put("ErrorMessage", session.getAttribute("errorMessage"));
                    }

                    if (session.getAttribute("errorMessage") != null) {
                        root.put("OkMessage", session.getAttribute("okMessage"));
                    }
                }

                freemarker.template.Template t = new freemarker.template.Template(node.getName(), new StringReader(templateString), cfg);
                t.process(root, out);

            } else {

                // if no template is given, just copy the input
                out.write(templateString);
                out.flush();

            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error: {0}", t.getMessage());
        }

    }

    // ----- protected methods -----
    protected String createUniqueIdentifier(String prefix) {
        StringBuilder ret = new StringBuilder(100);

        ret.append(prefix);
        ret.append(getIdString());

        return (ret.toString());
    }

    protected AbstractNode getNodeFromLoader() {
        List<StructrRelationship> rels = getIncomingDataRelationships();
        AbstractNode ret = null;

        for (StructrRelationship rel : rels) {
            // first one wins
            AbstractNode startNode = rel.getStartNode();
            if (startNode instanceof NodeSource) {
                NodeSource source = (NodeSource) startNode;
                ret = source.loadNode();
                break;
            }
        }

        return (ret);
    }
}
