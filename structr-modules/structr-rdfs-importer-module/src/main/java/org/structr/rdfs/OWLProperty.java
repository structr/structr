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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.core.graph.NodeAttribute;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class OWLProperty<T extends RDFItem> extends RDFItem<T> {

	private static final Map<String, String> replacementMap = new LinkedHashMap<>();

	static {

		replacementMap.put("&lt;", "less");
		replacementMap.put("<;",   "less");
		replacementMap.put("&gt;", "greater");
		replacementMap.put(">",    "greater");
		replacementMap.put("=",    "equal");
		replacementMap.put("€",    "euro");
		replacementMap.put("$",    "dollar");
		replacementMap.put("%",    "percent");
		replacementMap.put("/",    " per ");
		replacementMap.put("ä",    "ae");
		replacementMap.put("Ä",    "Ae");
		replacementMap.put("ö",    "oe");
		replacementMap.put("Ö",    "Oe");
		replacementMap.put("ü",    "ue");
		replacementMap.put("Ü",    "Ue");
		replacementMap.put("ß",    "ss");
		replacementMap.put("?",    "unknown");
		replacementMap.put("µ",    "micro");
		replacementMap.put(".",    "dot");
		replacementMap.put("(",    "_");
		replacementMap.put(")",    "_");
	}

	private final Set<OWLProperty> datatypeProperties = new TreeSet<>();
	private final Set<OWLProperty> objectProperties   = new TreeSet<>();
	private final Set<String> domainIds               = new TreeSet<>();
	private final Set<String> rangeIds                = new TreeSet<>();
	private final Set<String> typeIds                 = new TreeSet<>();
	private boolean allowsInstances                   = false;
	private boolean multipleOccurrences               = false;
	private boolean isMixedIn                         = false;
	private boolean oneWay                            = false;

	public OWLProperty(final Element element) {
		super(element);

		initialize();
	}

	@Override
	public String toString() {
		return getTypeName() + "(" + (typeIds.isEmpty() ? rangeIds : typeIds) + ")";
	}

	public void resolveClassProperties(final Map<URI, OWLClass> classes) {

		// only the domain can be a class
		for (final String domainId : getDomainIds()) {

			final URI uri     = URI.create(domainId);
			final OWLClass cl = classes.get(uri);
			if (cl != null) {

				cl.addProperty(this);

			} else {

				OWLParserv2.logger.println("No class found for " + domainId);
			}
		}
	}

	public Set<String> getDomainIds() {

		if (domainIds.isEmpty() && getSuperclass() != null) {
			return ((OWLProperty)getSuperclass()).getDomainIds();
		}

		return domainIds;
	}

	public Set<String> getRangeIds() {

		if (rangeIds.isEmpty() && getSuperclass() != null) {
			return ((OWLProperty)getSuperclass()).getRangeIds();
		}
		return rangeIds;
	}

	public Set<String> getTypeIds() {
		return typeIds;
	}

	public boolean allowsInstances() {
		return allowsInstances;
	}

	public boolean multipleOccurrences() {
		return multipleOccurrences;
	}

	public boolean isMixedIn() {
		return isMixedIn;
	}

	public boolean oneWay() {
		return oneWay;
	}

	public void addDatatypeProperty(final OWLProperty prop) {
		datatypeProperties.add(prop);
	}

	public void addObjectProperty(final OWLProperty prop) {
		objectProperties.add(prop);
	}

	public Set<OWLProperty> getDatatypeProperties() {
		return datatypeProperties;
	}

	public Set<OWLProperty> getObjectProperties() {
		return objectProperties;
	}

	// ----- methods from OWLProperty -----
	public boolean isPrimitiveType() {
		return !getTypeIds().isEmpty();
	}

	public String getRawPrimitiveType() {

		if (!getTypeIds().isEmpty()) {
			return getTypeIds().iterator().next();
		}

		return null;
	}

	public String getStructrType() {

		final String primitiveType = getRawPrimitiveType();
		if (primitiveType != null) {

			final String mappedXmlType = OWLParserv2.customKTypeMapping.get(primitiveType);
			if (mappedXmlType != null) {

				return OWLParserv2.xmlSchemaPropertyMapping.get(mappedXmlType);

			} else {

				// no mapping, must be "special" type
				switch (primitiveType) {

					case "KBlob":
						return "File";

					case "KChoice":
						return "Enum";
				}
			}
		}

		return null;
	}

	public String getAdditionalFormat() {

		switch (getRawPrimitiveType()) {

			case "KDate":
				return "dd.MM.yyyy";

			case "KDateAndTime":
				return "dd.MM.yyyy HH:mm:ss";

			case "KFlexTime":
				return "dd.MM.yyyy";
		}

		return null;
	}

	public Set<Choice> getChoices() {

		final NodeList hasChoiceElements = getElements(getElement(), "krdf:hasChoice");
		final Set<Choice> choices        = new TreeSet<>();
		final int len                    = hasChoiceElements.getLength();

		for (int i=0; i<len; i++) {

			final Element hasChoice = (Element)hasChoiceElements.item(i);
			final Element choice    = getFirstElement(hasChoice, "krdf:Choice");

			if (choice != null) {

				final Node choiceNameElement = getFirstElement(choice, "krdf:choiceName");
				if (choiceNameElement != null) {

					final String choiceName = getValue(choiceNameElement);
					if (choiceName != null) {

						final Choice choiceObject = new Choice(choiceName, convertValue(choiceName).toString());
						final String orderString  = getValue(getFirstElement(choice, "krdf:order"));

						// parse and set position (called "order")
						if (orderString != null && StringUtils.isNumeric(orderString)) {
							choiceObject.setPosition(Integer.parseInt(orderString));
						}

						choiceObject.setTranslations(getElements(choice, "krdf:choiceTranslation"));

						choices.add(choiceObject);
					}
				}
			}
		}

		return choices;
	}

	public Object convertValue(final Object source) {

		if ("Enum".equals(getStructrType()) && source != null) {

			String value = source.toString();

			for (final Map.Entry<String, String> replacement : replacementMap.entrySet()) {
				value = value.replace(replacement.getKey(), replacement.getValue());
			}

			return "_" + value.replaceAll("[\\W_]+", "_");
		}

		return source;
	}

	public String[] getEnums(final String domain) throws FrameworkException {

		final App app             = StructrApp.getInstance();
		final Set<Choice> choices = getChoices();
		final String[] names      = new String[choices.size()];
		int i                     = 0;

		for (final Choice choice : choices) {

			names[i++] = choice.getIdentifier();

			for (final Map.Entry<String, String> translation : choice.getTranslations().entrySet()) {

				// create localization for enum value
				app.create(Localization.class,

					new NodeAttribute(StructrApp.key(Localization.class, "domain"),        domain),
					new NodeAttribute(StructrApp.key(Localization.class, "name"),          choice.getIdentifier()),
					new NodeAttribute(StructrApp.key(Localization.class, "locale"),        translation.getKey()),
					new NodeAttribute(StructrApp.key(Localization.class, "localizedName"), translation.getValue())
				);
			}
		}

		return names;
	}

	// ----- protected methods -----
	@Override
	protected Set<String> getInheritanceIdentifiers() {

		final Set<String> identifiers = new HashSet<>();

		identifiers.add("rdfs:subPropertyOf");

		return identifiers;
	}


	// ----- protected methods -----
	private void initialize() {

		domainIds.addAll(getResourceIds("rdfs:domain"));
		rangeIds.addAll(getResourceIds("rdfs:range"));
		typeIds.addAll(getResourceIds("krdf:type"));

		allowsInstances     = "true".equals(getValue(getFirstElement(getElement(), "krdf:allowsInstances")));
		multipleOccurrences = "true".equals(getValue(getFirstElement(getElement(), "krdf:multipleOccurrences")));
		isMixedIn           = "true".equals(getValue(getFirstElement(getElement(), "krdf:isMixedIn")));
		oneWay              = "true".equals(getValue(getFirstElement(getElement(), "krdf:oneWay")));
	}
}
