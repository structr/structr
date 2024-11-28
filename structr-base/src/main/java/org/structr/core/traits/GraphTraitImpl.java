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
package org.structr.core.traits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.*;

public abstract class GraphTraitImpl implements GraphObjectTrait {

	private static final Logger logger = LoggerFactory.getLogger(GraphObjectTrait.class);

	private static final String SYSTEM_CATEGORY     = "System";
	private static final String VISIBILITY_CATEGORY = "Visibility";

	protected final PropertyKey<String> typeProperty;
	protected final PropertyKey<String> idProperty;
	protected boolean internalSystemPropertiesUnlocked;
	protected boolean readOnlyPropertiesUnlocked;
	protected SecurityContext securityContext;
	protected PropertyContainer obj;
	protected Traits traits;

	static {

		/*
		final Trait trait = Trait.create(GraphObjectTrait.class, n -> new GraphTraitImpl(n) {});

		trait.registerProperty(new StringProperty("base").partOfBuiltInSchema());
		trait.registerProperty(new TypeProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
		trait.registerProperty(new UuidProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
		trait.registerProperty(new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("createdBy").readOnly().writeOnce().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());

		 */
	}

	protected GraphTraitImpl(final PropertyContainer obj) {

		this.obj = obj;

		traits = Traits.of(obj.getType());

		typeProperty = traits.get("GraphTrait").key("type");
		idProperty   = traits.get("GraphTrait").key("id");

	}

	protected Trait as(final String type) {

		final Trait trait = Trait.of(type);
		if (trait != null) {

			return trait.getImplementation(obj);
		}

		// should throw exception here
		return null;
	}

	protected <T> PropertyKey<T> key(final String name) {
		return traits.key(name);
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = true;

		// the following two checks can be omitted in release 2.4 when Neo4j uniqueness constraints are live
		valid &= ValidationHelper.isValidStringNotBlank(this, idProperty, errorBuffer);

		if (securityContext != null && securityContext.uuidWasSetManually()) {
			valid &= ValidationHelper.isValidGloballyUniqueProperty(this, idProperty, errorBuffer);
		}

		valid &= ValidationHelper.isValidUuid(this, idProperty, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, typeProperty, errorBuffer);

		return valid;

	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
	}

	@Override
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void afterCreation(final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterModification(final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {
	}

	@Override
	public void ownerModified(final SecurityContext securityContext) {
	}

	@Override
	public void securityModified(final SecurityContext securityContext) {
	}

	@Override
	public void locationModified(final SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(final SecurityContext securityContext) {
	}
}
