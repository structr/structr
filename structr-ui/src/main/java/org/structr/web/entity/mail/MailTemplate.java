/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.mail;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.Property;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.web.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.notion.PropertySetNotion;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.property.Forward;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a template with placeholders, to be used for sending e-mails
 * 
 * @author Axel Morgner
 *
 */
public class MailTemplate extends AbstractNode {
	
	private static final Logger logger = Logger.getLogger(MailTemplate.class.getName());

	public static final Forward<Content> text   = new Forward("text", Content.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(true, uuid, name), true);
	public static final Property<String>        locale = new StringProperty("locale").indexed();
	
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
		String _uuid	= getProperty(uuid);

		hasError |= ValidationHelper.checkStringNotBlank(this, name, errorBuffer);
		hasError |= ValidationHelper.checkStringNotBlank(this, locale, errorBuffer);

		try {
			Result<MailTemplate> res = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(
				Search.andExactType(MailTemplate.class),
				Search.andExactName(_name),
				Search.andExactProperty(securityContext, locale, _locale)
			);

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

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (key.equals(text)) {
			
			Content content = getProperty(text);
			
			if (content != null) {
				return content.getProperty(Content.content);
			}
		}
		
		return super.getPropertyForIndexing(key);

	}

}
