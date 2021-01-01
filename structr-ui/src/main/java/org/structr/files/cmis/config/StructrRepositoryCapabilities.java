/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.files.cmis.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.chemistry.opencmis.commons.data.CreatablePropertyTypes;
import org.apache.chemistry.opencmis.commons.data.NewTypeSettableAttributes;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityOrderBy;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cmis.common.CMISExtensionsData;

/**
 *
 *
 */
public class StructrRepositoryCapabilities extends CMISExtensionsData implements RepositoryCapabilities, CreatablePropertyTypes, NewTypeSettableAttributes {

	private static final Logger logger = LoggerFactory.getLogger(StructrRepositoryCapabilities.class.getName());

	// ----- interface RepositoryCapabilities -----
	@Override
	public CapabilityContentStreamUpdates getContentStreamUpdatesCapability() {
		return CapabilityContentStreamUpdates.NONE;
	}

	@Override
	public CapabilityChanges getChangesCapability() {
		return CapabilityChanges.NONE;
	}

	@Override
	public CapabilityRenditions getRenditionsCapability() {
		return CapabilityRenditions.NONE;
	}

	@Override
	public Boolean isGetDescendantsSupported() {
		return true;
	}

	@Override
	public Boolean isGetFolderTreeSupported() {
		return true;
	}

	@Override
	public CapabilityOrderBy getOrderByCapability() {
		return CapabilityOrderBy.COMMON;
	}

	@Override
	public Boolean isMultifilingSupported() {
		return false;
	}

	@Override
	public Boolean isUnfilingSupported() {
		return false;
	}

	@Override
	public Boolean isVersionSpecificFilingSupported() {
		return false;
	}

	@Override
	public Boolean isPwcSearchableSupported() {
		return false;
	}

	@Override
	public Boolean isPwcUpdatableSupported() {
		return false;
	}

	@Override
	public Boolean isAllVersionsSearchableSupported() {
		return false;
	}

	@Override
	public CapabilityQuery getQueryCapability() {
		return CapabilityQuery.METADATAONLY;
	}

	@Override
	public CapabilityJoin getJoinCapability() {
		return CapabilityJoin.NONE;
	}

	@Override
	public CapabilityAcl getAclCapability() {
		return CapabilityAcl.MANAGE;
	}

	@Override
	public CreatablePropertyTypes getCreatablePropertyTypes() {
		return this;
	}

	@Override
	public NewTypeSettableAttributes getNewTypeSettableAttributes() {
		return this;
	}

	// ----- interface CreatablePropertyTypes -----
	@Override
	public Set<PropertyType> canCreate() {

		final Set<PropertyType> properties = new LinkedHashSet<>();

		properties.add(PropertyType.BOOLEAN);
		properties.add(PropertyType.DATETIME);
		properties.add(PropertyType.DECIMAL);
		properties.add(PropertyType.HTML);
		properties.add(PropertyType.ID);
		properties.add(PropertyType.INTEGER);
		properties.add(PropertyType.STRING);
		properties.add(PropertyType.URI);

		return properties;
	}

	// ----- interface NewTypeSettableAttributes -----
	@Override
	public Boolean canSetId() {
		return true;
	}

	@Override
	public Boolean canSetLocalName() {
		return true;
	}

	@Override
	public Boolean canSetLocalNamespace() {
		return true;
	}

	@Override
	public Boolean canSetDisplayName() {
		return true;
	}

	@Override
	public Boolean canSetQueryName() {
		return true;
	}

	@Override
	public Boolean canSetDescription() {
		return true;
	}

	@Override
	public Boolean canSetCreatable() {
		return true;
	}

	@Override
	public Boolean canSetFileable() {
		return true;
	}

	@Override
	public Boolean canSetQueryable() {
		return true;
	}

	@Override
	public Boolean canSetFulltextIndexed() {
		return true;
	}

	@Override
	public Boolean canSetIncludedInSupertypeQuery() {
		return true;
	}

	@Override
	public Boolean canSetControllablePolicy() {
		return true;
	}

	@Override
	public Boolean canSetControllableAcl() {
		return true;
	}
}
