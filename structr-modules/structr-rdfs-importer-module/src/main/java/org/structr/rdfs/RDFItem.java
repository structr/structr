/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rdfs;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public abstract class RDFItem<T extends RDFItem> implements Comparable<RDFItem> {

	protected final List<Name> names = new LinkedList<>();
	private final Set<T> subclasses  = new TreeSet<>();
	protected String createdAt       = null;
	protected String modifiedAt      = null;
	protected String rdfReferenceId  = null;
	private Element element          = null;
	private T superclass             = null;
	private URI superclassId         = null;
	private URI id                   = null;
	private String internalName      = null;
	private String type              = null;

	protected abstract Set<String> getInheritanceIdentifiers();

	public RDFItem(final Element element) {

		if (element != null) {

			this.type    = element.getTagName();
			this.element = element;

			initialize(element);
		}
	}

	@Override
	public String toString() {
		return getTypeName() + ", " + id + " extends " + superclassId;
	}

	public List<Name> getNames() {
		return names;
	}

	public final String getTypeName() {
		return CaseHelper.toUpperCamelCase(cleanName(type));
	}

	public final String getStructrName(final boolean upperCase) {
		return getFragmentName(upperCase);
	}

	public String getRawFragmentName() {

		if (id != null) {
			return id.getFragment();
		}

		return null;
	}

	public String getFragmentName(final boolean upperCase) {

		if (id != null) {

			final String fragment = id.getFragment();
			if (StringUtils.isNotBlank(fragment)) {

				if (upperCase) {

					return CaseHelper.toUpperCamelCase(cleanName(fragment));

				} else {

					return CaseHelper.toLowerCamelCase(cleanName(fragment));
				}
			}
		}

		return internalName;
	}

	public final URI getSuperclassId() {
		return superclassId;
	}

	public final URI getId() {
		return id;
	}

	public final void setId(final String id) {

		if (id != null) {

			try {
				setId(URI.create(id));

			} catch (Throwable ignore) {}
		}
	}

	public final void setId(final URI id) {
		this.id = id;
	}

	public void resolveSuperclasses(final Map<URI, T> classes) {

		final URI _superclassId = getSuperclassId();
		if (_superclassId != null) {

			final T _superclass = classes.get(_superclassId);
			if (_superclass != null) {

				this.addSuperclass(_superclass);
			}
		}
	}

	public void addSuperclass(final T superclass) {

		superclass.getSubclasses().add(this);
		this.superclass = superclass;
	}

	public T getSuperclass() {
		return superclass;
	}

	public Set<T> getSubclasses() {
		return subclasses;
	}

	public T getRoot() {

		if (superclass != null) {
			return (T)superclass.getRoot();
		}

		return (T)this;
	}

	public Set<T> getTypeAndSuperclasses() {

		final Set<T> superclasses = new LinkedHashSet<>();

		superclasses.add((T)this);

		if (getSuperclass() != null) {
			superclasses.addAll(superclass.getTypeAndSuperclasses());
		}

		return superclasses;
	}

	// ----- interface Comparable<RDFItem> -----
	@Override
	public int compareTo(final RDFItem o) {

		if (id != null && o != null && o.id != null) {
			return id.compareTo(o.id);
		}

		return -1;
	}

	// ----- protected methods -----
	protected final Element getElement() {
		return element;
	}

	protected final String getResourceId(final Node node) {
		return getAttribute(node, "rdf:resource");
	}

	protected final String getAttribute(final Node node, final String key) {

		final NamedNodeMap map = node.getAttributes();
		final Node attrNode    = map.getNamedItem(key);

		if (attrNode != null) {

			return attrNode.getNodeValue();
		}

		return null;
	}

	protected final String getValue(final Node element) {

		if (element != null) {

			final Node firstChild = element.getFirstChild();
			if (firstChild != null) {

				return firstChild.getNodeValue();
			}
		}

		return null;
	}

	protected Element getFirstElement(final Element element, final String key) {
		return (Element)element.getElementsByTagName(key).item(0);
	}

	protected NodeList getElements(final Element element, final String key) {
		return element.getElementsByTagName(key);
	}

	protected Set<String> getResourceIds(final String key) {

		final NodeList ranges = element.getElementsByTagName(key);
		final Set<String> ids = new TreeSet<>();
		if (ranges != null) {

			final int len = ranges.getLength();
			for (int i=0; i<len; i++) {

				final Node item     = ranges.item(i);
				final String itemId = getResourceId(item);

				if (itemId != null) {

					ids.add(itemId);
				}
			}
		}

		return ids;
	}

	protected Object setProperty(final Class nodeType, final GraphObject instance, final String propertyName, final Object value) throws FrameworkException {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		Object convertedValue              = value;

		if (convertedValue != null) {

			final PropertyKey key = StructrApp.key(nodeType, propertyName);
			if (key != null) {

				final PropertyConverter inputConverter = key.inputConverter(SecurityContext.getSuperUserInstance());
				if (inputConverter != null) {

					convertedValue = inputConverter.convert(convertedValue);
				}

				if (convertedValue == null) {
					OWLParserv2.logger.println("Invalid converted value " + convertedValue + ", source was " + value);
				}

				return instance.setProperty(key, convertedValue);

			} else {

				System.out.println("Key " + propertyName + " not found on " + nodeType.getSimpleName());
			}
		}

		return null;
	}

	// ----- private methods -----
	private void initialize(final Element element) {

		String _id = getAttribute(element, "rdf:about");
		if (_id != null) {

			setId(_id);
		}

		initializeRDFSLabels(element.getElementsByTagName("rdfs:label"));

		internalName = getValue(getFirstElement(getElement(), "krdf:internalName"));

		// detect inheritance
		for (final String inheritanceIdentifier : getInheritanceIdentifiers()) {

			initializeSuperclass(element.getElementsByTagName(inheritanceIdentifier));

			if (superclassId != null) {
				break;
			}
		}

		// extract and parse creation and modification dates
		createdAt  = getValue(getFirstElement(getElement(), "createdAt"));
		modifiedAt = getValue(getFirstElement(getElement(), "modifiedAt"));

		rdfReferenceId = getAttribute(getElement(), "rdf:ID");

	}

	private void initializeSuperclass(final NodeList nodeList) {

		if (nodeList != null) {

			final int len = nodeList.getLength();
			for (int i=0; i<len; i++) {

				final String superclass = getResourceId(nodeList.item(i));
				if (superclass != null) {

					this.superclassId = URI.create(superclass);
				}
			}
		}
	}

	private void initializeRDFSLabels(final NodeList elements) {

		if (elements != null) {

			final int len = elements.getLength();
			for (int i=0; i<len; i++) {

				final Node node    = elements.item(i);
				final String value = getValue(node);
				String lang        = getAttribute(node, "xml:lang");

				if (lang == null) {
					lang = "de";
				}

				if (lang != null && value != null) {

					names.add(new Name(lang, value));
				}
			}
		}
	}

	public static String cleanName(final String source) {

		final StringBuilder buf = new StringBuilder();
		final String[] parts    = source.split("[ ,\\.\\-\\+\\?\\*\\\"\\'\\=\\(\\)\\[\\]<>]+");
		final int len           = parts.length;

		for (int i=0; i<len; i++) {

			String part = parts[i].trim();
			if (StringUtils.isNotBlank(part)) {

				part = part.replace("ä", "ae");
				part = part.replace("ö", "oe");
				part = part.replace("ü", "ue");
				part = part.replace("Ä", "Ae");
				part = part.replace("Ö", "Oe");
				part = part.replace("Ü", "Ue");
				part = part.replace("ß", "ss");

				buf.append(StringUtils.capitalize(part));
			}
		}

		return buf.toString().replaceAll("[\\W]+", "");
	}
}
