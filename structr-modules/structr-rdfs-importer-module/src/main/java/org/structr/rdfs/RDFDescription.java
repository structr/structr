/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 *
 */
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

						PropertyKey key = config.getPropertyKeyForJSONName(type, keyName, false);
						if (key == null) {

							// property key not found, try inverse
							final OWLClass owlClass = owlClassesByName.get(type.getSimpleName());
							if (owlClass != null) {

								final OWLClass inverse = owlClass.getInverse();
								if (inverse != null) {

									final Class inverseType = config.getNodeEntityClass(inverse.getStructrName(true));
									if (inverseType != null) {

										key = config.getPropertyKeyForJSONName(inverseType, keyName, false);
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
