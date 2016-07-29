/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.impl.paypal;

import org.structr.common.error.SemanticErrorToken;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class PayPalErrorToken extends SemanticErrorToken {

	public PayPalErrorToken(final String type, final PropertyKey propertyKey, final String token, final Object detail) {
		super(type, propertyKey, token, detail);
	}
}
