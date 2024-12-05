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
package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.property.*;
import org.structr.core.traits.AbstractTraitDefinition;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.AfterModification;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.wrappers.ResourceAccessTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Controls access to REST resources.
 *
 * Objects of this class act as a doorkeeper for REST resources
 * that match the signature string in the 'signature' field.
 * <p>
 * A ResourceAccess object defines access permissions granted
 * <ul>
 * <li>to everyone (public)
 * <li>to authenticated principals
 * <li>to individual principals (when connected to a {link @Principal} node
 * </ul>
 *
 * <p>'flags' is a sum of any combination of the following values:
 *
 *  FORBIDDEN             = 0
 *  AUTH_USER_GET         = 1
 *  AUTH_USER_PUT         = 2
 *  AUTH_USER_POST        = 4
 *  AUTH_USER_DELETE      = 8
 *  NON_AUTH_USER_GET     = 16
 *  NON_AUTH_USER_PUT     = 32
 *  NON_AUTH_USER_POST    = 64
 *  NON_AUTH_USER_DELETE  = 128
 *  AUTH_USER_OPTIONS     = 256
 *  NON_AUTH_USER_OPTIONS = 512
 *
 *
 *
 */
public class ResourceAccessDefinition extends AbstractTraitDefinition {

	private static final Property<String>               signature          = new StringProperty("signature").indexed().partOfBuiltInSchema();
	private static final Property<Long>                 flags              = new LongProperty("flags").indexed().partOfBuiltInSchema();
	private static final Property<Boolean>              isResourceAccess   = new ConstantBooleanProperty("isResourceAccess", true).partOfBuiltInSchema();

	/*
	public static final View uiView = new View(ResourceAccess.class, PropertyView.Ui,
		signature, flags, isResourceAccess
	);

	public static final View publicView = new View(ResourceAccess.class, PropertyView.Public,
		signature, flags, isResourceAccess
	);
	*/

	public ResourceAccessDefinition(final String name) {
		super(name);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					boolean valid = true;

					valid &= ValidationHelper.isValidStringNotBlank(obj, ResourceAccessDefinition.signature, errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, ResourceAccessDefinition.flags, errorBuffer);

					return valid;
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
					ResourceAccessTraitWrapper.clearCache();
				}
			},

			AfterCreation.class,
			new AfterCreation() {

				@Override
				public void afterCreation(GraphObject graphObject, SecurityContext securityContext) throws FrameworkException {
					ResourceAccessTraitWrapper.clearCache();
				}
			},

			AfterModification.class,
			new AfterModification() {

				@Override
				public void afterModification(GraphObject graphObject, SecurityContext securityContext) throws FrameworkException {
					ResourceAccessTraitWrapper.clearCache();
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ResourceAccess.class, (traits, node) -> new ResourceAccessTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			signature,
			flags,
			isResourceAccess
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
