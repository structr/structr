/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.mail.MailServiceInterface;
import org.structr.core.Services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdvancedMailContainer {

	protected static final Logger logger = LoggerFactory.getLogger(AdvancedMailContainer.class.getName());

	private String fromName      = null;
	private String fromAddress   = null;
	private String bounceAddress = null;
	private String subject       = null;
	private String htmlContent   = null;
	private String textContent   = null;

	private String inReplyTo     = null;

	private final Map<String, String> to      = new LinkedHashMap<>(1);
	private final Map<String, String> cc      = new LinkedHashMap<>(0);
	private final Map<String, String> bcc     = new LinkedHashMap<>(0);
	private final Map<String, String> replyTo = new LinkedHashMap<>(0);
	private final Map<String, String> headers = new LinkedHashMap<>(0);

	private final List<DynamicMailAttachment> attachments = new ArrayList();
	private final List<Pair<String, String>> mimeParts    = new ArrayList();

	private boolean saveOutgoingMessage = false;
	private Object lastOutgoingMessage = null;

	private String configurationPrefix = null;

	private boolean useManualConfiguration = false;
	private String smtpHost        = null;
	private Integer smtpPort       = null;
	private String smtpUser        = null;
	private String smtpPassword    = null;
	private Boolean smtpUseTLS     = null;
	private Boolean smtpRequireTLS = null;

	private String error           = null;

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

	public List<Pair<String, String>> getMimeParts() {
		return mimeParts;
	}

	public List<DynamicMailAttachment> getAttachments() {
		return attachments;
	}

	public void addMimePart(final String content, String contentType) {
		getMimeParts().add(Pair.of(content, contentType));
	}

	public void addAttachment(final DynamicMailAttachment att) {
		getAttachments().add(att);
	}

	public void clearMimeParts() {
		getMimeParts().clear();
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

	public void removeCustomHeader(final String name) {
		getCustomHeaders().remove(name);
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

	public void setInReplyTo(final String inReplyToMessageId) {
		this.inReplyTo = inReplyToMessageId;
	}

	public String getInReplyTo() {
		return this.inReplyTo;
	}

	public void clearInReplyTo() {
		this.inReplyTo = null;
	}

	public boolean isSaveOutgoingMessage() {
		return saveOutgoingMessage;
	}

	public void setSaveOutgoingMessage(boolean saveOutgoingMessage) {
		this.saveOutgoingMessage = saveOutgoingMessage;
	}

	public Object getLastOutgoingMessage() {

		if (isSaveOutgoingMessage()) {

			return this.lastOutgoingMessage;

		} else {

			logger.warn("Advanced Mail not configured to save outgoing messages - not returning last outgoing message!");
			return null;
		}
	}

	public String getConfigurationPrefix() {
		return configurationPrefix;
	}

	public void setConfigurationPrefix(final String configurationPrefix) {
		this.configurationPrefix = configurationPrefix;
	}

	public boolean shouldUseManualConfiguration() {
		return useManualConfiguration;
	}

	public void setManualConfiguration(final String smtpHost, final int smtpPort, final String smtpUser, final String smtpPassword, final boolean smtpUseTLS, final boolean smtpRequireTLS) {

		useManualConfiguration = true;

		this.smtpHost       = smtpHost;
		this.smtpPort       = smtpPort;
		this.smtpUser       = smtpUser;
		this.smtpPassword   = smtpPassword;
		this.smtpUseTLS     = smtpUseTLS;
		this.smtpRequireTLS = smtpRequireTLS;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public int getSmtpPort() {
		return smtpPort;
	}

	public String getSmtpUser() {
		return smtpUser;
	}

	public String getSmtpPassword() {
		return smtpPassword;
	}

	public boolean getSmtpUseTLS() {
		return smtpUseTLS;
	}

	public boolean getSmtpRequireTLS() {
		return smtpRequireTLS;
	}

	public void setError(final Throwable ex) {

		error = ex.getMessage();

		if (ex.getCause() != null) {
			error += "\n" + ex.getCause().getMessage();
		}
	}

	public String getError() {
		return error;
	}

	public boolean hasError() {
		return (error != null);
	}

	public void clearError() {
		error = null;
	}

	public void resetManualConfiguration() {

		useManualConfiguration = false;

		this.smtpHost       = null;
		this.smtpPort       = null;
		this.smtpUser       = null;
		this.smtpPassword   = null;
		this.smtpUseTLS     = null;
		this.smtpRequireTLS = null;
	}

	public void clearMailContainer() {

		setFromName(null);
		setFromAddress(null);
		setSubject(null);
		setHtmlContent(null);
		setTextContent(null);
		setBounceAddress(null);

		clearInReplyTo();

		setSaveOutgoingMessage(false);
		this.lastOutgoingMessage = null;

		setConfigurationPrefix(null);
		resetManualConfiguration();

		clearTo();
		clearCc();
		clearBcc();
		clearReplyTo();
		clearMimeParts();
		clearAttachments();
		clearCustomHeaders();

		clearError();
	}

	public String send(final SecurityContext securityContext) throws EmailException, FrameworkException {

		boolean mandatoryFieldsPresent = (this.fromAddress != null && this.subject != null && (this.htmlContent != null || this.textContent != null));

		if (!mandatoryFieldsPresent) {
			throw new FrameworkException(422, "Unable to send e-mail. Not all mandatory fields are set (fromAddress, subject and either htmlContent or textContent)'");
		}

		if (getTo().isEmpty() && getCc().isEmpty() && getBcc().isEmpty()) {
			throw new FrameworkException(422, "Unable to send e-mail: There aren't any recipients (empty to:, cc: and bcc: fields)");
		}

		final String sentMessageId = MailHelper.sendAdvancedMail(this);

		if (isSaveOutgoingMessage()) {

			createOutgoingMessage(securityContext, sentMessageId);
		}

		return sentMessageId;
	}


	/*~~~~~~~~ private functions  ~~~~~~~~~*/

	public String getDisplayName (final String address, final String name) {
		return (name == null) ? address : (name + "<" + address + ">");
	}

	public String getCombinedDisplayNames(final Map<String, String> addresses) {

		final ArrayList<String> toList = new ArrayList();

		for (Map.Entry<String, String> entry : addresses.entrySet()) {
			toList.add(this.getDisplayName(entry.getKey(), entry.getValue()));
		}

		return String.join(", ", toList);
	}

	private void createOutgoingMessage(final SecurityContext securityContext, final String messageId) {

		final MailServiceInterface mailServiceClass = Services.getInstance().getServiceImplementation(MailServiceInterface.class);

		if (mailServiceClass != null) {
			this.lastOutgoingMessage = mailServiceClass.saveOutgoingMessage(securityContext, this, messageId);
		}
	}
}