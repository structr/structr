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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.wrappers.SchemaPropertyTraitWrapper;

import java.util.Map;
import java.util.Set;

public class SchemaPropertyTraitDefinition extends AbstractTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(SchemaProperty.class.getName());

	private static final String schemaPropertyNamePattern = "[_A-Za-z][\\-_0-9A-Za-z]*";

	private static final Property<NodeInterface>           schemaNode    = new StartNode("schemaNode", "SchemaNodeProperty", new PropertySetNotion(Traits.idProperty(), Traits.nameProperty(), Traits.of("SchemaNode").key("isBuiltinType")));
	private static final Property<Iterable<NodeInterface>> schemaViews   = new StartNodes("schemaViews", "SchemaViewProperty", new PropertySetNotion(Traits.idProperty(), Traits.nameProperty()));
	private static final Property<Iterable<NodeInterface>> excludedViews = new StartNodes("excludedViews", "SchemaExcludedViewProperty", new PropertySetNotion(Traits.idProperty(), Traits.nameProperty()));

	private static final Property<String>             declaringUuid         = new StringProperty("declaringUuid");
	private static final Property<String>             declaringClass        = new StringProperty("declaringClass");
	private static final Property<String>             defaultValue          = new StringProperty("defaultValue");
	private static final Property<String>             propertyType          = new StringProperty("propertyType").indexed();
	private static final Property<String>             contentType           = new StringProperty("contentType");
	private static final Property<String>             dbName                = new StringProperty("dbName");
	private static final Property<String>             fqcn                  = new StringProperty("fqcn");
	private static final Property<String>             format                = new StringProperty("format");
	private static final Property<String>             typeHint              = new StringProperty("typeHint");
	private static final Property<String>             hint                  = new StringProperty("hint");
	private static final Property<String>             category              = new StringProperty("category");
	private static final Property<Boolean>            notNull               = new BooleanProperty("notNull");
	private static final Property<Boolean>            compound              = new BooleanProperty("compound");
	private static final Property<Boolean>            unique                = new BooleanProperty("unique");
	private static final Property<Boolean>            indexed               = new BooleanProperty("indexed");
	private static final Property<Boolean>            readOnly              = new BooleanProperty("readOnly");
	private static final Property<Boolean>            isDynamic             = new BooleanProperty("isDynamic");
	private static final Property<Boolean>            isBuiltinProperty     = new BooleanProperty("isBuiltinProperty");
	private static final Property<Boolean>            isPartOfBuiltInSchema = new BooleanProperty("isPartOfBuiltInSchema");
	private static final Property<Boolean>            isDefaultInUi         = new BooleanProperty("isDefaultInUi");
	private static final Property<Boolean>            isDefaultInPublic     = new BooleanProperty("isDefaultInPublic");
	private static final Property<Boolean>            isCachingEnabled      = new BooleanProperty("isCachingEnabled").defaultValue(false);
	private static final Property<String>             contentHash           = new StringProperty("contentHash");
	private static final Property<String>             readFunction          = new StringProperty("readFunction");
	private static final Property<String>             writeFunction         = new StringProperty("writeFunction");
	private static final Property<String>             openAPIReturnType     = new StringProperty("openAPIReturnType");
	private static final Property<String[]>           validators            = new ArrayProperty("validators", String.class);
	private static final Property<String[]>           transformers          = new ArrayProperty("transformers", String.class);

	/*
	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		id, typeHandler, name, dbName, schemaNode, schemaViews, excludedViews, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		id, typeHandler, name, dbName, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, schemaNode, schemaViews, excludedViews, propertyType, contentType, fqcn, format, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View schemaView = new View(SchemaProperty.class, "schema",
		id, typeHandler, name, dbName, schemaNode, excludedViews, schemaViews, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View exportView = new View(SchemaProperty.class, "export",
		id, typeHandler, name, schemaNode, schemaViews, excludedViews, dbName, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);
	*/

	public SchemaPropertyTraitDefinition() {
		super("SchemaProperty");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidStringMatchingRegex(obj, Traits.nameProperty(), schemaPropertyNamePattern, errorBuffer);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaProperty.class, (traits, node) -> new SchemaPropertyTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(

			schemaNode,
			schemaViews,
			excludedViews,
			declaringUuid,
			declaringClass,
			defaultValue,
			propertyType,
			contentType,
			dbName,
			fqcn,
			format,
			typeHint,
			hint,
			category,
			notNull,
			compound,
			unique,
			indexed,
			readOnly,
			isDynamic,
			isBuiltinProperty,
			isPartOfBuiltInSchema,
			isDefaultInUi,
			isDefaultInPublic,
			isCachingEnabled,
			contentHash,
			readFunction,
			writeFunction,
			openAPIReturnType,
			validators,
			transformers
		);
	}

	/*
	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		// automatically add new property to the Ui or Custom view
		final AbstractSchemaNode parent = getProperty(SchemaProperty.schemaNode);
		if (parent != null) {

			// register property (so we have a chance to back up an existing builtin property)
			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type = conf.getNodeEntityClass(parent.getName());

			if (type != null) {
				conf.registerProperty(type, conf.getPropertyKeyForJSONName(type, getPropertyName()));
			}

			final String viewToAddTo;
			if (getProperty(isBuiltinProperty)) {
				viewToAddTo = PropertyView.Ui;
			} else {
				viewToAddTo = PropertyView.Custom;
			}

			for (final SchemaView view : parent.getProperty(AbstractSchemaNode.schemaViews)) {

				if (viewToAddTo.equals(view.getName())) {

					final Set<SchemaProperty> properties = Iterables.toSet(view.getProperty(SchemaView.schemaProperties));

					properties.add(this);

					view.setProperty(SchemaView.schemaProperties, new LinkedList<>(properties));

					break;
				}
			}
		}
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		final String uuid = getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}

		if (getProperty(schemaNode) == null) {
			StructrApp.getInstance().delete(this);
		} else {

			// prevent modification of properties using a content hash value
			if (getProperty(isBuiltinProperty) && !getContentHash().equals(getProperty(contentHash))) {
				throw new FrameworkException(403, "Modification of built-in properties not permitted.");
			}
		}
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

		super.onNodeDeletion(securityContext);

		final String thisName = getName();

		// remove property from the sortOrder of views it is used in (directly)
		for (SchemaView view : getProperty(SchemaProperty.schemaViews)) {

			final String sortOrder = view.getProperty(SchemaView.sortOrder);

			if (sortOrder != null) {

				try {
					view.setProperty(SchemaView.sortOrder, StringUtils.join(Arrays.stream(sortOrder.split(",")).filter(propertyName -> !thisName.equals(propertyName)).toArray(), ","));
				} catch (FrameworkException ex) {
					logger.error("Unable to remove property '{}' from view '{}'", thisName, view.getUuid());
				}
			}
		}
	}
	*/
}
