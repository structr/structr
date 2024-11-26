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
package org.structr.core.graph;


import org.structr.api.graph.Identity;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.RelationshipTrait;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

/**
 * A factory for structr relationships. This class exists because we need a fast
 * way to instantiate and initialize structr relationships, as this is the most-
 * used operation.
 *
 * @param <T>
 */
public class RelationshipFactory<T extends RelationshipTrait> extends Factory<Relationship, T> {

	public RelationshipFactory(final SecurityContext securityContext) {
		super(securityContext);
	}

	public RelationshipFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {
		super(securityContext, includeHidden, publicOnly);
	}

	public RelationshipFactory(final SecurityContext securityContext, final int pageSize, final int page) {
		super(securityContext, pageSize, page);
	}

	public RelationshipFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		super(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public T instantiate(final Relationship relationship) {
		return instantiate(relationship, null);
	}

	@Override
	public T instantiate(final Relationship relationship, final Identity pathSegmentId) {

		if (relationship == null || TransactionCommand.isDeleted(relationship) || relationship.isDeleted()) {
			return null;
		}

		return (T) instantiate(relationship, pathSegmentId, false);
	}

	@Override
	public T instantiate(final Relationship relationship, final Identity pathSegmentId, final boolean isCreation) {

		final Trait<RelationshipTrait> trait = Trait.of(RelationshipTrait.class);
		final RelationshipTrait base         = trait.getImplementation(relationship);

		final Traits traits      = base.getTraits();
		final Trait<T> typeTrait = (Trait<T>) traits.get(base.getType());

		return typeTrait.getImplementation(relationship);
	}

	@Override
	public T adapt(final Relationship relationship) {
		return instantiate(relationship);
	}

	@Override
	public T instantiate(final Relationship obj, final boolean includeHidden, final boolean publicOnly) throws FrameworkException {

		this.includeHidden = includeHidden;
		this.publicOnly    = publicOnly;

		return instantiate(obj);
	}
}
