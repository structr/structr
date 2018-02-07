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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonDateProperty;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class OWLParserv2 {

	private static final Set<String> unwantedPrefixes                   = new LinkedHashSet<>();

	public static final Map<String, String> xmlSchemaPropertyMapping     = new TreeMap<>();
	public static final Map<String, String> customKTypeMapping           = new TreeMap<>();
	public static final Set<String> blacklistedProperties                = new LinkedHashSet<>();

	public static final String XML_SCHEMA_TYPE_URI       = "http://www.w3.org/2001/XMLSchema#anyURI";
	public static final String XML_SCHEMA_TYPE_BOOLEAN   = "http://www.w3.org/2001/XMLSchema#boolean";
	public static final String XML_SCHEMA_TYPE_DATE      = "http://www.w3.org/2001/XMLSchema#date";
	public static final String XML_SCHEMA_TYPE_DATE_TIME = "http://www.w3.org/2001/XMLSchema#dateTime";
	public static final String XML_SCHEMA_TYPE_DOUBLE    = "http://www.w3.org/2001/XMLSchema#double";
	public static final String XML_SCHEMA_TYPE_INTEGER   = "http://www.w3.org/2001/XMLSchema#integer";
	public static final String XML_SCHEMA_TYPE_STRING    = "http://www.w3.org/2001/XMLSchema#string";

	public static PrintWriter logger                     = null;

	static {

		unwantedPrefixes.add("http://www.intelligent-views.de");
		unwantedPrefixes.add("box");
		unwantedPrefixes.add("Box");
		unwantedPrefixes.add("collect");
		unwantedPrefixes.add("context");
		unwantedPrefixes.add("eventLog");
		unwantedPrefixes.add("expertSearch");
		unwantedPrefixes.add("History");
		unwantedPrefixes.add("icons");
		unwantedPrefixes.add("intern");
		unwantedPrefixes.add("kp");
		unwantedPrefixes.add("kScript");
		unwantedPrefixes.add("kservice");
		unwantedPrefixes.add("onlineHelp");
		unwantedPrefixes.add("print");
		unwantedPrefixes.add("quick");
		unwantedPrefixes.add("render");
		unwantedPrefixes.add("rest");
		unwantedPrefixes.add("P-inverse");
		unwantedPrefixes.add("PfadSuche");
		unwantedPrefixes.add("viewConfig");
		unwantedPrefixes.add("viewconfig");
		unwantedPrefixes.add("welcome");

		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_URI,       "String");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_BOOLEAN,   "Boolean");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_DATE,      "Date");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_DATE_TIME, "Date");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_DOUBLE,    "Double");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_INTEGER,   "Integer");
		xmlSchemaPropertyMapping.put(XML_SCHEMA_TYPE_STRING,    "String");

		customKTypeMapping.put("KAttribute",          XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KBoolean",            XML_SCHEMA_TYPE_BOOLEAN);
		customKTypeMapping.put("KBlob",               XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KColorValue",         XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KContainerAttribute", XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KDate",               XML_SCHEMA_TYPE_DATE);
		customKTypeMapping.put("KDateAndTime",        XML_SCHEMA_TYPE_DATE_TIME);
		customKTypeMapping.put("KDocumentAttribute",  XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KFlexTime",           XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KFloat",              XML_SCHEMA_TYPE_DOUBLE);
		customKTypeMapping.put("KInteger",            XML_SCHEMA_TYPE_INTEGER);
		customKTypeMapping.put("KInterval",           XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KStringAttribute",    XML_SCHEMA_TYPE_STRING);
		customKTypeMapping.put("KURLAttribute",       XML_SCHEMA_TYPE_URI);

		// commented this out so that the import-<date> file will not be created every time structr starts with this module
//		try {
//
//			logger = new PrintWriter(new FileOutputStream("import-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(System.currentTimeMillis()) + ".log", false));
//
//		} catch (FileNotFoundException fnfex) {
//			fnfex.printStackTrace();
//		}

	}

	private final Map<String, OWLProperty> owlPropertiesByName = new TreeMap<>();
	private final Map<URI, OWLProperty> owlPropertiesByURI     = new TreeMap<>();
	private final Map<String, RDFDescription> rdfDescriptions  = new TreeMap<>();
	private final Map<String, OWLClass> owlClassesByFragment   = new TreeMap<>();
	private final Map<URI, OWLInstance> owlInstances           = new TreeMap<>();
	private final Map<URI, OWLClass> owlClassesByURI           = new TreeMap<>();

	private final boolean importSchema                         = true;
	private final boolean importData                           = true;
	private final boolean importFiles                          = false;
	private final boolean createFileRelationships              = false;

	public static void main(final String[] args) {

		String fileName = null;
		String blobsDir = null;

		if (args.length == 2) {

			fileName = args[0];
			blobsDir = args[1];

			System.out.println("Using file " + fileName + ", data directory " + blobsDir);

		} else {

			System.out.println("Please supply an import file name.");
			System.out.println("Usage: java -jar OWLParser.jar [fileName] [data dir]");

			System.exit(1);
		}

		OWLParserv2 parser = new OWLParserv2();
		parser.parse(fileName, blobsDir);
	}


	public void parse(final String fileName, final String blobsDirectory) {

		boolean success = true;

		try (final App app = StructrApp.getInstance()) {

			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.File(fileName));

			System.out.println("Parsing XML document..");
			logger.println("Parsing XML document..");

			// parse XML document
			parseDocument(doc.getDocumentElement(), 0);

			System.out.println("Filtering unwanted classes..");
			logger.println("Filtering unwanted classes..");

			// filter unwanted objects by their IDs
			filter(owlClassesByURI.values());
			filter(owlPropertiesByURI.values());

			if (importSchema) {

				// initialize class hierarchies
				System.out.println("Resolving " + owlClassesByURI.size() + " OWL superclasses..");
				logger.println("Resolving " + owlClassesByURI.size() + " OWL superclasses..");

				for (final OWLClass owlClass : owlClassesByURI.values()) {
					owlClass.resolveSuperclasses(owlClassesByURI);
				}

				for (final OWLClass owlClass : owlClassesByURI.values()) {
					owlClass.resolveRelatedTypes(owlClassesByURI);
				}

				for (final OWLClass owlClass : owlClassesByURI.values()) {
					owlClass.resolveRelationshipTypes(owlClassesByURI);
				}

				// initialize classes with datatype properties
				System.out.println("Resolving " + owlPropertiesByURI.size() + " datatype properties..");
				logger.println("Resolving " + owlPropertiesByURI.size() + " datatype properties..");

				for (final OWLProperty owlProperty : owlPropertiesByURI.values()) {

					owlProperty.resolveSuperclasses(owlPropertiesByURI);
					owlProperty.resolveClassProperties(owlClassesByURI);
				}


				final JsonSchema schema = StructrSchema.newInstance(URI.create("http://localhost/test/#"));

				// create common base class
				final JsonType baseType = schema.addType("BaseNode");
				final JsonType nameType = schema.addType("LocalizedName");

				nameType.addStringProperty("locale").setIndexed(true);
				nameType.addStringProperty("name").setIndexed(true);

				baseType.addStringProperty("originId").setIndexed(true);
				baseType.addDateProperty("createdAt").setIndexed(true);
				baseType.addDateProperty("modifiedAt").setIndexed(true);
				baseType.addFunctionProperty("isFallbackLang", "ui").setContentType("application/x-structr-script").setReadFunction("(empty(get_or_null(first(filter(this.names, equal(data.locale, substring(locale, 0, 2)))), 'name')))").setIndexed(true);
				baseType.addFunctionProperty("localizedName", "ui").setContentType("application/x-structr-script").setReadFunction("(if (equal('zh', substring(locale, 0, 2)),(if (empty(first(filter(this.names, equal(data.locale, 'zh')))),if (empty(first(filter(this.names, equal(data.locale, 'en')))),get_or_null(first(filter(this.names, equal(data.locale, 'de'))), 'name'),get(first(filter(this.names, equal(data.locale, 'en'))), 'name')),get(first(filter(this.names, equal(data.locale, 'zh'))), 'name'))),if (equal('de', substring(locale, 0, 2)),(if (empty(first(filter(this.names, equal(data.locale, 'de')))),if (empty(first(filter(this.names, equal(data.locale, 'en')))),get_or_null(first(filter(this.names, equal(data.locale, 'zh'))), 'name'),get(first(filter(this.names, equal(data.locale, 'en'))), 'name')),get(first(filter(this.names, equal(data.locale, 'de'))), 'name'))),(if (empty(first(filter(this.names, equal(data.locale, 'en')))),if (empty(first(filter(this.names, equal(data.locale, 'de')))),get_or_null(first(filter(this.names, equal(data.locale, 'zh'))), 'name'),get(first(filter(this.names, equal(data.locale, 'de'))), 'name')),get(first(filter(this.names, equal(data.locale, 'en'))), 'name'))))))").setIndexed(true);
				baseType.addFunctionProperty("nameDE", "ui").setContentType("application/x-structr-script").setReadFunction("get_or_null(first(filter(this.names, equal(data.locale, 'de'))), 'name')").setWriteFunction("(store('node', first(filter(this.names, equal(data.locale, 'de')))),if (empty(retrieve('node')),set(this, 'names', merge(this.names, create('LocalizedName', 'locale', 'de', 'name', value))),(if (empty(value),delete(retrieve('node')),set(retrieve('node'), 'name', value)))))").setIndexed(true);
				baseType.addFunctionProperty("nameEN", "ui").setContentType("application/x-structr-script").setReadFunction("get_or_null(first(filter(this.names, equal(data.locale, 'en'))), 'name')").setWriteFunction("(store('node', first(filter(this.names, equal(data.locale, 'en')))),if (empty(retrieve('node')),set(this, 'names', merge(this.names, create('LocalizedName', 'locale', 'en', 'name', value))),(if (empty(value),delete(retrieve('node')),set(retrieve('node'), 'name', value)))))").setIndexed(true);
				baseType.addFunctionProperty("nameZH", "ui").setContentType("application/x-structr-script").setReadFunction("get_or_null(first(filter(this.names, equal(data.locale, 'zh'))), 'name')").setWriteFunction("(store('node', first(filter(this.names, equal(data.locale, 'zh')))),if (empty(retrieve('node')),set(this, 'names', merge(this.names, create('LocalizedName', 'locale', 'zh', 'name', value))),(if (empty(value),delete(retrieve('node')),set(retrieve('node'), 'name', value)))))").setIndexed(true);

				final JsonReferenceType names = ((JsonObjectType)baseType).relate((JsonObjectType)nameType, "HasName", Cardinality.OneToMany);
				names.setSourcePropertyName("isNameOf");
				names.setTargetPropertyName("names");

				final JsonReferenceType extensions = ((JsonObjectType)baseType).relate((JsonObjectType)baseType, "ExtendedBy", Cardinality.ManyToMany);
				extensions.setSourcePropertyName("extends");
				extensions.setTargetPropertyName("extendedBy");

				baseType.addStringProperty("name").setIndexed(true);

				System.out.println("Creating schema..");
				logger.println("Creating schema..");


				try (final Tx tx = StructrApp.getInstance().tx()) {

					for (final OWLClass owlClass : owlClassesByURI.values()) {

						final String name = owlClass.getStructrName(true);
						if (name != null && schema.getType(name) == null && owlClass.isPrimary()) {

							logger.println("Creating type " + name + "..");
							schema.addType(name);
						}
					}

					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();

				} catch (FrameworkException fex) {
					System.out.println(fex.getErrorBuffer().getErrorTokens());
				}

				// resolve inheritance
				System.out.println("Resolving class inheritance..");
				logger.println("Resolving class inheritance..");

				try (final Tx tx = StructrApp.getInstance().tx()) {

					for (final OWLClass owlClass : owlClassesByURI.values()) {

						final String name         = owlClass.getStructrName(true);
						final JsonType type       = schema.getType(name);
						final OWLClass superclass = owlClass.getSuperclass();

						// type can be null if it is inverseOf another type
						if (type != null) {

							if (superclass != null) {

								final JsonType superType = schema.getType(superclass.getStructrName(true));
								if (superType != null) {

									type.setExtends(superType);

								} else {

									type.setExtends(baseType);
								}

							} else {

								type.setExtends(baseType);
							}

							for (final Name localizedName : owlClass.getNames()) {

								app.create(Localization.class,
									new NodeAttribute(StructrApp.key(Localization.class, "name"), name),
									new NodeAttribute(StructrApp.key(Localization.class, "localizedName"), localizedName.name),
									new NodeAttribute(StructrApp.key(Localization.class, "locale"), localizedName.lang)
								);
							}
						}
					}

					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();

				} catch (FrameworkException fex) {
					System.out.println(fex.getErrorBuffer().getErrorTokens());
				}

				// resolve relationship types
				System.out.println("Resolving relationship types..");
				logger.println("Resolving relationship types..");

				try (final Tx tx = StructrApp.getInstance().tx()) {

					for (final OWLClass possibleOutgoingRelationshipType : owlClassesByURI.values()) {

						final OWLClass possibleIncomingRelationshipType = possibleOutgoingRelationshipType.getInverse();
						if (possibleOutgoingRelationshipType.isPrimary() && possibleIncomingRelationshipType != null) {

							// this is a relationship
							final List<OWLClass> sourceTypes = possibleOutgoingRelationshipType.getActualSourceTypes();
							final List<OWLClass> targetTypes = possibleOutgoingRelationshipType.getActualTargetTypes();

							for (final OWLClass sourceType : sourceTypes) {

								for (final OWLClass targetType : targetTypes) {

									final String sourceName     = possibleOutgoingRelationshipType.getStructrName(false);
									final String targetName     = possibleIncomingRelationshipType.getStructrName(false);
									final String sourceTypeName = sourceType.getStructrName(true);
									final String targetTypeName = targetType.getStructrName(true);

									final JsonType sourceJsonType = schema.getType(sourceTypeName);
									final JsonType targetJsonType = schema.getType(targetTypeName);

									if (sourceJsonType != null && targetJsonType != null) {

										final String relationshipTypeName = possibleOutgoingRelationshipType.getStructrName(true);
										final JsonObjectType relType      = schema.addType(relationshipTypeName);
										final JsonObjectType srcType      = (JsonObjectType)sourceJsonType;
										final JsonObjectType tgtType      = (JsonObjectType)targetJsonType;

										srcType.relate(relType, sourceName, Cardinality.OneToMany, sourceType.getStructrName(false), sourceName);
										relType.relate(tgtType, targetName, Cardinality.ManyToOne, targetName, targetType.getStructrName(false));

										possibleOutgoingRelationshipType.setIsRelationship(true);
									}
								}
							}
						}
					}

					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();

				} catch (FrameworkException fex) {

					System.out.println(fex.getErrorBuffer().getErrorTokens());
				}

				System.out.println("Adding properties to types");
				logger.println("Adding properties to types");

				try (final Tx tx = StructrApp.getInstance().tx()) {

					for (final OWLClass owlClass : owlClassesByURI.values()) {

						final String typeName = owlClass.getStructrName(true);
						JsonType type         = schema.getType(typeName);

						// type not found, try to set property on inverse type
						if (type == null) {

							final OWLClass inverse = owlClass.getInverse();
							if (inverse != null) {

								type = schema.getType(inverse.getStructrName(true));
							}
						}

						if (type != null) {

							for (final OWLProperty prop : owlClass.getAllProperties()) {

								addProperty(type, prop, prop.getStructrName(false));
							}

						} else {

							System.out.println("Class: no type found for " + owlClass.getId());
						}
					}

					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();
				}

				System.out.println("Adding metdata to node types");
				logger.println("Adding metdata to node types");

				try (final Tx tx = StructrApp.getInstance().tx()) {

					for (final OWLClass owlClass : owlClassesByURI.values()) {

						final String name           = owlClass.getStructrName(true);
						final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(name).getFirst();
						String icon                 = owlClass.getIcon();

						if (schemaNode != null) {

							// truncate icon name, use only the
							// part after the second dash
							if (icon != null && icon.contains("-")) {

								// start with
								final int pos = icon.indexOf("-", 7);
								if (pos > -1) {

									icon = icon.substring(pos + 1);
								}
							}

							schemaNode.setProperty(SchemaNode.icon, icon);
						}
					}

					tx.success();

				} catch (FrameworkException fex) {
					System.out.println(fex.getErrorBuffer().getErrorTokens());
				}

				// create instances
				System.out.println("Resolving instances..");
				logger.println("Resolving instances..");

				final Iterator<OWLInstance> instancesIterator = owlInstances.values().iterator();
				final List<OWLInstance> newInstances          = new LinkedList<>();
				int count                                      = 0;

				while (instancesIterator.hasNext()) {

					try (final Tx tx = StructrApp.getInstance().tx()) {

						while (instancesIterator.hasNext()) {

							final OWLInstance instance = instancesIterator.next();
							final OWLClass owlType     = instance.getType();

							if (owlType != null) {

								instance.createDatabaseNode(app);
								instance.resolveProperties();
								instance.resolveExtensions(app, owlClassesByFragment, owlInstances, newInstances);
							}

							if (++count == 100) {

								count = 0;
								break;
							}
						}

						tx.success();
					}
				}

				// add newly created extension instances to global map
				for (final OWLInstance newInstance : newInstances) {
					owlInstances.put(newInstance.getId(), newInstance);
				}


				System.out.println("Resolving instance relationships..");
				logger.println("Resolving instance relationships..");

				final Iterator<OWLInstance> relationshipsIterator = owlInstances.values().iterator();
				count = 0;

				while (relationshipsIterator.hasNext()) {

					try (final Tx tx = StructrApp.getInstance().tx()) {

						while (relationshipsIterator.hasNext()) {

							final OWLInstance instance = relationshipsIterator.next();
							final OWLClass owlType     = instance.getType();

							if (owlType != null) {

								instance.resolveRelationships(schema, owlClassesByFragment, owlInstances, rdfDescriptions, owlPropertiesByName);
							}

							if (++count == 100) {

								count = 0;
								break;
							}
						}

						tx.success();
					}
				}
			}

			final java.io.File blobs = new java.io.File(blobsDirectory);
			if (blobs.exists()) {

				final ConfigurationProvider config            = StructrApp.getConfiguration();
				final List<Tuple<Class, PropertyKey>> mapping = createPropertyKeyMapping(config);

				final Set<Path> files = new LinkedHashSet<>();
				int count             = 0;

				// collect all files
				Files.walkFileTree(blobs.toPath(), new Visitor(files));

				if (createFileRelationships) {

					System.out.println("Resolving file relationships..");
					logger.println("Resolving file relationships..");

					// iterate over files to identify relationships and extend schema
					final Iterator<Path> pathIteratorForSchemaExtension = files.iterator();
					try (final Tx tx = StructrApp.getInstance().tx()) {

						while (pathIteratorForSchemaExtension.hasNext()) {

							final Path file     = pathIteratorForSchemaExtension.next();
							final String name   = file.getFileName().toString();
							final int pos       = name.indexOf(".", 7);
							final String idPart = name.substring(6, pos == -1 ? name.length() : pos);

							if (name.startsWith("KBlob-") && name.length() > 23) {

								for (final Tuple<Class, PropertyKey> entry : mapping) {

									final Class type                 = entry.getKey();
									final PropertyKey key            = entry.getValue();
									Object value                     = idPart;

									if (key instanceof ArrayProperty) {
										value = new String[] { idPart };
									}

									final Query<NodeInterface> query = app.nodeQuery().andType(type).and(key, value, false);
									final List<NodeInterface> nodes  = query.getAsList();

									if (nodes.size() == 1) {

										System.out.println("                ##########: " + nodes.size() + " results..");

										// create schema relationship from schema type to file (once)
										// import file
										// link file
										final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(type.getSimpleName()).getFirst();
										if (schemaNode != null) {

											System.out.println("                ##########: found SchemaNode " + schemaNode.getUuid() + " (" + schemaNode.getName() + ")");

											final SchemaNode fileSchemaNode = app.nodeQuery(SchemaNode.class).andName(File.class.getSimpleName()).getFirst();
											if (fileSchemaNode != null) {

												final String capitalJsonName = StringUtils.capitalize(key.jsonName());
												final String targetJsonName  = "has" + capitalJsonName;
												final String sourceJsonName  = "is" + capitalJsonName + "Of" + type.getSimpleName();

												final SchemaRelationshipNode link = app.nodeQuery(SchemaRelationshipNode.class)
													.and(SchemaRelationshipNode.sourceNode, schemaNode)
													.and(SchemaRelationshipNode.targetNode, fileSchemaNode)
													.and(SchemaRelationshipNode.relationshipType, key.jsonName())
													.getFirst();

												if (link == null) {

													System.out.println("Creating link from " + schemaNode + " to " + fileSchemaNode + ", " + sourceJsonName + ", " + targetJsonName);

													app.create(SchemaRelationshipNode.class,
														new NodeAttribute(SchemaRelationshipNode.sourceNode, schemaNode),
														new NodeAttribute(SchemaRelationshipNode.targetNode, fileSchemaNode),
														new NodeAttribute(SchemaRelationshipNode.relationshipType, key.jsonName()),
														new NodeAttribute(SchemaRelationshipNode.sourceMultiplicity, "1"),
														new NodeAttribute(SchemaRelationshipNode.targetMultiplicity, key instanceof ArrayProperty ? "*" : "1"),
														new NodeAttribute(SchemaRelationshipNode.sourceJsonName, sourceJsonName),
														new NodeAttribute(SchemaRelationshipNode.targetJsonName, targetJsonName)
													);

												} else {

													System.out.println("Link relationship already exists: " + link);
												}

											} else {

												System.out.println("NO SchemaNode found for type File!");
											}

										} else {

											System.out.println("NO SchemaNode found for type " + type.getSimpleName() + "!");
										}

										// no need to search further
										//break;
									}
								}
							}
						}

						tx.success();
					}
				}

				if (importFiles) {

					System.out.println("Importing files..");
					logger.println("Importing files..");

					final SecurityContext superUserSecurityContext           = SecurityContext.getSuperUserInstance();
					final Iterator<Path> pathIteratorForRelationshipCreation = files.iterator();

					while (pathIteratorForRelationshipCreation.hasNext()) {

						try (final Tx tx = StructrApp.getInstance().tx()) {

							while (pathIteratorForRelationshipCreation.hasNext()) {

								final Path file     = pathIteratorForRelationshipCreation.next();
								final String name   = file.getFileName().toString();
								final int pos       = name.indexOf(".", 7);
								final String idPart = name.substring(6, pos == -1 ? name.length() : pos);
								boolean found       = false;

								if (name.startsWith("KBlob-") && name.length() > 23) {

									for (final Tuple<Class, PropertyKey> entry : mapping) {

										final Class type                 = entry.getKey();
										final PropertyKey key            = entry.getValue();
										final boolean isMultiple         = (key instanceof ArrayProperty);
										Object value                     = idPart;

										if (isMultiple) {
											value = new String[] { idPart };
										}

										final Query<NodeInterface> query = app.nodeQuery().andType(type).and(key, value, false);
										final List<NodeInterface> nodes  = query.getAsList();

										if (nodes.size() == 1) {

											final String capitalJsonName = StringUtils.capitalize(key.jsonName());
											final String targetJsonName  = "has" + capitalJsonName;
											final NodeInterface node     = nodes.get(0);

											final PropertyKey fileRelationshipKey = StructrApp.key(type, targetJsonName);
											if (fileRelationshipKey != null) {

												try (final InputStream is = new FileInputStream(file.toFile())) {

													// import file..
													final Class fileType = ImageHelper.isImageType(name) ? Image.class : File.class;

													if (isMultiple) {

														final String[] possibleNames = (String[])node.getProperty(key);
														String actualName            = name;

														for (final String possibleName : possibleNames) {

															if (possibleName.startsWith(name)) {

																actualName = possibleName.substring(name.length() + 1);
																break;
															}
														}

														logger.println("        Importing " + name + " => " + actualName);

														final File importedFile   = FileHelper.createFile(superUserSecurityContext, is, null, fileType, actualName);
														final List<File> fileList = (List<File>)node.getProperty(fileRelationshipKey);
														fileList.add(importedFile);

														node.setProperty(fileRelationshipKey, fileList);

													} else {

														final String possibleName = (String)node.getProperty(key);
														String actualName         = name;

														if (possibleName != null) {
															actualName = possibleName.substring(name.length() + 1);
														}

														logger.println("        Importing " + name + " => " + actualName);

														final File importedFile = FileHelper.createFile(superUserSecurityContext, is, null, fileType, actualName);
														node.setProperty(fileRelationshipKey, importedFile);
													}

												} catch (Throwable t) {
													t.printStackTrace();
												}

											} else {

												System.out.println("############################# INVALID KEY " + type.getSimpleName() + "." + targetJsonName + ", not found??!");
												logger.println("############################# INVALID KEY " + type.getSimpleName() + "." + targetJsonName + ", not found??!");
											}

											found = true;

											// no need to search further
											break;
										}
									}
								}

								if (!found) {

									System.out.println("Found NO document for file " + name + ", importing without association");
									logger.println("Found NO document for file " + name + ", importing without association");

									try (final InputStream is = new FileInputStream(file.toFile())) {

										// import file..
										final Class fileType = ImageHelper.isImageType(name) ? Image.class : File.class;
										FileHelper.createFile(superUserSecurityContext, is, null, fileType, name);

									} catch (Throwable t) {
										t.printStackTrace();
									}
								}

								if (++count == 100) {

									count = 0;
									break;
								}
							}

							tx.success();
						}
					}
				}
			}


		} catch(Throwable t) {

			t.printStackTrace();

			success = false;
		}


		if (success) {

			System.out.println("Import successful");
			logger.println("Import successful");
		}

		logger.flush();
		logger.close();
	}

	private void addProperty(final JsonType type, final OWLProperty property, final String name) throws URISyntaxException, FrameworkException {

		final String id = property.getId().toString();
		if (blacklistedProperties.contains(id)) {

			System.out.println("DatatypeProperty " + property.getId().toString() + " is blacklisted, igoring!");
			logger.println("DatatypeProperty " + property.getId().toString() + " is blacklisted, igoring!");

			return;
		}

		final String propertyType = property.getStructrType();
		if (propertyType != null) {

			final String propertyName = property.getStructrName(false);
			if (propertyName != null) {

				switch (propertyType) {

					case "String":
						if (property.multipleOccurrences()) {
							type.addStringArrayProperty(propertyName);
						} else {
							type.addStringProperty(propertyName).setIndexed(true);
						}
						break;

					case "Boolean":
						type.addBooleanProperty(propertyName).setIndexed(true);
						break;

					case "Integer":
						type.addIntegerProperty(propertyName).setIndexed(true);
						break;

					case "Double":
						type.addNumberProperty(propertyName).setIndexed(true);
						break;

					case "Date":
						final JsonDateProperty dateProperty = type.addDateProperty(propertyName);
						final String additionalFormat = property.getAdditionalFormat();
						if (additionalFormat != null) {

							dateProperty.setDatePattern(additionalFormat);
						}
						dateProperty.setIndexed(true);
						break;

					case "Enum":
						if (property.multipleOccurrences()) {

							type.addStringArrayProperty(propertyName);

						} else {

							final JsonEnumProperty enumProperty = type.addEnumProperty(propertyName);
							enumProperty.setEnums(property.getEnums(name));
							enumProperty.setIndexed(true);

						}
						break;

					case "File":
						break;
				}
			}
		}

	}

	private void parseDocument(final Element element, final int depth) {

		final String elementType  = element.getTagName();
		switch (elementType) {

			case "owl:Class":
			case "owl:ObjectProperty":
				final OWLClass owlClass = new OWLClass(element);
				owlClassesByURI.put(owlClass.getId(), owlClass);

				final String fragment = owlClass.getFragmentName(true);
				if (fragment != null) {

					owlClassesByFragment.put(fragment, owlClass);
				}
				return;

			case "rdf:Property":
				break;

			case "rdf:Description":
				final RDFDescription description = new RDFDescription(element);
				rdfDescriptions.put(description.getReferenceId(), description);
				return;

			case "owl:DatatypeProperty":
				final OWLProperty owlProperty = new OWLProperty(element);
				owlPropertiesByURI.put(owlProperty.getId(), owlProperty);

				final String rawFragmentName = owlProperty.getRawFragmentName();
				if (rawFragmentName != null) {

					owlPropertiesByName.put(rawFragmentName, owlProperty);
				}
				return;

			default:

				if (importData) {

					final OWLInstance instance = new OWLInstance(element);
					final URI id               = instance.getId();
					if (id != null) {

						final String type = instance.getTypeName();
						if (type != null) {

							final OWLClass owlType = owlClassesByFragment.get(type);
							if (owlType != null) {

								instance.setType(owlType);
							}
						}

						owlInstances.put(instance.getId(), instance);
					}
				}
				break;
		}

		// do not descend further
		if (depth >= 1) {
			return;
		}

		// recurse
		for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {

			if (child instanceof Element) {

				parseDocument((Element)child, depth+1);
			}
		}
	}

	private <T extends RDFItem> void filter(final Collection<T> items) {

		for (final Iterator<T> it = items.iterator(); it.hasNext();) {

			final T t             = it.next();
			final URI id          = t.getId();
			final String idString = id.toString();
			final String fragment = id.getFragment();

			for (final String unwanted : unwantedPrefixes) {

				if (idString.startsWith(unwanted) || fragment.startsWith(unwanted)) {
					it.remove();
				}
			}
		}
	}

	private List<Tuple<Class, PropertyKey>> createPropertyKeyMapping(final ConfigurationProvider config) {

		final List<Tuple<Class, PropertyKey>> mapping = new LinkedList<>();
		final String[][] sourceMapping = {

			{ "UploadedDocument", "dok" },
			{ "Pruefung",         "pruefbericht" },
			{ "Pruefauftrag",     "pruefbericht" },
			{ "Article",          "bild" },
			{ "ProductVersion",   "bild" },
			{ "Pers",             "bild" },
			{ "Product",          "bildKlein" },
			{ "Component",        "bildKlein" },
			{ "Product",          "bildGross" },
			{ "Component",        "bos" },
			{ "ProductVersion",   "erpLink" },
			{ "FckFile",          "fckBlob" },
			{ "UserAccount",      "nnState" },
			{ "Regulation",       "anhang"  },
			{ "HistoryVersions",  "dokumentDateiVersion" }
		};

		for (final String[] entry : sourceMapping) {

			final String className    = entry[0];
			final String propertyName = entry[1];

			final Class<NodeInterface> type = config.getNodeEntityClass(className);
			if (type != null) {

				final PropertyKey key = StructrApp.key(type, propertyName);
				if (key != null) {

					mapping.add(new Tuple(type, key));
				}
			}
		}

		return mapping;
	}

	// ----- nested classes -----
	private static class Visitor implements FileVisitor<Path> {

		private Set<Path> files = null;

		public Visitor(final Set<Path> files) {
			this.files = files;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			files.add(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	private static class Tuple<K, V> {

		private K key = null;
		private V value = null;

		public Tuple(K a, V b) {
			this.key = a;
			this.value = b;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}
	}
}
