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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.LifecycleMethodAdapter;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.core.traits.operations.nodeinterface.OnNodeCreation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SchemaMethodTraitWrapper extends AbstractNodeTraitWrapper implements SchemaMethod {

	public SchemaMethodTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public AbstractSchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("schemaNode"));
		if (node != null) {

			return node.as(AbstractSchemaNode.class);
		}

		return null;
	}

	@Override
	public Iterable<SchemaMethodParameter> getParameters() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("parameters");

		return Iterables.map(n -> n.as(SchemaMethodParameter.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getName() {
		return wrappedObject.getProperty(traits.key("name"));
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key("source"));
	}

	@Override
	public String getSummary() {
		return wrappedObject.getProperty(traits.key("summary"));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	@Override
	public String getCodeType() {
		return wrappedObject.getProperty(traits.key("codeType"));
	}

	@Override
	public String getReturnType() {
		return wrappedObject.getProperty(traits.key("returnType"));
	}

	@Override
	public String getOpenAPIReturnType() {
		return wrappedObject.getProperty(traits.key("openAPIReturnType"));
	}

	@Override
	public String getVirtualFileName() {
		return wrappedObject.getProperty(traits.key("virtualFileName"));
	}

	@Override
	public String getSignature() {
		return wrappedObject.getProperty(traits.key("signature"));
	}

	@Override
	public String[] getExceptions() {
		return wrappedObject.getProperty(traits.key("exceptions"));
	}

	@Override
	public String[] getTags() {
		return wrappedObject.getProperty(traits.key("tags"));
	}

	@Override
	public boolean callSuper() {
		return wrappedObject.getProperty(traits.key("callSuper"));
	}

	@Override
	public boolean overridesExisting() {
		return wrappedObject.getProperty(traits.key("overridesExisting"));
	}

	@Override
	public boolean doExport() {
		return wrappedObject.getProperty(traits.key("doExport"));
	}

	@Override
	public boolean includeInOpenAPI() {
		return wrappedObject.getProperty(traits.key("includeInOpenAPI"));
	}

	@Override
	public boolean isStaticMethod() {
		return wrappedObject.getProperty(traits.key("isStatic"));
	}

	@Override
	public boolean isPrivateMethod() {
		return wrappedObject.getProperty(traits.key("isPrivate"));
	}

	@Override
	public boolean returnRawResult() {
		return wrappedObject.getProperty(traits.key("returnRawResult"));
	}

	@Override
	public String getHttpVerb() {
		return wrappedObject.getProperty(traits.key("httpVerb"));
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
	public boolean isJava() {
		return "java".equals(getCodeType());
	}

	@Override
	public LifecycleMethod asLifecycleMethod() {

		final Class<LifecycleMethod> type = getMethodType();
		if (type != null) {

			return new LifecycleMethodAdapter(getSource());
		}

		return null;
	}

	@Override
	public boolean isLifecycleMethod() {
		return getMethodType() != null;
	}

	@Override
	public Class<LifecycleMethod> getMethodType() {

		final AbstractSchemaNode parent = getSchemaNode();
		final boolean hasParent         = (parent != null);
		final String methodName         = getName();

		if (hasParent) {

			final Map<String, Class<? extends LifecycleMethod>> typeBasedLifecycleMethods = new LinkedHashMap<>();

			typeBasedLifecycleMethods.put("onNodeCreation",    OnNodeCreation.class);
			typeBasedLifecycleMethods.put("onCreate",          OnCreation.class);
			typeBasedLifecycleMethods.put("onCreation",        OnCreation.class);
			typeBasedLifecycleMethods.put("afterCreate",       AfterCreation.class);
			typeBasedLifecycleMethods.put("afterCreation",     AfterCreation.class);
			typeBasedLifecycleMethods.put("onSave",            OnModification.class);
			typeBasedLifecycleMethods.put("onModification",    OnModification.class);
			typeBasedLifecycleMethods.put("afterSave",         AfterModification.class);
			typeBasedLifecycleMethods.put("afterModification", AfterModification.class);
			typeBasedLifecycleMethods.put("onDelete",          OnDeletion.class);
			typeBasedLifecycleMethods.put("afterDelete",       AfterDeletion.class);

			final Set<String> fileLifecycleMethods                              = Set.of("onUpload", "onDownload");
			final Set<String> userLifecycleMethods                              = Set.of("onOAuthLogin");

			for (final Map.Entry<String, Class<? extends LifecycleMethod>> entry : typeBasedLifecycleMethods.entrySet()) {

				final String lifecycleMethodPrefix = entry.getKey();

				if (methodName.startsWith(lifecycleMethodPrefix)) {
					// type cast to get rid of
					return (Class)entry.getValue();
				}
			}

			boolean inheritsFromFile = false;
			boolean inheritsFromUser = false;

			if (Traits.exists(parent.getName())) {

				final Traits traits = Traits.of(parent.getName());

				inheritsFromFile = traits.contains("AbstractFile");
				inheritsFromUser = traits.contains("User");
			}

			if (inheritsFromFile) {

				if (fileLifecycleMethods.contains(methodName)) {
					// FIXME
					return null;
					//return OnUpload.class;
				}
			}

			if (inheritsFromUser) {

				if (userLifecycleMethods.contains(methodName)) {
					// FIXME
					return null;
					//return true;
				}
			}

		} else {

			final Set<String> globalLifecycleMethods = Set.of("onStructrLogin", "onStructrLogout", "onAcmeChallenge");

			if (globalLifecycleMethods.contains(methodName)) {
				// FIXME
				return null;
				//return true;
			}

		}

		return null;
	}
}
