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
package org.structr.mail;

import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.mail.entity.traits.definitions.EMailMessageTraitDefinition;
import org.structr.mail.entity.traits.definitions.MailboxTraitDefinition;
import org.structr.mail.entity.traits.definitions.relationship.EMailMessageHAS_ATTACHMENTFile;
import org.structr.mail.entity.traits.definitions.relationship.MailboxCONTAINS_EMAILMESSAGESEMailMessage;
import org.structr.mail.function.*;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;


public class AdvancedMailModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		StructrTraits.registerRelationshipType("EMailMessageHAS_ATTACHMENTFile", new EMailMessageHAS_ATTACHMENTFile());
		StructrTraits.registerRelationshipType("MailboxCONTAINS_EMAIL_MESSAGEEMailMessage", new MailboxCONTAINS_EMAILMESSAGESEMailMessage());

		StructrTraits.registerNodeType("EMailMessage", new EMailMessageTraitDefinition());
		StructrTraits.registerNodeType("Mailbox",      new MailboxTraitDefinition());

		Functions.put(licenseManager, new MailBeginFunction(this));
		Functions.put(licenseManager, new MailSetFromFunction(this));
		Functions.put(licenseManager, new MailSetSubjectFunction(this));
		Functions.put(licenseManager, new MailSetHtmlContentFunction(this));
		Functions.put(licenseManager, new MailSetTextContentFunction(this));
		Functions.put(licenseManager, new MailAddToFunction(this));
		Functions.put(licenseManager, new MailClearToFunction(this));
		Functions.put(licenseManager, new MailAddCcFunction(this));
		Functions.put(licenseManager, new MailClearCcFunction(this));
		Functions.put(licenseManager, new MailAddBccFunction(this));
		Functions.put(licenseManager, new MailClearBccFunction(this));
		Functions.put(licenseManager, new MailSetBounceAddressFunction(this));
		Functions.put(licenseManager, new MailClearBounceAddressFunction(this));
		Functions.put(licenseManager, new MailAddReplyToFunction(this));
		Functions.put(licenseManager, new MailClearReplyToFunction(this));
		Functions.put(licenseManager, new MailAddMimePartFunction(this));
		Functions.put(licenseManager, new MailAddAttachmentFunction(this));
		Functions.put(licenseManager, new MailClearMimePartsFunction(this));
		Functions.put(licenseManager, new MailClearAttachmentsFunction(this));
		Functions.put(licenseManager, new MailAddHeaderFunction(this));
		Functions.put(licenseManager, new MailRemoveHeaderFunction(this));
		Functions.put(licenseManager, new MailClearHeadersFunction(this));
		Functions.put(licenseManager, new MailSetInReplyTo(this));
		Functions.put(licenseManager, new MailClearInReplyTo(this));
		Functions.put(licenseManager, new MailSaveOutgoingMessageFunction(this));
		Functions.put(licenseManager, new MailGetLastOutgoingMessageFunction(this));
		Functions.put(licenseManager, new MailSendFunction(this));
		Functions.put(licenseManager, new MailDecodeTextFunction(this));
		Functions.put(licenseManager, new MailEncodeTextFunction(this));
		Functions.put(licenseManager, new MailSelectConfigFunction(this));
		Functions.put(licenseManager, new MailSetManualConfigFunction(this));
		Functions.put(licenseManager, new MailResetManualConfigFunction(this));
		Functions.put(licenseManager, new MailGetErrorFunction(this));
		Functions.put(licenseManager, new MailHasErrorFunction(this));
	}

	@Override
	public String getName() {
		return "advanced-mail";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}
}
