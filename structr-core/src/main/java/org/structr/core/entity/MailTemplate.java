/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a template with placeholders, to be used for sending e-mails
 *
 * @author Axel Morgner
 *
 */
public class MailTemplate extends AbstractNode {

	private static final Logger logger = Logger.getLogger(MailTemplate.class.getName());

	public static final Property<String>  text   = new StringProperty("text").indexed();
	public static final Property<String>  locale = new StringProperty("locale").indexed();

	public static final org.structr.common.View uiView = new org.structr.common.View(MailTemplate.class, PropertyView.Ui,
		type, name, text, locale
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(MailTemplate.class, PropertyView.Public,
		type, name, text, locale
	);

	//~--- get methods ----------------------------------------------------
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean hasError = false;

		String _name	= getProperty(name);
		String _locale	= getProperty(locale);
		String _uuid	= getProperty(id);

		hasError |= ValidationHelper.checkStringNotBlank(this, name, errorBuffer);
		hasError |= ValidationHelper.checkStringNotBlank(this, locale, errorBuffer);

		try {
			Result<MailTemplate> res = StructrApp.getInstance(securityContext).nodeQuery(MailTemplate.class).andName(_name).and(locale, _locale).getResult();
			if (!res.isEmpty() && res.size() > 1) {

				hasError = true;
				errorBuffer.add(MailTemplate.class.getName(), new UniqueToken(_uuid, name, _name));
				errorBuffer.add(MailTemplate.class.getName(), new UniqueToken(_uuid, locale, _locale));
			}


		} catch (FrameworkException fe) {

			logger.log(Level.WARNING, "Could not search a MailTemplate with name {0} and locale {1}", new Object[]{getProperty(name), getProperty(locale)});

		}


		return !hasError;

	}

}
