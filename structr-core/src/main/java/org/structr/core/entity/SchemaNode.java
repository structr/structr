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

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaNotion;
import org.structr.schema.action.ActionEntry;
import org.structr.schema.action.Actions;
import org.structr.schema.parser.Validator;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNode extends AbstractSchemaNode implements Schema {

	public static final Property<List<SchemaNode>>  relatedTo        = new EndNodes<>("relatedTo", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	public static final Property<List<SchemaNode>>  relatedFrom      = new StartNodes<>("relatedFrom", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	public static final Property<String>            extendsClass     = new StringProperty("extendsClass").indexed();
	public static final Property<String>            defaultSortKey   = new StringProperty("defaultSortKey");
	public static final Property<String>            defaultSortOrder = new StringProperty("defaultSortOrder");
	public static final Property<Boolean>           isBuiltinType    = new BooleanProperty("isBuiltinType").readOnly().indexed();

	static {

		name.addValidator(new TypeUniquenessValidator<String>(SchemaNode.class));
	}

	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		name, extendsClass, relatedTo, relatedFrom, defaultSortKey, defaultSortOrder, isBuiltinType
	);

	public static final View uiView = new View(SchemaNode.class, PropertyView.Ui,
		name, extendsClass, relatedTo, relatedFrom, defaultSortKey, defaultSortOrder, isBuiltinType
	);

	private final Set<String> dynamicViews = new LinkedHashSet<>();

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		final List<PropertyKey> propertyKeys = new LinkedList<>(Iterables.toList(super.getPropertyKeys(propertyView)));

		// add "custom" property keys as String properties
		for (final String key : SchemaHelper.getProperties(getNode())) {

			final PropertyKey newKey = new StringProperty(key);
			newKey.setDeclaringClass(getClass());

			propertyKeys.add(newKey);
		}

		Collections.sort(propertyKeys, new Comparator<PropertyKey>() {

			@Override
			public int compare(PropertyKey o1, PropertyKey o2) {
				return o1.jsonName().compareTo(o2.jsonName());
			}
		});

		return new LinkedHashSet<>(propertyKeys);
	}

	@Override
	public String getSource(final ErrorBuffer errorBuffer) throws FrameworkException {

		final Map<Actions.Type, List<ActionEntry>> saveActions = new EnumMap<>(Actions.Type.class);
		final Map<String, Set<String>> viewProperties          = new LinkedHashMap<>();
		final Set<String> existingPropertyNames                = new LinkedHashSet<>();
		final Set<String> propertyNames                        = new LinkedHashSet<>();
		final Set<Validator> validators                        = new LinkedHashSet<>();
		final Set<String> enums                                = new LinkedHashSet<>();
		final StringBuilder src                                = new StringBuilder();
		final Class baseType                                   = AbstractNode.class;
		final String _className                                = getProperty(name);
		final String _extendsClass                             = getProperty(extendsClass);

		src.append("package org.structr.dynamic;\n\n");

		SchemaHelper.formatImportStatements(src, baseType);

		String superClass = _extendsClass != null ? _extendsClass : baseType.getSimpleName();

		src.append("public class ").append(_className).append(" extends ").append(superClass).append(" {\n\n");

		// output related node definitions, collect property views
		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {

			final String propertyName = outRel.getPropertyName(_className, existingPropertyNames, true);

			//outRel.setProperty(SchemaRelationship.targetJsonName, propertyName);

			src.append(outRel.getPropertySource(propertyName, true));
			//existingPropertyNames.clear();
			addPropertyNameToViews(propertyName, viewProperties);

		}

		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {

			final String propertyName = inRel.getPropertyName(_className, existingPropertyNames, false);

			//inRel.setProperty(SchemaRelationship.sourceJsonName, propertyName);

			src.append(inRel.getPropertySource(propertyName, false));
			//existingPropertyNames.clear();
			addPropertyNameToViews(propertyName, viewProperties);

		}

		// extract properties from node
		src.append(SchemaHelper.extractProperties(this, propertyNames, validators, enums, viewProperties, saveActions, errorBuffer));

		// output possible enum definitions
		for (final String enumDefition : enums) {
			src.append(enumDefition);
		}

		for (Entry<String, Set<String>> entry : viewProperties.entrySet()) {

			final String viewName  = entry.getKey();
			final Set<String> view = entry.getValue();

			if (!view.isEmpty()) {
				dynamicViews.add(viewName);
				SchemaHelper.formatView(src, _className, viewName, viewName, view);
			}
		}

		if (getProperty(defaultSortKey) != null) {

			String order = getProperty(defaultSortOrder);
			if (order == null || "desc".equals(order)) {
				order = "GraphObjectComparator.DESCENDING";
			} else {
				order = "GraphObjectComparator.ASCENDING";
			}

			src.append("\n\t@Override\n");
			src.append("\tpublic PropertyKey getDefaultSortKey() {\n");
			src.append("\t\treturn ").append(getProperty(defaultSortKey)).append("Property;\n");
			src.append("\t}\n");

			src.append("\n\t@Override\n");
			src.append("\tpublic String getDefaultSortOrder() {\n");
			src.append("\t\treturn ").append(order).append(";\n");
			src.append("\t}\n");
		}

		SchemaHelper.formatValidators(src, validators);
		SchemaHelper.formatSaveActions(src, saveActions);

		src.append("}\n");

		return src.toString();
	}

	@Override
	public Set<String> getViews() {
		return dynamicViews;
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		return ValidationHelper.checkStringMatchesRegex(this, name, "[A-Z][a-zA-Z0-9_]+", errorBuffer);

	}

	@Override
	public String getMultiplicity(final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();
		final String _className                 = getProperty(name);

		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {

			if (propertyNameToCheck.equals(outRel.getPropertyName(_className, existingPropertyNames, true))) {
				return outRel.getMultiplicity(true);
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {

			if (propertyNameToCheck.equals(inRel.getPropertyName(_className, existingPropertyNames, false))) {
				return inRel.getMultiplicity(false);
			}
		}

		// fallback, search NodeInterface (this allows the owner relationship to be used in Notions!)
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(NodeInterface.class, propertyNameToCheck, false);
		if (key != null) {

			
			// return "extended" multiplicity when the falling back to a NodeInterface property
			// to signal the code generator that it must not append "Property" to the name of
			// the generated NotionProperty parameter, i.e. NotionProperty(owner, ...) instead
			// of NotionProperty(ownerProperty, ...)..

			if (key instanceof StartNode || key instanceof EndNode) {
				return "1X";
			}

			if (key instanceof StartNodes || key instanceof EndNodes) {
				return "*X";
			}
		}

		return null;
	}

	@Override
	public String getRelatedType(final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();
		final String _className                 = getProperty(name);

		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {

			if (propertyNameToCheck.equals(outRel.getPropertyName(_className, existingPropertyNames, true))) {
				return outRel.getSchemaNodeTargetType();
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {

			if (propertyNameToCheck.equals(inRel.getPropertyName(_className, existingPropertyNames, false))) {
				return inRel.getSchemaNodeSourceType();
			}
		}

		// fallback, search NodeInterface (this allows the owner relationship to be used in Notions!)
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(NodeInterface.class, propertyNameToCheck, false);
		if (key != null) {

			final Class relatedType = key.relatedType();
			if (relatedType != null) {

				return relatedType.getSimpleName();
			}
		}

		return null;
	}

	@Override
	public String getAuxiliarySource() throws FrameworkException {

		// only File needs to return auxiliary code!
		if (!"File".equals(getClassName())) {
			return null;
		}

		final Map<Actions.Type, List<ActionEntry>> saveActions = new EnumMap<>(Actions.Type.class);
		final Map<String, Set<String>> viewProperties          = new LinkedHashMap<>();
		final Set<String> propertyNames                        = new LinkedHashSet<>();
		final Set<Validator> validators                        = new LinkedHashSet<>();
		final Set<String> enums                                = new LinkedHashSet<>();
		final String _className                                = getProperty(name);
		final ErrorBuffer dummyErrorBuffer                     = new ErrorBuffer();

		// extract properties
		final String propertyDefinitions = SchemaHelper.extractProperties(this, propertyNames, validators, enums, viewProperties, saveActions, dummyErrorBuffer);

		if (!propertyNames.isEmpty() || validators.isEmpty() || !saveActions.isEmpty()) {

			final StringBuilder src = new StringBuilder();

			src.append("package org.structr.dynamic;\n\n");

			SchemaHelper.formatImportStatements(src, AbstractNode.class);

			src.append("public class _").append(_className).append("Helper {\n");

			// output possible enum definitions
			for (final String enumDefition : enums) {
				src.append(enumDefition);
			}

			// formatting is important :)
			if (!enums.isEmpty()) {
				src.append("\n");
			}

			if (!propertyNames.isEmpty()) {

				src.append("\n");

				// extract properties from node
				src.append(propertyDefinitions);

				src.append("\n\tstatic {\n\n");

				for (final String propertyName : propertyNames) {

					src.append("\t\t").append(propertyName).append(".setDeclaringClass(").append(_className).append(".class);\n\n");
					src.append("\t\tStructrApp.getConfiguration().registerDynamicProperty(").append(_className).append(".class, ").append(propertyName).append(");\n");
					src.append("\t\tStructrApp.getConfiguration().registerPropertySet(").append(_className).append(".class, PropertyView.Ui, ").append(propertyName).append(");\n\n");

				}

				src.append("\t}\n\n");
			}

			SchemaHelper.formatDynamicValidators(src, validators);
			SchemaHelper.formatDynamicSaveActions(src, saveActions);

			src.append("}\n");

			return src.toString();
		}

		return null;
	}

	// ----- private methods -----
	private void addPropertyNameToViews(final String propertyName, final Map<String, Set<String>> viewProperties) {
		//SchemaHelper.addPropertyToView(PropertyView.Public, propertyName, viewProperties);
		SchemaHelper.addPropertyToView(PropertyView.Ui, propertyName, viewProperties);
	}

	// ----- interface GraphObject -----
	@Override
	public List<GraphObject> getSyncData() {

		final List<GraphObject> data = super.getSyncData();

		// outgoing relationships
		for (final SchemaRelationship rel : getOutgoingRelationships(SchemaRelationship.class)) {
			data.add(rel);
		}

		// incoming relationships
		for (final SchemaRelationship rel : getIncomingRelationships(SchemaRelationship.class)) {
			data.add(rel);
		}

		return data;
	}
}
