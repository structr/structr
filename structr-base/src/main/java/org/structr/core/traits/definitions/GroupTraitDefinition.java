/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.core.traits.wrappers.GroupTraitWrapper;
import org.structr.schema.action.EvaluationHints;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 */
public final class GroupTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String MEMBERS_PROPERTY           = "members";
	public static final String JWKS_REFERENCE_ID_PROPERTY = "jwksReferenceId";
	public static final String IS_GROUP_PROPERTY          = "isGroup";


	public GroupTraitDefinition() {
		super(StructrTraits.GROUP);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					boolean valid = true;

					final Traits traits                       = obj.getTraits();
					final PropertyKey nameProperty            = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
					final PropertyKey jwksReferenceIdProperty = traits.key(JWKS_REFERENCE_ID_PROPERTY);

					valid &= ValidationHelper.isValidPropertyNotNull(obj, nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj,  nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj,  jwksReferenceIdProperty, errorBuffer);

					// check for circular group hierarchy
					valid &= GroupTraitDefinition.doesNotContainCircles( (NodeInterface) obj, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			SetProperty.class,
			new SetProperty() {

				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					graphObject.clearCaches();

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		final Set<AbstractMethod> methods = new LinkedHashSet<>();

		methods.add(new JavaMethod("addMember", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

				if (entity.is(StructrTraits.GROUP)) {

					final NodeInterface userNode = (NodeInterface)arguments.get(0);
					Principal user               = null;

					if (userNode != null) {
						user = userNode.as(Principal.class);
					}

					entity.as(Group.class).addMember(securityContext, user);
				}

				return null;
			}
		});

		methods.add(new JavaMethod("getMembers", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

				if (entity.is(StructrTraits.GROUP)) {

					return entity.as(Group.class).getMembers();
				}

				return null;
			}
		});

		methods.add(new JavaMethod("removeMember", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

				if (entity.is(StructrTraits.GROUP)) {

					final NodeInterface userNode = (NodeInterface)arguments.get(0);
					Principal user               = null;

					if (userNode != null) {
						user = userNode.as(Principal.class);
					}

					entity.as(Group.class).removeMember(securityContext, user);
				}

				return null;
			}
		});

		return methods;
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Group.class, (traits, node) -> new GroupTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> membersProperty = new EndNodes(MEMBERS_PROPERTY, StructrTraits.GROUP_CONTAINS_PRINCIPAL);
		final Property<String> jwksReferenceIdProperty          = new StringProperty(JWKS_REFERENCE_ID_PROPERTY).indexed().unique();
		final Property<String> nameProperty                     = new StringProperty(NodeInterfaceTraitDefinition.NAME_PROPERTY).indexed().notNull().unique();
		final Property<Boolean> isGroupProperty                 = new ConstantBooleanProperty(IS_GROUP_PROPERTY, true);

		return newSet(
			membersProperty,
			jwksReferenceIdProperty,
			nameProperty,
			isGroupProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				NodeInterfaceTraitDefinition.NAME_PROPERTY, IS_GROUP_PROPERTY, MEMBERS_PROPERTY, PrincipalTraitDefinition.BLOCKED_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					IS_GROUP_PROPERTY, JWKS_REFERENCE_ID_PROPERTY, MEMBERS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	@Override
	public boolean shouldSkipSecurityRelationships() {
		return isAdmin();
	}
	*/

	// ----- public static methods -----
	public static boolean doesNotContainCircles(final NodeInterface group, final ErrorBuffer errorBuffer) {

		try {
			recursiveCollectParentUuids(group, new LinkedHashSet<>(), errorBuffer);
			recursiveCollectChildrenUuids(group, new LinkedHashSet<>(), errorBuffer);

		} catch (RuntimeException r) {

			return false;
		}

		return true;
	}

	private static void recursiveCollectParentUuids(final NodeInterface node, final Set<String> uuids, final ErrorBuffer errorBuffer) {

		final Principal principal = node.as(Principal.class);
		final String uuid         = principal.getUuid();

		// only recurse if the set did not already contain the current node
		if (uuids.add(uuid)) {

			for (final Group parent : principal.getParentsPrivileged()) {

				recursiveCollectParentUuids(parent, uuids, errorBuffer);
			}

		} else {

			errorBuffer.getErrorTokens().add(new SemanticErrorToken(StructrTraits.GROUP, PrincipalTraitDefinition.GROUPS_PROPERTY, "circular_reference"));

			throw new RuntimeException("Abort");
		}
	}

	private static void recursiveCollectChildrenUuids(final NodeInterface node, final Set<String> uuids, final ErrorBuffer errorBuffer) {

		final Group group = node.as(Group.class);
		final String uuid = group.getUuid();

		// only recurse if the set did not already contain the current node
		if (uuids.add(uuid)) {

			for (final Principal member : group.getMembers()) {

				if (member.is(StructrTraits.GROUP)) {

					recursiveCollectChildrenUuids(member.as(Group.class), uuids, errorBuffer);
				}
			}

		} else {

			errorBuffer.getErrorTokens().add(new SemanticErrorToken(StructrTraits.GROUP, GroupTraitDefinition.MEMBERS_PROPERTY, "circular_reference"));

			throw new RuntimeException("Abort");
		}
	}
}
