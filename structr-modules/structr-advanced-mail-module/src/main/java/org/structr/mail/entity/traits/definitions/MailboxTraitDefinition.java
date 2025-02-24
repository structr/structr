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

					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key("folders"), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key("host"), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key("user"), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(node, traits.key("mailProtocol"), errorBuffer);

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

		final Property<Iterable<NodeInterface>> emailsProperty  = new EndNodes("emails", "MailboxCONTAINS_EMAILMESSAGESEMailMessage");
		final Property<String> hostProperty                     = new StringProperty("host").indexed().notNull();
		final Property<String> userProperty                     = new StringProperty("user").indexed().notNull();
		final Property<String> overrideMailEntityTypeProperty   = new StringProperty("overrideMailEntityType").indexed();
		final Property<String> passwordProperty                 = new EncryptedStringProperty("password").indexed();
		final Property<String[]> foldersProperty                = new ArrayProperty("folders", String.class).indexed();
		final Property<String> mailProtocolProperty             = new EnumProperty("mailProtocol", Set.of("pop3", "imaps")).indexed().notNull();
		final Property<Integer> portProperty                    = new IntProperty("port").indexed();
		final Property<Object> availableFoldersOnServerProperty = new FunctionProperty("availableFoldersOnServer").readFunction("{return Structr.this.getAvailableFoldersOnServer()}");

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
			newSet("id", "type", "name"),
			PropertyView.Ui,
			newSet("host", "user", "overrideMailEntityType", "password", "folders", "mailProtocol", "port", "availableFoldersOnServer")
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
