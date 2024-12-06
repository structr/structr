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
package org.structr.schema.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonGrant;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.structr.core.traits.Traits;
import org.structr.web.maintenance.DeployCommand;

/**
 *
 *
 */
public class StructrGrantDefinition implements JsonGrant, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrGrantDefinition.class.getName());

	private JsonType parent            = null;
	private NodeInterface schemaGrant  = null;
	private String principalName       = null;
	private boolean allowRead          = false;
	private boolean allowWrite         = false;
	private boolean allowDelete        = false;
	private boolean allowAccessControl = false;

	StructrGrantDefinition(final JsonType parent, final String principalName) {

		this.parent        = parent;
		this.principalName = principalName;
	}

	@Override
	public String toString() {
		return "StructrGrantDefinition(" + principalName + ", " + allowRead + ", " + allowWrite + ", " + allowDelete + ", " + allowAccessControl + ")";
	}

	@Override
	public int hashCode() {
		return principalName.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrGrantDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("grants/" + getPrincipalName());

			} catch (URISyntaxException urex) {
				logger.warn("", urex);
			}
		}

		return null;
	}

	@Override
	public JsonType getParent() {
		return parent;
	}

	@Override
	public String getPrincipalName() {
		return principalName;
	}

	@Override
	public boolean getAllowRead() {
		return allowRead;
	}

	@Override
	public boolean getAllowWrite() {
		return allowWrite;
	}

	@Override
	public boolean getAllowDelete() {
		return allowDelete;
	}

	@Override
	public boolean getAllowAccessControl() {
		return allowAccessControl;
	}

	@Override
	public int compareTo(final JsonGrant o) {
		return getPrincipalName().compareTo(o.getPrincipalName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	NodeInterface getSchemaGrant() {
		return schemaGrant;
	}

	NodeInterface createDatabaseSchema(final App app, final NodeInterface schemaNode) throws FrameworkException {

		final PropertyMap getOrCreateProperties = new PropertyMap();
		final PropertyMap updateProperties      = new PropertyMap();
		final Traits traits                     = Traits.of("SchemaGrant");
		final List<NodeInterface> principals    = app.nodeQuery("Principal").andName(principalName).getAsList();

		if (principals.isEmpty()) {

			// log error
			logger.warn("No node of type Principal with name '{}' found for schema grant, ignoring.", principalName);
			DeployCommand.addMissingPrincipal(principalName);
			return null;
		}

		if (principals.size() > 1) {

			// log error
			logger.warn("Found {} nodes of type Principal named '{}' for schema grant, ignoring.", principals.size(), principalName);
			DeployCommand.addAmbiguousPrincipal(principalName);
			return null;
		}

		getOrCreateProperties.put(traits.key("principal"),  principals.get(0));
		getOrCreateProperties.put(traits.key("schemaNode"), (SchemaNode)schemaNode);

		NodeInterface grant = app.nodeQuery("SchemaGrant").and(getOrCreateProperties).getFirst();
		if (grant == null) {

			grant = app.create("SchemaGrant", getOrCreateProperties);
		}

		updateProperties.put(traits.key("allowRead"),          getAllowRead());
		updateProperties.put(traits.key("allowWrite"),         getAllowWrite());
		updateProperties.put(traits.key("allowDelete"),        getAllowDelete());
		updateProperties.put(traits.key("allowAccessControl"), getAllowAccessControl());

		grant.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

		this.schemaGrant = grant;

		return grant;
	}


	void deserialize(final Map<String, Object> source) {

		final Object _grantRead = source.get(JsonSchema.KEY_GRANT_READ);
		if (_grantRead != null && _grantRead instanceof Boolean) {

			this.allowRead = (Boolean)_grantRead;
		}

		final Object _grantWrite = source.get(JsonSchema.KEY_GRANT_WRITE);
		if (_grantWrite != null && _grantWrite instanceof Boolean) {

			this.allowWrite = (Boolean)_grantWrite;
		}

		final Object _grantDelete = source.get(JsonSchema.KEY_GRANT_DELETE);
		if (_grantDelete != null && _grantDelete instanceof Boolean) {

			this.allowDelete = (Boolean)_grantDelete;
		}

		final Object _grantAccessControl = source.get(JsonSchema.KEY_GRANT_ACCESS_CONTROL);
		if (_grantAccessControl != null && _grantAccessControl instanceof Boolean) {

			this.allowAccessControl = (Boolean)_grantAccessControl;
		}
	}

	void deserialize(final NodeInterface node) {

		this.schemaGrant = node;

		final SchemaGrant grant = node.as(SchemaGrant.class);

		this.allowRead          = grant.allowRead();
		this.allowWrite         = grant.allowWrite();
		this.allowDelete        = grant.allowDelete();
		this.allowAccessControl = grant.allowAccessControl();
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map    = new TreeMap<>();

		map.put(JsonSchema.KEY_GRANT_READ,           allowRead);
		map.put(JsonSchema.KEY_GRANT_WRITE,          allowWrite);
		map.put(JsonSchema.KEY_GRANT_DELETE,         allowDelete);
		map.put(JsonSchema.KEY_GRANT_ACCESS_CONTROL, allowAccessControl);

		return map;
	}

	void initializeReferences() {
	}

	void diff(final StructrGrantDefinition other) {
	}

	// ----- static methods -----
	static StructrGrantDefinition deserialize(final StructrTypeDefinition parent, final String name, final Map<String, Object> source) {

		final StructrGrantDefinition newGrant = new StructrGrantDefinition(parent, name);

		newGrant.deserialize(source);

		return newGrant;
	}

	static StructrGrantDefinition deserialize(final StructrTypeDefinition parent, final NodeInterface grant) {

		final StructrGrantDefinition newGrant = new StructrGrantDefinition(parent, grant.getPrincipalName());

		newGrant.deserialize(grant);

		return newGrant;
	}
}
