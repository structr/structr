package org.structr.schema.importer;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.structr.common.CaseHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MaintenanceCommand;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author Christian Morgner
 */
public class RDFImporter extends SchemaImporter implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(RDFImporter.class.getName());

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String fileName = (String)attributes.get("file");
		final String source   = (String)attributes.get("source");
		final String url      = (String)attributes.get("url");

		if (fileName == null && source == null && url == null) {
			throw new FrameworkException(422, "Please supply file, url or source parameter.");
		}

		if (fileName != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		if (fileName != null && url != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		if (url != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		try {

			if (fileName != null) {

				importCypher(importRDF(new FileInputStream(fileName)));

			} else if (url != null) {

				importCypher(importRDF(new URL(url).openStream()));

			} else if (source != null) {

				importCypher(importRDF(new ByteArrayInputStream(source.getBytes())));

			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		analyzeSchema();
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	public static List<String> importRDF(final InputStream is) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final Document doc                            = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		final Set<HasSubclassRelationship> subclasses = new LinkedHashSet<>();
		final Map<String, String> comments            = new LinkedHashMap<>();
		final Map<String, String> properties          = new LinkedHashMap<>();
		final Map<String, String> classes             = new LinkedHashMap<>();
		final Set<Triple> triples                     = new LinkedHashSet<>();
		final StringBuilder cypher                    = new StringBuilder();
		final List<String> cypherStatements           = new LinkedList<>();

		for (Node node = doc.getElementsByTagName("rdfs:Class").item(0); node != null; node = node.getNextSibling()) {

			final String type = node.getNodeName();

			if ("rdfs:Class".equals(type)) {
				handleClass(classes, subclasses, comments, node);
			}

			if ("rdf:Property".equals(type)) {
				handleProperty(properties, triples, comments, node);
			}
		}

		for (final Entry<String, String> entry : classes.entrySet()) {

			final Set<String> superclasses = new LinkedHashSet<>();
			final String id                = entry.getKey();
			final String name              = entry.getValue();

			superclasses.add(name);

			for (final HasSubclassRelationship rel : subclasses) {

				if (rel.child.equals(id)) {
					superclasses.add(classes.get(rel.parent));
				}
			}

			cypher.append("CREATE (").append(id);

			for (final String cl : superclasses) {
				cypher.append(" : ").append(cl.replaceAll("[\\W_]+", ""));
			}

			cypher.append(" { name: '").append(id).append("_").append(name).append("'})\n");
		}

		for (final Triple triple : triples) {

			if (!triple.relationship.endsWith("i") && !triple.target.startsWith("http")) {

				cypher.append("CREATE (").append(triple.source).append("-[:");

				final String relType = properties.get(triple.relationship);
				cypher.append(CaseHelper.toLowerCamelCase(relType).replaceAll("[\\W_]+", ""));

				cypher.append("]->");
				cypher.append(triple.target);
				cypher.append(")\n");
			}
		}

		// we need to put all the statements in a single element because
		// the individual elements are executed separately and there are
		// cross references in the output of this importer.

		cypherStatements.add(cypher.toString());

		return cypherStatements;
	}

	private static void handleClass(final Map<String, String> classes, final Set<HasSubclassRelationship> subclasses, final Map<String, String> comments, final Node classNode) {

		final NamedNodeMap attributes = classNode.getAttributes();
		final String comment          = getChildString(classNode, "rdfs:comment");
		final String rawName          = getString(attributes, "rdf:about");
		final int position            = rawName.indexOf("_");
		final String name             = rawName.substring(position+1);
		final String id               = rawName.substring(0, position);

		classes.put(id, name);

		if (comment != null) {
			comments.put(id, comment);
		}

		for (Node node = classNode.getChildNodes().item(0); node != null; node = node.getNextSibling()) {

			final String type = node.getNodeName();

			if ("rdfs:subClassOf".equals(type)) {

				final String parent = handleSubclass(classes, node);
				subclasses.add(new HasSubclassRelationship(parent, id));
			}
		}
	}

	private static String handleSubclass(final Map<String, String> classes, final Node classNode) {

		final NamedNodeMap attributes = classNode.getAttributes();
		final String rawName          = getString(attributes, "rdf:resource");
		final int position            = rawName.indexOf("_");
		final String name             = rawName.substring(position+1);
		final String id               = rawName.substring(0, position);

		classes.put(id, name);

		return id;
	}

	private static void handleProperty(final Map<String, String> properties, final Set<Triple> triples, final Map<String, String> comments, final Node propertyNode) {

		final NamedNodeMap attributes = propertyNode.getAttributes();
		final String comment          = getChildString(propertyNode, "rdfs:comment");
		final String rawName          = getString(attributes, "rdf:about");
		final int position            = rawName.indexOf("_");
		final String name             = rawName.substring(position+1);
		final String id               = rawName.substring(0, position);

		properties.put(id, name);

		if (comment != null) {
			comments.put(id, comment);
		}

		String domain = null;
		String range  = null;

		for (Node node = propertyNode.getChildNodes().item(0); node != null; node = node.getNextSibling()) {

			final String type = node.getNodeName();

			if ("rdfs:domain".equals(type)) {

				domain = handleDomain(node);
			}

			if ("rdfs:range".equals(type)) {

				range = handleRange(node);
			}
		}

		triples.add(new Triple(domain, id, range));
	}

	private static String handleDomain(final Node propertyNode) {

		final NamedNodeMap attributes = propertyNode.getAttributes();
		final String rawName          = getString(attributes, "rdf:resource");
		final int position            = rawName.indexOf("_");

		if (position >= 0) {

			final String id = rawName.substring(0, position);

			return id;

		} else {

			return rawName;
		}
	}

	private static String handleRange(final Node propertyNode) {

		final NamedNodeMap attributes = propertyNode.getAttributes();
		final String rawName          = getString(attributes, "rdf:resource");
		final int position            = rawName.indexOf("_");

		if (position >= 0) {

			final String id = rawName.substring(0, position);

			return id;

		} else {

			return rawName;
		}
	}

	private static String getString(final NamedNodeMap attributes, final String key) {
		return attributes.getNamedItem(key).getNodeValue();
	}

	private static String getChildString(final Node parent, final String key) {

		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {

			if (key.equals(child.getNodeName())) {

				return child.getTextContent();
			}
		}

		return null;
	}

	private static class HasSubclassRelationship {

		public String parent = null;
		public String child  = null;

		public HasSubclassRelationship(final String parent, final String child) {
			this.parent = parent;
			this.child  = child;
		}

		@Override
		public String toString() {
			return parent + "->" + child;
		}
	}

	private static class Triple {

		public String relationship = null;
		public String source   = null;
		public String target   = null;

		public Triple(final String source, final String property, final String target) {

			this.relationship = property;
			this.source   = source;
			this.target   = target;
		}

		@Override
		public String toString() {
			return source + "-[" + relationship + "]->" + target;
		}
	}
}
