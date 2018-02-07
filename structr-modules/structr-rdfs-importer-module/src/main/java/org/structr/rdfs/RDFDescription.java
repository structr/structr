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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class RDFDescription extends RDFItem<RDFDescription> {

	private String referenceId = null;

	public RDFDescription(final Element element) {

		super(element);

		initialize();
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void resolveProperties(final NodeInterface nodeInterface, final Map<String, OWLClass> owlClassesByName, final Map<String, OWLProperty> propertiesByName) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final NodeList children            = getElement().getChildNodes();
		final int length                   = children.getLength();

		for (int i=0; i<length; i++) {

			final Node node = children.item(i);
			if (node instanceof Element) {

				final Element childElement = (Element)node;
				final String tagName       = childElement.getTagName();

				final OWLProperty prop = propertiesByName.get(tagName);
				if (prop != null) {

					final Class type     = nodeInterface.getClass();
					final String keyName = prop.getStructrName(false);
					Object value         = getValue(childElement);

					try {

						PropertyKey key = StructrApp.key(type, keyName);
						if (key == null) {

							// property key not found, try inverse
							final OWLClass owlClass = owlClassesByName.get(type.getSimpleName());
							if (owlClass != null) {

								final OWLClass inverse = owlClass.getInverse();
								if (inverse != null) {

									final Class inverseType = config.getNodeEntityClass(inverse.getStructrName(true));
									if (inverseType != null) {

										key = StructrApp.key(inverseType, keyName);
									}
								}
							}
						}

						if (key != null) {

							final PropertyConverter converter = key.inputConverter(SecurityContext.getSuperUserInstance());
							if (converter != null) {

								value = converter.convert(value);
							}

							nodeInterface.setProperty(key, value);

						} else {

							System.out.println("Description: no property key found for " + keyName + " of " + type.getSimpleName());
						}

					} catch (Throwable t) {
						t.printStackTrace();
					}

				} else {

					System.out.println("Property for " + tagName + " not found!");
				}
			}
		}

	}

	@Override
	protected Set<String> getInheritanceIdentifiers() {
		return Collections.emptySet();
	}

	private void initialize() {
		referenceId = getAttribute(getElement(), "rdf:ID");
	}
}
