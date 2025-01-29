/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.importer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.function.XmlFunction;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 *
 */
public class RDFImporter extends SchemaAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(RDFImporter.class.getName());

	private static String separator = null;

	private final static String RDF_IMPORT_STATUS   = "RDF_IMPORT_STATUS";

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String fileName = (String)attributes.get("file");
		final String source   = (String)attributes.get("source");
		final String url      = (String)attributes.get("url");
		separator             = (String)attributes.get("separator");

		if (StringUtils.isBlank(separator)) {
			separator = "#";
		}

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

				try (final FileInputStream fis = new FileInputStream(fileName)) {

					importCypher(importRDF(fis));
				}

			} else if (url != null) {

				try (final InputStream is = new URL(url).openStream()) {

					importCypher(importRDF(is));
				}

			} else if (source != null) {

				try (final InputStream bis = new ByteArrayInputStream(source.getBytes())) {

					importCypher(importRDF(bis));
				}
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}

		analyzeSchema(RDF_IMPORT_STATUS);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	public List<String> importRDF(final InputStream is) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final Document doc                            = XmlFunction.getDocumentBuilder().parse(is);
		final Set<HasSubclassRelationship> subclasses = new LinkedHashSet<>();
		final Map<String, RdfProperty> properties     = new LinkedHashMap<>();
		final Map<String, RdfClass> classes           = new LinkedHashMap<>();
		final Set<Triple> triples                     = new LinkedHashSet<>();
		final StringBuilder cypher                    = new StringBuilder();
		final List<String> cypherStatements           = new LinkedList<>();

		for (Node node = doc.getElementsByTagName("rdfs:Class").item(0); node != null; node = node.getNextSibling()) {

			final String type = node.getNodeName();

			if ("rdfs:Class".equals(type)) {
				handleClass(classes, subclasses, node);
			}

			if ("rdf:Property".equals(type)) {
				handleProperty(properties, triples, node);
			}
		}

		for (final Entry<String, RdfClass> entry : classes.entrySet()) {

			final Set<String> superclasses = new LinkedHashSet<>();
			final String id                = entry.getKey();
			final RdfClass rdfClass        = entry.getValue();

			superclasses.add(rdfClass.name);

			for (final HasSubclassRelationship rel : subclasses) {

				if (rel.child.equals(id)) {
					superclasses.add(classes.get(rel.parent).id);
				}
			}

			cypher.append("CREATE (").append(rdfClass.id.replaceAll("[\\W_]+", ""));

			for (final String cl : superclasses) {
				cypher.append(":").append(rdfClass.type);
			}

			cypher.append(" { ");

			cypher.append("name: '").append(rdfClass.name).append("' ");
			if (StringUtils.isNotBlank(rdfClass.comment)) { cypher.append(", comment: '").append(rdfClass.comment).append("' "); }

			cypher.append("})\n");
		}

		for (final Triple triple : triples) {

			if (!triple.relationship.endsWith("i") && !triple.target.startsWith("http")) {

				cypher.append("CREATE (").append(triple.source).append("-[:");

				final String relType = properties.get(triple.relationship).id;
				cypher.append(CaseHelper.toLowerCamelCase(relType).replaceAll("[\\W_]+", ""));

				cypher.append("]->");
				cypher.append(triple.target);
				cypher.append(")\n");
			}
		}

		// we need to put all the statements in a single element because
		// the individual elements are executed separately and there are
		// cross references in the output of this importer.

		logger.info(cypher.toString());

		cypherStatements.add(cypher.toString());

		return cypherStatements;
	}

	private static void handleClass(final Map<String, RdfClass> classes, final Set<HasSubclassRelationship> subclasses, final Node classNode) {

		final NamedNodeMap attributes = classNode.getAttributes();
		final String comment          = getChildString(classNode, "rdfs:comment");
		final String rawName          = getString(attributes, "rdf:ID") != null ? getString(attributes, "rdf:ID") : getString(attributes, "rdf:about");

		classes.put(rawName, new RdfClass(rawName, comment));

		for (Node node = classNode.getChildNodes().item(0); node != null; node = node.getNextSibling()) {

			final String type = node.getNodeName();

			if ("rdfs:subClassOf".equals(type)) {

				final String parent = handleSubclass(classes, node);
				subclasses.add(new HasSubclassRelationship(parent, rawName));
			}
		}
	}

	private static String handleSubclass(final Map<String, RdfClass> classes, final Node classNode) {

		final NamedNodeMap attributes = classNode.getAttributes();
		final String comment          = getChildString(classNode, "rdfs:comment");
		final String rawName          = getString(attributes, "rdf:resource");

		classes.put(rawName, new RdfClass(rawName, comment));

		return rawName;
	}

	private static void handleProperty(final Map<String, RdfProperty> properties, final Set<Triple> triples, final Node propertyNode) {

		final NamedNodeMap attributes = propertyNode.getAttributes();
		final String comment          = getChildString(propertyNode, "rdfs:comment");
		final String rawName          = getString(attributes, "rdf:ID") != null ? getString(attributes, "rdf:ID") : getString(attributes, "rdf:about");

		properties.put(rawName, new RdfProperty(rawName, comment));

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

		triples.add(new Triple(domain, rawName, range));
	}

	private static String handleDomain(final Node propertyNode) {

		final NamedNodeMap attributes = propertyNode.getAttributes();
		final String rawName          = getString(attributes, "rdf:resource");
		final int position            = rawName.lastIndexOf(separator);

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
		final int position            = rawName.lastIndexOf(separator);

		if (position >= 0) {

			final String id = rawName.substring(0, position);

			return id;

		} else {

			return rawName;
		}
	}

	private static String getString(final NamedNodeMap attributes, final String key) {
		final Node node = attributes.getNamedItem(key);
		return node != null ? attributes.getNamedItem(key).getNodeValue() : null;
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

	private static class RdfClass {

		public String comment = null;
		public String name    = null;
		public String id      = null;
		public String type    = null;

		public RdfClass(final String rawAttribueValue, final String comment) {

			final int position = rawAttribueValue.lastIndexOf(separator);

			this.name    = rawAttribueValue;
			this.id      = rawAttribueValue.replaceAll("[\\W_]+", "");
			this.type    = rawAttribueValue.substring(position+1).replaceAll("[\\W_]+", "");
			this.comment = comment;

		}
	}

	private static class RdfProperty {

		public String comment = null;
		public String name    = null;
		public String id      = null;
		public String type    = null;

		public RdfProperty(final String rawAttribueValue, final String comment) {

			final int position = rawAttribueValue.lastIndexOf(separator);

			this.name    = rawAttribueValue;
			this.id      = rawAttribueValue.replaceAll("[\\W_]+", "");
			this.type    = rawAttribueValue.substring(position+1).replaceAll("[\\W_]+", "");
			this.comment = comment;

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
