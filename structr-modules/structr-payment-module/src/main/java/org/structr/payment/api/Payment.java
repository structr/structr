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
package org.structr.payment.api;

import org.structr.common.error.FrameworkException;

/**
 *
 */
public interface Payment {

	// ----- read-only methods -----
	Iterable<PaymentItem> getItems();
	String getCurrencyCode();
	int getTotal();

	String getToken();
	void setToken(final String token) throws FrameworkException;

	String getPaymentState();
	void setPaymentState(final String state) throws FrameworkException;

	public String getDescription();
	public void setDescription(final String description) throws FrameworkException;
	public String getBillingAddressName();
	public void setBillingAddressName(String billingAddressName) throws FrameworkException;
	public String getBillingAddressStreet1();
	public void setBillingAddressStreet1(String billingAddressStreet1) throws FrameworkException;
	public String getBillingAddressStreet2();
	public void setBillingAddressStreet2(String billingAddressStreet2) throws FrameworkException;
	public String getBillingAddressZip();
	public void setBillingAddressZip(String billingAddressZip) throws FrameworkException;
	public String getBillingAddressCity();
	public void setBillingAddressCity(String billingAddressCity) throws FrameworkException;
	public String getBillingAddressCountry();
	public void setBillingAddressCountry(String billingAddressCountry) throws FrameworkException;
	public String getPayer();
	public void setPayer(String payer) throws FrameworkException;
	public String getPayerBusiness();
	public void setPayerBusiness(String payerBusiness) throws FrameworkException;
	public String getPayerAddressName();
	public void setPayerAddressName(String payerAddressName) throws FrameworkException;
	public String getPayerAddressStreet1();
	public void setPayerAddressStreet1(String payerAddressStreet1) throws FrameworkException;
	public String getPayerAddressStreet2();
	public void setPayerAddressStreet2(String payerAddressStreet2) throws FrameworkException;
	public String getPayerAddressZip();
	public void setPayerAddressZip(String payerAddressZip) throws FrameworkException;
	public String getPayerAddressCity();
	public void setPayerAddressCity(String payerAddressCity) throws FrameworkException;
	public String getPayerAddressCountry();
	public void setPayerAddressCountry(String payerAddressCountry) throws FrameworkException;
	public String getBillingAgreementId();
	public void setBillingAgreementId(String billingAgreementId) throws FrameworkException;
	public String getNote();
	public void setNote(String note) throws FrameworkException;
	public String getInvoiceId();
	public void setInvoiceId(String invoiceId) throws FrameworkException;
}