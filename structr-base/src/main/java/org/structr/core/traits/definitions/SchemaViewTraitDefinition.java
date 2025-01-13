/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.traits.definitions;

import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.SchemaViewTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class SchemaViewTraitDefinition extends AbstractTraitDefinition {

	public static final String schemaViewNamePattern    = "[a-zA-Z_][a-zA-Z0-9_]*";

	/*
	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		id, name, schemaNode, schemaProperties, nonGraphProperties
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		id, name, schemaNode, schemaProperties, nonGraphProperties, isBuiltinView, sortOrder
	);

	public static final View schemaView = new View(SchemaView.class, "schema",
		id, name, schemaNode, schemaProperties, nonGraphProperties, isBuiltinView, sortOrder
	);

	public static final View exportView = new View(SchemaView.class, "export",
		id, typeHandler, name, schemaNode, nonGraphProperties, isBuiltinView, sortOrder
	);
	*/

	public SchemaViewTraitDefinition() {
		super("SchemaView");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of(
			SchemaView.class, (traits, node) -> new SchemaViewTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface>           schemaNode         = new StartNode("schemaNode", "SchemaNodeView", new PropertySetNotion(Set.of(Traits.idProperty(), Traits.nameProperty())));
		final Property<Iterable<NodeInterface>> schemaProperties   = new EndNodes("schemaProperties", "SchemaViewProperty", new PropertySetNotion(Set.of(Traits.idProperty(), Traits.nameProperty())));
		final Property<Boolean>                 isBuiltinView      = new BooleanProperty("isBuiltinView");
		final Property<String>                  nonGraphProperties = new StringProperty("nonGraphProperties");
		final Property<String>                  sortOrder          = new StringProperty("sortOrder");

		return Set.of(
			schemaNode,
			schemaProperties,
			isBuiltinView,
			nonGraphProperties,
			sortOrder
		);
	}

	/*
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, schemaViewNamePattern, errorBuffer);

		// check case-insensitive name uniqueness on current type
		final String thisViewName       = getProperty(AbstractNode.name);
		final AbstractSchemaNode parent = this.getProperty(SchemaView.schemaNode);

		try {

			final List<SchemaView> viewsOnParent = StructrApp.getInstance().nodeQuery("SchemaView").and(SchemaView.name, thisViewName).and(SchemaView.schemaNode, parent).getAsList();

			for (final SchemaView schemaView : viewsOnParent) {

				final boolean isSameNode             = this.getUuid().equals(schemaView.getUuid());
				final boolean isSameNameIgnoringCase = thisViewName.equalsIgnoreCase(schemaView.getName());

				if (!isSameNode && isSameNameIgnoringCase) {

					errorBuffer.add(new SemanticErrorToken(this.getType(), "name", "already_exists").withValue(thisViewName).withDetail("Multiple views with identical names (case-insensitive) are not supported on the same level"));
					valid = false;
				}
			}

		} catch (FrameworkException fex) {

			errorBuffer.add(new SemanticErrorToken(this.getType(),"none", "exception_occurred").withValue(thisViewName).withDetail("Exception occurred while checking uniqueness of view name - please retry. Cause: " + fex.getMessage()));
			valid = false;
		}

		return valid;
	}

	@Override
	public boolean reloadSchemaOnCreate() {
		return true;
	}

	@Override
	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {
		return true;
	}

	@Override
	public boolean reloadSchemaOnDelete() {
		return true;
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (getProperty(schemaNode) == null) {
			StructrApp.getInstance().delete(this);
		}
	}
	*/
}
