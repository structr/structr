/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.parboiled.common.StringUtils;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;

/**
 *
 * @author Christian Morgner
 */
public class StructrPaymentsModule implements StructrModule {

	private static final Logger logger = Logger.getLogger(StructrPaymentsModule.class.getName());

	@Override
	public void onLoad() {

		logger.log(Level.INFO, "Checking payment provider configuration..");

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

			logger.log(Level.WARNING, "{0}", message);

		} else {

			logger.log(Level.INFO, "{0}: {1}", new Object[] { key, value } );
		}
	}
}
