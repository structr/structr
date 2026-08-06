/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.entity;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.ExampleData;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.ImporterWithXMLParser;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public interface Widget extends NodeInterface {

	ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\[[^\\]]+\\]");

	String getSource();
	String getShortDescription();
	String getDescription();
	boolean isWidget();
	String getSvgPath();
	String getThumbnailPath();
	String getTreePath();
	String getConfiguration();
	boolean isPageTemplate();
	boolean isExclusiveInParent();
	boolean isRenderTemplate();
	String[] getSelectors();
	String getComponentType();
	Integer getDimensions();

	/**
	 * Parses HTML code from the "source" entry in the given parameters map into a set of DOMNodes that are created
	 * in the given page, with the given parent. An additional config entry in the parameters can be specified to
	 * add the "componentType" and "dimensions" attributes to the newly created widget, but for historical reasons,
	 * the config object must be of type JsonSingleInput! Caution: this method uses the XML-based HTML parser to
	 * import fragments, so there might be subtle errors in the imported structure..
	 *
	 * @param page
	 * @param parent
	 * @param baseUrl
	 * @param parameters
	 * @param processDeploymentInfo
	 * @throws FrameworkException
	 */
	static DOMNode expandWidget(final Page page, final DOMNode parent, final String baseUrl, final Map<String, Object> parameters, final boolean processDeploymentInfo) throws FrameworkException {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final App app                         = StructrApp.getInstance(securityContext);
		final List<String> attributes         = new LinkedList<>();
		final ErrorBuffer errorBuffer         = new ErrorBuffer();
		String _source = (String) parameters.get("source");

		if (_source == null) {

			errorBuffer.add(new EmptyPropertyToken(Widget.class.getSimpleName(), "source"));

		} else {

			// check source for mandatory parameters
			final Matcher matcher = threadLocalTemplateMatcher.get();

			// initialize with source
			matcher.reset(_source);

			// create default objects if keywords from "append widget" dialog are present
			if ("create:schema".equals(parameters.get("dataSource"))) {

				attributes.addAll(createSchemaDataSource(app, parameters));
			}

			if ("create:datasource".equals(parameters.get("dataSource"))) {

				final String name              = (String) parameters.get("dataSourceName");
				final NodeInterface dataSource = app.create(StructrTraits.SCRIPT_DATA_SOURCE, name);

				parameters.put("dataSource", "node:" + dataSource.getUuid());
			}

			if ("create:query".equals(parameters.get("dataSource"))) {

				final String name              = (String) parameters.get("dataSourceName");
				final NodeInterface dataSource = app.create(StructrTraits.QUERY_DATA_SOURCE, name);

				parameters.put("dataSource", "node:" + dataSource.getUuid());
			}

			if ("create-new-data-adapter".equals(parameters.get("dataAdapter"))) {

				throw new IllegalStateException("Data adapter creation is done elsewhere now, please do not use this special key.");
			}

			while (matcher.find()) {

				final String group         = matcher.group();
				final String source        = group.substring(1, group.length() - 1);
				final ReplacementInfo info = new ReplacementInfo(source);
				final String key           = info.getKey();
				final Object value         = parameters.get(key);

				if (value != null) {

					// replace and restart matching process
					_source = _source.replace(group, value.toString());
					matcher.reset(_source);
				}
			}
		}

		if (!errorBuffer.hasError()) {

			final ImporterWithXMLParser importer = new ImporterWithXMLParser(securityContext, _source, baseUrl, null, false, false, false, false);
			final JsonSingleInput configData     = ((JsonSingleInput) parameters.get("config"));

			importer.setIsDeployment(true);
			importer.setCommentHandler(new DeploymentCommentHandler());

			if (importer.parse(true)) {

				final DOMNode imported = importer.createChildNodes(parent, page, true);
				final JsonInput config = configData != null ? configData.getFirst() : null;

				if (parent != null) {

					// set componentRoot flag so we don't cross component boundaries
					for (final DOMNode newChild : parent.getChildren()) {

						if (config != null) {

							// hasSharedComponent is not set yet, because it is set in a lifecycle method
							final DOMNode sharedComponent = newChild.getSharedComponent();
							if (sharedComponent != null) {

								sharedComponent.setComponentType((String) config.get("componentType"));
								sharedComponent.setDimensions(toInt(config.get("dimensions")));

							} else {

								newChild.setComponentType((String) config.get("componentType"));
								newChild.setDimensions(toInt(config.get("dimensions")));
							}
						}

						newChild.setIsComponentRoot(true);

						if (!attributes.isEmpty()) {

							final ComponentConfiguration componentConfiguration = newChild.getComponentConfiguration();
							if (componentConfiguration != null) {

								componentConfiguration.setFieldSet(StringUtils.join(attributes, ","));
							}
						}
					}

					final String tableChildElement = importer.getTableChildElement();
					if (tableChildElement != null) {

						for (final NodeInterface child : parent.getAllChildNodes()) {

							if (child.getType().toLowerCase().equals(tableChildElement)) {

								parent.appendChild(child.as(DOMNode.class));

							} else {

								StructrApp.getInstance().delete(child);
							}
						}
					}
				}

				return imported;
			}

		} else {

			// report error to ui
			throw new FrameworkException(422, "Unable to import the given source code", errorBuffer);
		}

		return null;
	}

	static List<String> createSchemaDataSource(final App app, final Map<String, Object> parameters) throws FrameworkException {

		final String name              = (String) parameters.get("dataSourceName");
		final NodeInterface dataSource = app.create(StructrTraits.SCHEMA_NODE, name);
		final List<String> attributes  = new LinkedList<>();

		parameters.put("dataSource", "node:" + dataSource.getName());

		// process attributes
		if (parameters.containsKey("dataSourceAttribute0") && parameters.containsKey("dataSourceType0")) {

			final Traits schemaPropertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);
			boolean found                     = true;
			int index                         = 0;

			while (found) {

				found = false;

				final String attributeName = (String) parameters.get("dataSourceAttribute" + index);
				final String attributeType = (String) parameters.get("dataSourceType" + index);

				if (attributeName != null && attributeType != null) {

					attributes.add(attributeName);

					app.create(StructrTraits.SCHEMA_PROPERTY,
						new NodeAttribute<>(schemaPropertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), dataSource),
						new NodeAttribute<>(schemaPropertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), attributeName),
						new NodeAttribute<>(schemaPropertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), attributeType)
					);

					found = true;
				}

				index++;
			}
		}

		// checkbox without value sends the string "on"
		if ("on".equals(parameters.get("dataSourceCreateExampleData"))) {

			TransactionCommand.registerTransactionListener(ExampleData.postProcess(name, attributes, 5));
		}

		return attributes;
	}

	static Integer toInt(final Object value) {

		if (value != null && value instanceof Number number) {

			return number.intValue();
		}

		return null;
	}

	class ReplacementInfo {

		private final ArrayList<String> options = new ArrayList<>();
		private String key                = null;
		private boolean hasOptions        = false;

		public ReplacementInfo(final String value) {

			this.key = value;

			if (value.contains(":")) {

				final String[] parts = value.split("[:]+");
				this.key = parts[0];

				if (parts[1].contains(",")) {

					final String[] opts = parts[1].split("[,]+");
					final int count     = opts.length;

					for (int i=0; i<count; i++) {

						final String trimmedPart = opts[i].trim();
						if (!trimmedPart.isEmpty()) {

							options.add(trimmedPart);
						}
					}

					hasOptions = true;
				}
			}
		}

		public String getKey() {

			return key;
		}

		public ArrayList<String> getOptions() {

			return options;
		}

		public boolean hasOptions() {

			return hasOptions;
		}
	}
}
