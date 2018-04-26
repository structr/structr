/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.mail.EmailException;
import org.structr.common.error.FrameworkException;

public class AdvancedMailContainer {

	private String fromName      = null;
	private String fromAddress   = null;
	private String bounceAddress = null;
	private String subject       = null;
	private String htmlContent   = null;
	private String textContent   = null;

	private final Map<String, String> to      = new LinkedHashMap<>(1);
	private final Map<String, String> cc      = new LinkedHashMap<>(0);
	private final Map<String, String> bcc     = new LinkedHashMap<>(0);
	private final Map<String, String> replyTo = new LinkedHashMap<>(0);
	private final Map<String, String> headers = new LinkedHashMap<>(0);

	private final ArrayList<DynamicMailAttachment> attachments = new ArrayList();

	public String getFromName() {
		return fromName;
	}

	public void setFromName(final String fromName) {
		this.fromName = fromName;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(final String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public void setFrom (final String fromAddress, final String fromName) {
		setFromAddress(fromAddress);
		setFromName(fromName);
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	public void setHtmlContent(final String htmlContent) {
		this.htmlContent = htmlContent;
	}

	public String getTextContent() {
		return textContent;
	}

	public void setTextContent(final String textContent) {
		this.textContent = textContent;
	}


	public Map<String, String> getTo() {
		return to;
	}

	public void addTo(final String address, final String name) {
		getTo().put(address, name);
	}

	public void clearTo() {
		getTo().clear();
	}


	public Map<String, String> getCc() {
		return cc;
	}

	public void addCc(final String address, final String name) {
		getCc().put(address, name);
	}

	public void clearCc() {
		getCc().clear();
	}


	public Map<String, String> getBcc() {
		return bcc;
	}

	public void addBcc(final String address, final String name) {
		getBcc().put(address, name);
	}

	public void clearBcc() {
		getBcc().clear();
	}


	public Map<String, String> getReplyTo() {
		return replyTo;
	}

	public void addReplyTo(final String address, final String name) {
		getReplyTo().put(address, name);
	}

	public void clearReplyTo() {
		getReplyTo().clear();
	}


	public ArrayList<DynamicMailAttachment> getAttachments() {
		return attachments;
	}

	public void addAttachment(final DynamicMailAttachment att) {
		getAttachments().add(att);
	}

	public void clearAttachments() {
		getAttachments().clear();
	}


	public Map<String, String> getCustomHeaders() {
		return headers;
	}

	public void addCustomHeader(final String name, final String content) {
		getCustomHeaders().put(name, content);
	}

	public void clearCustomHeaders() {
		getCustomHeaders().clear();
	}


	public void setBounceAddress(final String address) {
		bounceAddress = address;
	}

	public String getBounceAddress() {
		return bounceAddress;
	}


	public void clearMailContainer() {
		setFromName(null);
		setFromAddress(null);
		setSubject(null);
		setHtmlContent(null);
		setTextContent(null);
		setBounceAddress(null);

		clearTo();
		clearCc();
		clearBcc();
		clearReplyTo();
		clearAttachments();
		clearCustomHeaders();
	}


	public void send() throws EmailException, FrameworkException {

		boolean mandatoryFieldsPresent = (this.fromAddress != null && this.subject != null && this.htmlContent != null);

		if (!mandatoryFieldsPresent) {
			throw new FrameworkException(422, "Cant send mail. Not all mandatory fields are set (fromAddress, subject, hmtlContent)'");
		}

		if (getTo().isEmpty() && getCc().isEmpty() && getBcc().isEmpty()) {
			throw new FrameworkException(422, "Cant send mail without any recipients");
		}

		MailHelper.sendAdvancedMail(this);
	}
}