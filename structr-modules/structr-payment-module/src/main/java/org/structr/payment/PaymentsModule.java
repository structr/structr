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
package org.structr.payment;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;


public class PaymentsModule implements StructrModule {

	private static final Logger logger = LoggerFactory.getLogger(PaymentsModule.class.getName());

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		// read configuration..
		checkString("paypal.mode",      Settings.getOrCreateStringSetting("paypal", "mode").getValue(),      "paypal.mode not set, please set to either sandbox or live.");
		checkString("paypal.username",  Settings.getOrCreateStringSetting("paypal", "username").getValue(),  "paypal.username not set in structr.conf.");
		checkString("paypal.password",  Settings.getOrCreateStringSetting("paypal", "password").getValue(),  "paypal.password not set in structr.conf.");
		checkString("paypal.signature", Settings.getOrCreateStringSetting("paypal", "signature").getValue(), "paypal.signature not set in structr.conf.");
		checkString("paypal.redirect",  Settings.getOrCreateStringSetting("paypal", "redirect").getValue(),  "paypal.redirect not set in structr.conf.");
		checkString("stripe.apikey",    Settings.getOrCreateStringSetting("stripe", "apikey").getValue(),    "stripe.apikey not set in structr.conf.");
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "payments";
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
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	// ----- private methods -----
	private void checkString(final String key, final String value, final String message) {

		if (StringUtils.isEmpty(value)) {

			// don't warn for empty values
			//logger.warn(message);

		} else {

			logger.info("{}: {}", new Object[] { key, value } );
		}
	}
}
