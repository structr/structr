/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.entity;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaNodeView;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.schema.ReloadSchema;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractSchemaNode extends AbstractNode {

	public static final Property<List<SchemaProperty>> schemaProperties = new EndNodes<>("schemaProperties", SchemaNodeProperty.class);
	public static final Property<List<SchemaMethod>>   schemaMethods    = new EndNodes<>("schemaMethods", SchemaNodeMethod.class);
	public static final Property<List<SchemaView>>     schemaViews      = new EndNodes<>("schemaViews", SchemaNodeView.class);

	public static final View defaultView = new View(AbstractSchemaNode.class, PropertyView.Public,
		name
	);

	public static final View uiView = new View(SchemaNode.class, PropertyView.Ui,
		name, schemaProperties, schemaViews, schemaMethods
	);

	static {

		AbstractNode.name.addValidator(new TypeUniquenessValidator<String>(AbstractSchemaNode.class));
	}

	public String getClassName() {
		return getProperty(name);
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			// register transaction post processing that recreates the schema information
			TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer)) {

			// register transaction post processing that recreates the schema information
			TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

			return true;
		}

		return false;
	}

	@Override
	public void onNodeDeletion() {

		Services.getInstance().getConfigurationProvider().unregisterEntityType(getClassName());

		final String signature = getResourceSignature();
		if (StringUtils.isNotBlank(signature)) {

			SchemaHelper.removeDynamicGrants(getResourceSignature());
		}

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

	}

	public String getResourceSignature() {
		//return SchemaHelper.normalizeEntityName(getProperty(name));
		return getProperty(name);
	}
}
