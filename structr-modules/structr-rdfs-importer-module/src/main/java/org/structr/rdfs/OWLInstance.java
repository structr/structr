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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.json.JsonSchema;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class OWLInstance extends RDFItem<OWLInstance> {

	private static final Map<URI, NodeInterface> instances = new LinkedHashMap<>();
	private final SimpleDateFormat isoFormat                = new SimpleDateFormat(ISO8601DateProperty.getDefaultFormat());
	private final SimpleDateFormat dateFormat              = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	protected OWLClass type                                = null;
	protected Class nodeType                               = null;
	protected PropertyKey originIdKey                      = null;
	protected NodeInterface instance                       = null;

	public OWLInstance(final Element element) {
		super(element);
	}

	public void setType(final OWLClass type) {
		this.type = type;
	}

	public OWLClass getType() {
		return type;
	}

	public void createDatabaseNode(final App app) throws FrameworkException {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final String className             = type.getStructrName(true);
		nodeType = config.getNodeEntityClass(className);

		if (nodeType != null) {

			originIdKey = StructrApp.key(nodeType, "originId");
			if (originIdKey != null) {

				instance = app.create(nodeType, new NodeAttribute(originIdKey, getId().toString()));
				if (instance != null) {

					instances.put(getId(), instance);
				}

			} else {

				OWLParserv2.logger.println("NOT creating instance for " + getId() + ", no originId property key found.");
			}

		} else {

			OWLParserv2.logger.println("NOT creating instance for " + getId() + ", no node type found.");
		}
	}

	public void resolveExtensions(final App app, final Map<String, OWLClass> owlClassesByFragment, final Map<URI, OWLInstance> owlInstances, final List<OWLInstance> newInstances) throws FrameworkException {

		final NodeList extensions          = getElements(getElement(), "krdf:KExtendedByRelation");
		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class baseType               = config.getNodeEntityClass("BaseNode");
		final PropertyKey extensionsKey    = StructrApp.key(baseType, "extendedBy");

		if (extensions != null && extensionsKey != null) {

			int len = extensions.getLength();
			for (int i=0; i<len; i++) {

				final Element ext       = (Element)extensions.item(i);
				final NodeList contents = ext.getChildNodes();

				if (contents != null) {

					int contentLen = contents.getLength();
					for (int j=0; j<contentLen; j++) {

						final Node node = contents.item(j);
						if (node instanceof Element) {

							final Element content  = (Element)node;
							final OWLInstance inst = new OWLInstance(content);
							final URI id           = inst.getId();
							if (id != null) {

								final String type = inst.getTypeName();
								if (type != null) {

									final OWLClass owlType = owlClassesByFragment.get(type);
									if (owlType != null) {

										inst.setType(owlType);
										newInstances.add(inst);

										inst.createDatabaseNode(app);

										if (instance != null) {

											// create extension entity and link
											final Set extendedBySet = new HashSet<>((List)instance.getProperty(extensionsKey));
											extendedBySet.add(inst.instance);

											instance.setProperty(extensionsKey, new LinkedList<>(extendedBySet));
										}
									}
								}
							}

						}
					}
				}
			}
		}
	}

	public void resolveProperties() throws FrameworkException {

		if (instance != null && type != null) {

			OWLParserv2.logger.println("#################################################################################################");
			OWLParserv2.logger.println("Resolving properties of " + type.getStructrName(true) + ": " + getId());

			final App app                      = StructrApp.getInstance();
			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class baseNodeType           = config.getNodeEntityClass("BaseNode");
			final Class localizedNameType      = config.getNodeEntityClass("LocalizedName");
			final PropertyKey namesKey         = StructrApp.key(baseNodeType, "names");
			final PropertyKey langKey          = StructrApp.key(localizedNameType, "locale");

			if (localizedNameType != null && namesKey != null) {

				// set name(s)
				final List<NodeInterface> localizedNames = new LinkedList<>();
				for (final Name name : names) {

					localizedNames.add(app.create(localizedNameType, new NodeAttribute<>(AbstractNode.name, name.name), new NodeAttribute<>(langKey, name.lang)));
				}

				instance.setProperty(namesKey, localizedNames);
			}

			// extract creation and modification dates
			try { setProperty(nodeType, instance, "createdAt",  isoFormat.format(dateFormat.parse(createdAt)));  } catch (Throwable ignore) { }
			try { setProperty(nodeType, instance, "modifiedAt", isoFormat.format(dateFormat.parse(modifiedAt))); } catch (Throwable ignore) { }

			for (final OWLProperty property : type.getAllProperties()) {

				final String rawPropertyName   = property.getId().getFragment();
				final String cleanPropertyName = property.getFragmentName(false);

				if (property.multipleOccurrences()) {

					final NodeList values         = getElements(getElement(), rawPropertyName);
					final int len                 = values.getLength();
					final ArrayList<String> array = new ArrayList<>();

					for (int i=0; i<len; i++) {

						final Object value = getValue(values.item(i));
						if (value != null) {

							array.add(value.toString());
						}
					}

					final String[] value = array.toArray(new String[0]);

					OWLParserv2.logger.println(cleanPropertyName + " = " + value);
					setProperty(nodeType, instance, cleanPropertyName, value);

				} else {

					Object value = getValue(getFirstElement(getElement(), rawPropertyName));
					OWLParserv2.logger.println(cleanPropertyName + " = " + value);
					setProperty(nodeType, instance, cleanPropertyName, property.convertValue(value));
				}
			}
		}
	}

	public void resolveRelationships(final JsonSchema schema, final Map<String, OWLClass> owlClassesByFragment, final Map<URI, OWLInstance> instances, final Map<String, RDFDescription> descriptions, final Map<String, OWLProperty> properties) throws FrameworkException {

		if (instance != null && type != null) {

			OWLParserv2.logger.println("#################################################################################################");
			OWLParserv2.logger.println("Resolving relationships of " + type.getStructrName(true) + ": " + getId());

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final NodeList propertyElements    = getElement().getChildNodes();

			if (propertyElements != null) {

				final int len = propertyElements.getLength();
				for (int i=0; i<len; i++) {

					final Node propertyElement = propertyElements.item(i);
					if (propertyElement instanceof Element) {

						final Element element  = (Element)propertyElement;
						final String tagName   = RDFItem.cleanName(element.getTagName());
						final String reference = getAttribute(element, "rdf:resource");

						if (reference != null) {

							final OWLInstance relatedInstance = instances.get(URI.create(reference));
							if (relatedInstance != null) {

								final OWLClass relationshipType = owlClassesByFragment.get(tagName);
								if (relationshipType != null) {

									final List<OWLClass> sourceTypes = relationshipType.getActualSourceTypes();
									final List<OWLClass> targetTypes = relationshipType.getActualTargetTypes();
									final Class hyperRelationshipType = config.getNodeEntityClass(tagName);

									if (hyperRelationshipType != null) {
										if (sourceTypes.size() == 1 && targetTypes.size() == 1) {

											final OWLClass sourceType       = sourceTypes.get(0);
											final OWLClass targetType       = targetTypes.get(0);
											final String sourcePropertyName = sourceType.getStructrName(false);
											final String targetPropertyName = targetType.getStructrName(false);
											final PropertyKey sourceKey     = StructrApp.key(hyperRelationshipType, sourcePropertyName);
											final PropertyKey targetKey     = StructrApp.key(hyperRelationshipType, targetPropertyName);

											if (sourceKey != null && targetKey != null) {

												if (this.instance != null && relatedInstance.instance != null) {

													final NodeInterface hyperNode = StructrApp.getInstance().create(hyperRelationshipType,
														new NodeAttribute(sourceKey, this.instance),
														new NodeAttribute(targetKey, relatedInstance.instance)
													);

													// resolve properties that come via rdf:Description
													final String referenceId = getAttribute(element, "rdf:ID");
													if (referenceId != null && hyperNode != null) {

														final RDFDescription description = descriptions.get(referenceId);
														if (description != null) {

															description.resolveProperties(hyperNode, owlClassesByFragment, properties);
														}
													}

												} else {

													System.out.println("!!!!!!!! No instance found to set on " + getId());
												}

											} else {

												System.out.println("        No property keys found for " + sourcePropertyName + ", " + targetPropertyName);
											}

										} else {

											System.out.println("        Ambiguous source or target types: " + sourceTypes + ", " + targetTypes);
										}

									} else {

										System.out.println("        Relationship type " + tagName + " not found for " + getId());
									}

								} else {

									System.out.println("No type found for " + tagName);
								}

							} else {

								System.out.println("        No instance found for " + reference);
							}
						}
					}
				}
			}
		}
	}

	// ----- protected methods -----

	@Override
	protected Set<String> getInheritanceIdentifiers() {
		return Collections.emptySet();
	}
}
