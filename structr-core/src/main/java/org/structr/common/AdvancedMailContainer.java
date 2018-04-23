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

	private boolean initDone = false;

	private String fromName      = null;
	private String fromAddress   = null;
	private String bounceAddress = null;
	private String subject       = null;
	private String htmlContent   = null;
	private String textContent   = null;

	private final Map<String, String> to  = new LinkedHashMap<>(1);
	private final Map<String, String> cc  = new LinkedHashMap<>(0);
	private final Map<String, String> bcc = new LinkedHashMap<>(0);

	private final ArrayList<DynamicMailAttachment> attachments = new ArrayList();

	public void init (final String fromAddress, final String fromName, final String subject, final String textContent) {
		init(fromAddress, fromName, subject, textContent, null);
	}

	public void init (final String fromAddress, final String fromName, final String subject, final String htmlContent, final String textContent) {
		this.fromAddress = fromAddress;
		this.fromName = fromName;
		this.subject = subject;
		this.htmlContent = htmlContent;
		this.textContent = textContent;

		this.initDone = true;
	}

	public void addTo(final String address, final String name) {
		getTo().put(address, name);
	}

	public void addCc(final String address, final String name) {
		getCc().put(address, name);
	}

	public void addBcc(final String address, final String name) {
		getBcc().put(address, name);
	}

	public void setBounce(final String address) {
		bounceAddress = address;
	}

	public void addAttachment(final DynamicMailAttachment att) {
		getAttachments().add(att);
	}

	public void send() throws EmailException, FrameworkException {

		if (!this.initDone) {
			throw new FrameworkException(422, "Cant send mail - must call 'mail_begin()'");
		}

		if(getTo().isEmpty() && getCc().isEmpty() && getBcc().isEmpty()) {
			throw new FrameworkException(422, "Cant send mail without any recipients");
		}

		MailHelper.sendAdvancedMail(this);
	}

	/**
	 * @return the fromName
	 */
	public String getFromName() {
		return fromName;
	}

	/**
	 * @return the fromAddress
	 */
	public String getFromAddress() {
		return fromAddress;
	}

	/**
	 * @return the bounceAddress
	 */
	public String getBounceAddress() {
		return bounceAddress;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @return the htmlContent
	 */
	public String getHtmlContent() {
		return htmlContent;
	}

	/**
	 * @return the textContent
	 */
	public String getTextContent() {
		return textContent;
	}

	/**
	 * @return the to
	 */
	public Map<String, String> getTo() {
		return to;
	}

	/**
	 * @return the cc
	 */
	public Map<String, String> getCc() {
		return cc;
	}

	/**
	 * @return the bcc
	 */
	public Map<String, String> getBcc() {
		return bcc;
	}

	/**
	 * @return the attachments
	 */
	public ArrayList<DynamicMailAttachment> getAttachments() {
		return attachments;
	}
}