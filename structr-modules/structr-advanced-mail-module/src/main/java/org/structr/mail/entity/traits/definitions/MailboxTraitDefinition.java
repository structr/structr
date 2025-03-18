/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.mail.entity.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.mail.entity.Mailbox;
import org.structr.mail.entity.traits.wrappers.MailboxTraitWrapper;
import org.structr.schema.action.EvaluationHints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MailboxTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EMAILS_PROPERTY                      = "emails";
	public static final String HOST_PROPERTY                        = "host";
	public static final String USER_PROPERTY                        = "user";
	public static final String OVERRIDE_MAIL_ENTITY_TYPE_PROPERTY   = "overrideMailEntityType";
	public static final String PASSWORD_PROPERTY                    = "password";
	public static final String FOLDERS_PROPERTY                     = "folders";
	public static final String MAIL_PROTOCOL_PROPERTY               = "mailProtocol";
	public static final String PORT_PROPERTY                        = "port";
	public static final String AVAILABLE_FOLDERS_ON_SERVER_PROPERTY = "availableFoldersOnServer";

	public MailboxTraitDefinition() {
		super(StructrTraits.MAILBOX);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();
					final Mailbox node = obj.as(Mailbox.class);
					boolean valid      = true;

					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key(FOLDERS_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key(HOST_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key(USER_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key(MAIL_PROTOCOL_PROPERTY), errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(
			new JavaMethod("getAvailableFoldersOnServer", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return getAvailableFoldersOnServer(securityContext, entity.as(Mailbox.class));
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> emailsProperty  = new EndNodes(EMAILS_PROPERTY, StructrTraits.MAILBOX_CONTAINS_EMAIL_MESSAGES_EMAIL_MESSAGE);
		final Property<String> hostProperty                     = new StringProperty(HOST_PROPERTY).indexed().notNull();
		final Property<String> userProperty                     = new StringProperty(USER_PROPERTY).indexed().notNull();
		final Property<String> overrideMailEntityTypeProperty   = new StringProperty(OVERRIDE_MAIL_ENTITY_TYPE_PROPERTY).indexed();
		final Property<String> passwordProperty                 = new EncryptedStringProperty(PASSWORD_PROPERTY).indexed();
		final Property<String[]> foldersProperty                = new ArrayProperty(FOLDERS_PROPERTY, String.class).indexed();
		final Property<String> mailProtocolProperty             = new EnumProperty(MAIL_PROTOCOL_PROPERTY, Set.of("pop3", "imaps")).indexed().notNull();
		final Property<Integer> portProperty                    = new IntProperty(PORT_PROPERTY).indexed();
		final Property<Object> availableFoldersOnServerProperty = new FunctionProperty(AVAILABLE_FOLDERS_ON_SERVER_PROPERTY).readFunction("{return Structr.this.getAvailableFoldersOnServer()}");

		return newSet(
			emailsProperty,
			hostProperty,
			userProperty,
			overrideMailEntityTypeProperty,
			passwordProperty,
			foldersProperty,
			mailProtocolProperty,
			portProperty,
			availableFoldersOnServerProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY),

			PropertyView.Ui,
			newSet(HOST_PROPERTY, USER_PROPERTY, OVERRIDE_MAIL_ENTITY_TYPE_PROPERTY, PASSWORD_PROPERTY, FOLDERS_PROPERTY, MAIL_PROTOCOL_PROPERTY, PORT_PROPERTY, AVAILABLE_FOLDERS_ON_SERVER_PROPERTY)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Mailbox.class, (traits, node) -> new MailboxTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public List<String> getAvailableFoldersOnServer(final SecurityContext securityContext, final Mailbox mailbox) {

		final Iterable<String> result = StructrApp.getInstance(securityContext).command(org.structr.mail.service.FetchFoldersCommand.class).execute(mailbox);
		if (result != null) {

			return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());

		} else {

			return new ArrayList<>();
		}
	}
}
