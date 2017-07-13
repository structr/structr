/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.structr.api.service.LicenseManager;
import org.structr.common.ResultTransformer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.module.StructrModule;
import org.structr.module.api.APIBuilder;
import org.structr.schema.action.Actions;

/**
 *
 */
public class APIBuilderModule implements StructrModule, APIBuilder {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
		// check and read configuration..
	}

	@Override
	public String getName() {
		return "api-builder";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}

	// ----- interface APIBuilder -----
	@Override
	public ResultTransformer createMapping(final App app, final String sourceType, final String targetType, final Map<String, String> propertyMappings, final Map<String, String> transforms) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final VirtualType type = app.create(VirtualType.class,
				new NodeAttribute<>(VirtualType.sourceType, sourceType),
				new NodeAttribute<>(VirtualType.name, targetType)
			);

			int i = 0;

			for (final Entry<String, String> entry : propertyMappings.entrySet()) {

				final String sourceProperty = entry.getKey();
				final String targetProperty = entry.getValue();

				app.create(VirtualProperty.class,
					new NodeAttribute<>(VirtualProperty.virtualType, type),
					new NodeAttribute<>(VirtualProperty.sourceName, sourceProperty),
					new NodeAttribute<>(VirtualProperty.targetName, targetProperty),
					new NodeAttribute<>(VirtualProperty.position, i++),
					new NodeAttribute<>(VirtualProperty.inputFunction, transforms.get(sourceProperty))
				);
			}

			tx.success();

			return type;
		}
	}

	@Override
	public void removeMapping(final App app, final String sourceType, final String targetType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final VirtualType type = app.nodeQuery(VirtualType.class).andName(targetType).getFirst();
			if (type != null) {

				for (final VirtualProperty property : type.getProperty(VirtualType.properties)) {

					app.delete(property);
				}

				app.delete(type);
			}

			tx.success();
		}
	}
}
