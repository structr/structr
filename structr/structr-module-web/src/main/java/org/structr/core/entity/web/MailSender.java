/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity.web;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.renderer.MailSenderRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Send e-mails containing form data.
 *
 * Parameter names have to be registered in order for their values to be used in
 * subject and body templates.
 *
 * The parameter values can be addressed using placeholders like e.g. ${param}
 * for the value of the parameter named 'param'.
 *
 * @author axel
 */
public class MailSender extends Form {

	static {

		EntityContext.registerPropertySet(MailSender.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	/** Template for mail subject line */
	public enum Key implements PropertyKey {

		mailSubjectTemplate, htmlBodyTemplate, toAddressTemplate, fromAddressTemplate, ccAddressTemplate,
		bccAddressTemplate, toNameTemplate, fromNameTemplate;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new MailSenderRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/email_go.png";
	}

	/**
	 * Get template for mail subject line
	 *
	 * @return
	 */
	public String getMailSubjectTemplate() {
		return getStringProperty(Key.mailSubjectTemplate.name());
	}

	/**
	 * Get template for mail body
	 *
	 * @return
	 */
	public String getHtmlBodyTemplate() {
		return getStringProperty(Key.htmlBodyTemplate.name());
	}

	/**
	 * Get template for 'to address'
	 *
	 * @return
	 */
	public String getToAddressTemplate() {
		return getStringProperty(Key.toAddressTemplate.name());
	}

	/**
	 * Get template for 'from address'
	 *
	 * @return
	 */
	public String getFromAddressTemplate() {
		return getStringProperty(Key.fromAddressTemplate.name());
	}

	/**
	 * Get template for 'to name'
	 *
	 * @return
	 */
	public String getToNameTemplate() {
		return getStringProperty(Key.toNameTemplate.name());
	}

	/**
	 * Get template for 'from name'
	 *
	 * @return
	 */
	public String getFromNameTemplate() {
		return getStringProperty(Key.fromNameTemplate.name());
	}

	/**
	 * Get template for 'cc address'
	 *
	 * @return
	 */
	public String getCcAddressTemplate() {
		return getStringProperty(Key.ccAddressTemplate.name());
	}

	/**
	 * Get template for 'bcc address'
	 *
	 * @return
	 */
	public String getBccAddressTemplate() {
		return getStringProperty(Key.bccAddressTemplate.name());
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Set template for mail subject line
	 *
	 * @param value
	 */
	public void setMailSubjectTemplate(final String value) {

		setProperty(Key.mailSubjectTemplate.name(),
			    value);
	}

	/**
	 * Set template for mail body
	 *
	 * @param value
	 */
	public void setHtmlBodyTemplate(final String value) {

		setProperty(Key.htmlBodyTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'to address'
	 *
	 * @param value
	 */
	public void setToAddressTemplate(final String value) {

		setProperty(Key.toAddressTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'from address'
	 *
	 * @param value
	 */
	public void setFromAddressTemplate(final String value) {

		setProperty(Key.fromAddressTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'to name'
	 *
	 * @param value
	 */
	public void setToNameTemplate(final String value) {

		setProperty(Key.toNameTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'to address'
	 *
	 * @param value
	 */
	public void setFromNameTemplate(final String value) {

		setProperty(Key.fromNameTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'cc address'
	 *
	 * @param value
	 */
	public void setCcAddressTemplate(final String value) {

		setProperty(Key.ccAddressTemplate.name(),
			    value);
	}

	/**
	 * Set template for 'bcc address'
	 *
	 * @param value
	 */
	public void setBccAddressTemplate(final String value) {

		setProperty(Key.bccAddressTemplate.name(),
			    value);
	}
}
