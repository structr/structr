/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.transform;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.module.StructrModule;
import org.structr.module.api.APIBuilder;
import org.structr.transform.traits.definitions.VirtualPropertyTraitDefinition;
import org.structr.transform.traits.definitions.VirtualTypeTraitDefinition;
import org.structr.transform.traits.relationship.VirtualTypevirtualPropertyVirtualProperty;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 */
public class APIBuilderModule implements StructrModule, APIBuilder {

	private static final Logger logger = LoggerFactory.getLogger(APIBuilderModule.class.getName());

	@Override
	public void onLoad() {

		StructrTraits.registerTrait(new VirtualTypevirtualPropertyVirtualProperty());
		StructrTraits.registerTrait(new VirtualTypeTraitDefinition());
		StructrTraits.registerTrait(new VirtualPropertyTraitDefinition());

		StructrTraits.registerRelationshipType(StructrTraits.VIRTUAL_TYPE_VIRTUAL_PROPERTY_VIRTUAL_PROPERTY, StructrTraits.VIRTUAL_TYPE_VIRTUAL_PROPERTY_VIRTUAL_PROPERTY);

		StructrTraits.registerNodeType(StructrTraits.VIRTUAL_TYPE,     StructrTraits.VIRTUAL_TYPE);
		StructrTraits.registerNodeType(StructrTraits.VIRTUAL_PROPERTY, StructrTraits.VIRTUAL_PROPERTY);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "api-builder";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public boolean hasDeploymentData () {
		return true;
	}

	@Override
	public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {

		final App app                                = StructrApp.getInstance();
		final Path virtualTypesFile                  = target.resolve("virtual-types.json");
		final List<Map<String, Object>> virtualTypes = new LinkedList();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface virtualTypeNode : app.nodeQuery(StructrTraits.VIRTUAL_TYPE).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final VirtualType virtualType   = virtualTypeNode.as(VirtualType.class);
				final Map<String, Object> entry = new TreeMap<>();
				virtualTypes.add(entry);

				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         virtualType.getName());
				entry.put(VirtualTypeTraitDefinition.SOURCE_TYPE_PROPERTY,                    virtualType.getSourceType());
				entry.put(VirtualTypeTraitDefinition.POSITION_PROPERTY,                       virtualType.getPosition());
				entry.put(VirtualTypeTraitDefinition.FILTER_EXPRESSION_PROPERTY,              virtualType.getFilterExpression());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, virtualType.isVisibleToAuthenticatedUsers());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        virtualType.isVisibleToPublicUsers());

				final List<Map<String, Object>> properties = new LinkedList();
				entry.put(VirtualTypeTraitDefinition.PROPERTIES_PROPERTY, properties);

				for (final NodeInterface node : virtualType.getVirtualProperties()) {

					final VirtualProperty virtualProperty = node.as(VirtualProperty.class);
					final Map<String, Object> virtualPropEntry = new TreeMap<>();
					properties.add(virtualPropEntry);

					virtualPropEntry.put(VirtualPropertyTraitDefinition.SOURCE_NAME_PROPERTY,                virtualProperty.getSourceName());
					virtualPropEntry.put(VirtualPropertyTraitDefinition.TARGET_NAME_PROPERTY,                virtualProperty.getTargetName());
					virtualPropEntry.put(VirtualPropertyTraitDefinition.INPUT_FUNCTION_PROPERTY,             virtualProperty.getInputFunction());
					virtualPropEntry.put(VirtualPropertyTraitDefinition.OUTPUT_FUNCTION_PROPERTY,            virtualProperty.getOutputFunction());
					virtualPropEntry.put(VirtualPropertyTraitDefinition.POSITION_PROPERTY,                   virtualProperty.getPosition());
					virtualPropEntry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        virtualProperty.isVisibleToPublicUsers());
					virtualPropEntry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, virtualProperty.isVisibleToAuthenticatedUsers());
				}
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(virtualTypesFile.toFile()))) {

			gson.toJson(virtualTypes, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	@Override
	public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

		final Path virtualTypesConf = source.resolve("virtual-types.json");
		if (Files.exists(virtualTypesConf)) {

			logger.info("Reading {}..", virtualTypesConf);

			try (final Reader reader = Files.newBufferedReader(virtualTypesConf, Charset.forName("utf-8"))) {

				final List<Map<String, Object>> virtualTypes = gson.fromJson(reader, List.class);

				final SecurityContext context = SecurityContext.getSuperUserInstance();
				context.setDoTransactionNotifications(false);

				final App app                 = StructrApp.getInstance(context);

				try (final Tx tx = app.tx()) {

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.VIRTUAL_TYPE).getAsList()) {
						app.delete(toDelete);
					}

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.VIRTUAL_PROPERTY).getAsList()) {
						app.delete(toDelete);
					}

					for (final Map<String, Object> entry : virtualTypes) {

						final PropertyMap map = PropertyMap.inputTypeToJavaType(context, StructrTraits.VIRTUAL_TYPE, entry);

						app.create(StructrTraits.VIRTUAL_TYPE, map);
					}

					tx.success();
				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}
	}

	// ----- interface APIBuilder -----
	@Override
	public ResultTransformer createMapping(final App app, final String sourceType, final String targetType, final Map<String, String> propertyMappings, final Map<String, String> transforms) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Traits typeTraits = Traits.of(StructrTraits.VIRTUAL_TYPE);
			final Traits propTraits = Traits.of(StructrTraits.VIRTUAL_PROPERTY);

			final NodeInterface type = app.create(StructrTraits.VIRTUAL_TYPE,
				new NodeAttribute<>(typeTraits.key(VirtualTypeTraitDefinition.SOURCE_TYPE_PROPERTY), sourceType),
				new NodeAttribute<>(typeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),      targetType)
			);

			int i = 0;

			for (final Entry<String, String> entry : propertyMappings.entrySet()) {

				final String sourceProperty = entry.getKey();
				final String targetProperty = entry.getValue();

				app.create(StructrTraits.VIRTUAL_PROPERTY,
					new NodeAttribute<>(propTraits.key(VirtualPropertyTraitDefinition.VIRTUAL_TYPE_PROPERTY),   type),
					new NodeAttribute<>(propTraits.key(VirtualPropertyTraitDefinition.SOURCE_NAME_PROPERTY),    sourceProperty),
					new NodeAttribute<>(propTraits.key(VirtualPropertyTraitDefinition.TARGET_NAME_PROPERTY),    targetProperty),
					new NodeAttribute<>(propTraits.key(VirtualPropertyTraitDefinition.POSITION_PROPERTY),       i++),
					new NodeAttribute<>(propTraits.key(VirtualPropertyTraitDefinition.INPUT_FUNCTION_PROPERTY), transforms.get(sourceProperty))
				);
			}

			tx.success();

			return type.as(VirtualType.class);
		}
	}

	@Override
	public void removeMapping(final App app, final String sourceType, final String targetType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.VIRTUAL_TYPE).name(targetType).getFirst();
			if (node != null) {

				final VirtualType type = node.as(VirtualType.class);

				for (final NodeInterface property : type.getVirtualProperties()) {

					app.delete(property);
				}

				app.delete(node);
			}

			tx.success();
		}
	}
}
