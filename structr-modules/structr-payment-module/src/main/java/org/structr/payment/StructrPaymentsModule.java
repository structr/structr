/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;


public class StructrPaymentsModule implements StructrModule {

	private static final Logger logger = LoggerFactory.getLogger(StructrPaymentsModule.class.getName());

	@Override
	public void onLoad() {

		logger.info("Checking payment provider configuration..");

		// check and read configuration..
		checkString("paypal.mode",      StructrApp.getConfigurationValue("paypal.mode"),      "paypal.mode not set, please set to either sandbox or live.");
		checkString("paypal.username",  StructrApp.getConfigurationValue("paypal.username"),  "paypal.username not set in structr.conf.");
		checkString("paypal.password",  StructrApp.getConfigurationValue("paypal.password"),  "paypal.password not set in structr.conf.");
		checkString("paypal.signature", StructrApp.getConfigurationValue("paypal.signature"), "paypal.signature not set in structr.conf.");
		checkString("paypal.redirect",  StructrApp.getConfigurationValue("paypal.redirect"),  "paypal.redirect not set in structr.conf.");
		checkString("stripe.apikey",    StructrApp.getConfigurationValue("stripe.apikey"),    "stripe.apikey not set in structr.conf.");
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	// ----- private methods -----
	private void checkString(final String key, final String value, final String message) {

		if (StringUtils.isEmpty(value)) {

			logger.warn("{}", message);

		} else {

			logger.info("{}: {}", new Object[] { key, value } );
		}
	}
}
