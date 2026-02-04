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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.LifecycleMethodAdapter;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.core.traits.operations.nodeinterface.OnNodeCreation;

import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaMethodTraitWrapper extends AbstractNodeTraitWrapper implements SchemaMethod {

	public SchemaMethodTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public AbstractSchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY));
		if (node != null) {

			return node.as(AbstractSchemaNode.class);
		}

		return null;
	}

	@Override
	public Iterable<SchemaMethodParameter> getParameters() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY);

		return Iterables.map(n -> n.as(SchemaMethodParameter.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getStaticSchemaNodeName() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY));
	}

	@Override
	public String getName() {
		return wrappedObject.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY));
	}

	@Override
	public String getSummary() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.SUMMARY_PROPERTY));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public String getCodeType() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.CODE_TYPE_PROPERTY));
	}

	@Override
	public String getReturnType() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.RETURN_TYPE_PROPERTY));
	}

	@Override
	public String getOpenAPIReturnType() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.OPEN_API_RETURN_TYPE_PROPERTY));
	}

	@Override
	public String getVirtualFileName() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.VIRTUAL_FILE_NAME_PROPERTY));
	}

	@Override
	public String[] getExceptions() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.EXCEPTIONS_PROPERTY));
	}

	@Override
	public String[] getTags() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.TAGS_PROPERTY));
	}

	@Override
	public void setSource(final String source) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source);
	}

	@Override
	public boolean callSuper() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.CALL_SUPER_PROPERTY));
	}

	@Override
	public boolean overridesExisting() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.OVERRIDES_EXISTING_PROPERTY));
	}

	@Override
	public boolean doExport() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.DO_EXPORT_PROPERTY));
	}

	@Override
	public boolean includeInOpenAPI() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY));
	}

	@Override
	public boolean isStaticMethod() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
	}

	@Override
	public boolean isPrivateMethod() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));
	}

	@Override
	public boolean returnRawResult() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.RETURN_RAW_RESULT_PROPERTY));
	}

	@Override
	public String getHttpVerb() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY));
	}

	public SchemaMethodParameter getSchemaMethodParameter(final String name) {

		for (final SchemaMethodParameter param : getParameters()) {

			if (name.equals(param.getName())) {
				return param;
			}
		}

		return null;
	}

	@Override
	public boolean wrapJsInMain() {
		return wrappedObject.getProperty(traits.key(SchemaMethodTraitDefinition.WRAP_JS_IN_MAIN_PROPERTY));
	}

	@Override
	public boolean isJava() {
		return "java".equals(getCodeType());
	}

	@Override
	public LifecycleMethod asLifecycleMethod() {

		final Class<LifecycleMethod> type = getMethodType();
		if (type != null) {

			final String source = getSource();
			if (source != null) {

				return new LifecycleMethodAdapter(this);
			}
		}

		return null;
	}

	@Override
	public boolean isLifecycleMethod() {
		return getMethodType() != null;
	}

	@Override
	public Class<LifecycleMethod> getMethodType() {

		/**
		 * This method determines whether a method is a lifecycle method,
		 * which onDownload, onStructrLogin etc. are NOT, hence we ignore
		 * them here.
		 */

		final AbstractSchemaNode parent = getSchemaNode();
		final boolean hasParent         = (parent != null);
		final String methodName         = getName();

		if (hasParent) {

			if (parent.isServiceClass()) {

				// prevent service classes from having lifecycle methods
				return null;
			}

			final Map<String, Class<? extends LifecycleMethod>> typeBasedLifecycleMethods = new LinkedHashMap<>();

			typeBasedLifecycleMethods.put("onNodeCreation",    OnNodeCreation.class);
			typeBasedLifecycleMethods.put("onCreate",          OnCreation.class);
			typeBasedLifecycleMethods.put("onCreation",        OnCreation.class);  // FIXME: deprecate this
			typeBasedLifecycleMethods.put("afterCreate",       AfterCreation.class);
			typeBasedLifecycleMethods.put("afterCreation",     AfterCreation.class);  // FIXME: deprecate this
			typeBasedLifecycleMethods.put("onSave",            OnModification.class);
			typeBasedLifecycleMethods.put("onModification",    OnModification.class);  // FIXME: deprecate this
			typeBasedLifecycleMethods.put("afterSave",         AfterModification.class);
			typeBasedLifecycleMethods.put("afterModification", AfterModification.class);  // FIXME: deprecate this
			typeBasedLifecycleMethods.put("onDelete",          OnDeletion.class);
			typeBasedLifecycleMethods.put("afterDelete",       AfterDeletion.class);

			for (final Map.Entry<String, Class<? extends LifecycleMethod>> entry : typeBasedLifecycleMethods.entrySet()) {

				final String lifecycleMethodPrefix = entry.getKey();

				if (methodName.startsWith(lifecycleMethodPrefix)) {
					return (Class)entry.getValue();
				}
			}
		}

		return null;
	}
}
