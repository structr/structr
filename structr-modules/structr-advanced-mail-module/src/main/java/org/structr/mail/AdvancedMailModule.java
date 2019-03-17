/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.Set;
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.mail.function.MailAddAttachmentFunction;
import org.structr.mail.function.MailAddBccFunction;
import org.structr.mail.function.MailAddCcFunction;
import org.structr.mail.function.MailAddHeaderFunction;
import org.structr.mail.function.MailAddReplyToFunction;
import org.structr.mail.function.MailAddToFunction;
import org.structr.mail.function.MailBeginFunction;
import org.structr.mail.function.MailClearAttachmentsFunction;
import org.structr.mail.function.MailClearBccFunction;
import org.structr.mail.function.MailClearBounceAddressFunction;
import org.structr.mail.function.MailClearCcFunction;
import org.structr.mail.function.MailClearHeadersFunction;
import org.structr.mail.function.MailClearReplyToFunction;
import org.structr.mail.function.MailClearToFunction;
import org.structr.mail.function.MailSendFunction;
import org.structr.mail.function.MailSetBounceAddressFunction;
import org.structr.mail.function.MailSetFromFunction;
import org.structr.mail.function.MailSetHtmlContentFunction;
import org.structr.mail.function.MailSetSubjectFunction;
import org.structr.mail.function.MailSetTextContentFunction;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;


public class AdvancedMailModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new MailBeginFunction());
		Functions.put(licenseManager, new MailSetFromFunction());
		Functions.put(licenseManager, new MailSetSubjectFunction());
		Functions.put(licenseManager, new MailSetHtmlContentFunction());
		Functions.put(licenseManager, new MailSetTextContentFunction());
		Functions.put(licenseManager, new MailAddToFunction());
		Functions.put(licenseManager, new MailClearToFunction());
		Functions.put(licenseManager, new MailAddCcFunction());
		Functions.put(licenseManager, new MailClearCcFunction());
		Functions.put(licenseManager, new MailAddBccFunction());
		Functions.put(licenseManager, new MailClearBccFunction());
		Functions.put(licenseManager, new MailSetBounceAddressFunction());
		Functions.put(licenseManager, new MailClearBounceAddressFunction());
		Functions.put(licenseManager, new MailAddReplyToFunction());
		Functions.put(licenseManager, new MailClearReplyToFunction());
		Functions.put(licenseManager, new MailAddAttachmentFunction());
		Functions.put(licenseManager, new MailClearAttachmentsFunction());
		Functions.put(licenseManager, new MailAddHeaderFunction());
		Functions.put(licenseManager, new MailClearHeadersFunction());
		Functions.put(licenseManager, new MailSendFunction());
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}
}
