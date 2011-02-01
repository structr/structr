/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;

/**
 *
 * Import helper
 *
 * @author axel
 */
public class Imp {

    public static void setStringPropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (StringUtils.isNotBlank(p)) {
            node.setProperty(propertyKey, p);
        }
    }

    public static void setStringArrayPropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (StringUtils.isNotBlank(p)) {
            node.setProperty(propertyKey, new String[]{p});
        }
    }

    public static void setLongPropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (StringUtils.isNotBlank(p)) {
            long result = -1; // default
            try {
                if (StringUtils.isNotBlank(p)) {
                    result = Long.parseLong(p);
                    node.setProperty(propertyKey, result);
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse " + p + " to Long in field " + databaseKey + "");
            }

        }
    }

    public static void setDoublePropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (StringUtils.isNotBlank(p)) {
            double result = 0d; // default
            try {
                if (StringUtils.isNotBlank(p)) {
                    result = Double.parseDouble(p);
                    node.setProperty(propertyKey, result);
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse " + p + " to Double in field " + databaseKey + "");
            }

        }
    }

    public static void setDatePropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (StringUtils.isNotBlank(p)) {
            Date result = null; // default
            try {
                if (StringUtils.isNotBlank(p)) {
                    SimpleDateFormat sdf = new SimpleDateFormat();
                    result = sdf.parse(p);
                    node.setProperty(propertyKey, result);
                }
            } catch (ParseException pe1) {

                try {
                    Long l = Long.parseLong(p);
                    result = new Date(l.longValue());
                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + p + " to Date in field " + databaseKey + "");
                }

            }

        }
    }

    public static void setBooleanPropertyIfNotEmpty(Node node, Element e, String propertyKey, String databaseKey) {
        String p = e.elementTextTrim(databaseKey);
        if (p != null) {
            boolean result = false; // default
            try {
                if (StringUtils.isNotBlank(p)) {
                    result = Boolean.parseBoolean(p);
                    node.setProperty(propertyKey, result);
                }
            } catch (Exception be) {
                System.out.println("Could not parse " + p + " to Boolean in field " + databaseKey + "");
            }

        }
    }

    public static Node createNode(final String type, final String name, final Node rootNode) {
        // search for existing node
        for (Node n : Traversal.description().depthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(rootNode).nodes()) {

            if (n.hasProperty("type") && n.getProperty("type").equals(type)
                    && n.hasProperty("name") && n.getProperty("name").equals(name)) {
                return n;
            }
        }

        Node node = rootNode.getGraphDatabase().createNode();
        node.setProperty("name", name);
        node.setProperty("type", type);
        rootNode.createRelationshipTo(node, RelType.HAS_CHILD);
        return node;

    }

    public static Node createOrLinkNode(final String type, final String name, final Node toNode, final Node parentNode) {

        Node node = null;

        // search for existing node
        for (Node n : Traversal.description().depthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(parentNode).nodes()) {

            if (n.hasProperty("type") && n.getProperty("type").equals(type)
                    && n.hasProperty("name") && n.getProperty("name").equals(name)) {

                // found
                node = n;

                // ignore dupes
                break;

            }
        }

        if (node == null) {
            // create new node
            node = createNode(type, name, parentNode);
        }

        linkNode(node, toNode, RelType.LINK);

        return node;

    }

    public static void linkNode(final Node fromNode, final Node toNode, RelationshipType relType) {

        // test if such a relationship exists already
        if (fromNode.hasRelationship(relType, Direction.OUTGOING)) {
            Iterable<Relationship> rels = fromNode.getRelationships(relType, Direction.OUTGOING);

            for (Relationship r : rels) {
                Node endNode = r.getEndNode();
                if (endNode.equals(toNode)) {
                    return;
                }
            }

        }

        fromNode.createRelationshipTo(toNode, relType);
    }

    public static void indexNode(IndexService index, final Node n) {
        for (String propertyKey : n.getPropertyKeys()) {
            index.index(n, propertyKey, n.getProperty(propertyKey));
        }

    }

    public static void reIndexNode(IndexService index, final Node n) {
        for (String propertyKey : n.getPropertyKeys()) {
            index.removeIndex(n, propertyKey);
            index.index(n, propertyKey, n.getProperty(propertyKey));
        }

    }

    public static String nvl(Object value) {
        if (value != null && value instanceof String && !((String) value).isEmpty()) {
            return (String) value;
        } else {
            return "";
        }
    }

    public static Node findNode(final String type, final String name, final Node rootNode) {
        // search for existing node
        for (Node n : Traversal.description().depthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(rootNode).nodes()) {

            if (n.hasProperty("type") && n.getProperty("type").equals(type)
                    && n.hasProperty("name") && n.getProperty("name").equals(name)) {

                // found
                return n;

            }
        }
        // not found
        return null;
    }

}
