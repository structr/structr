/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.cmis.info;

import java.util.GregorianCalendar;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyMap;

/**
 * Optional interface for CMIS support in Structr core.
 *
 *
 */
public interface CMISObjectInfo {

	public SecurityContext getSecurityContext();

	public BaseTypeId getBaseTypeId();
	public String getUuid();
	public String getName();
	public String getType();
	public String getCreatedBy();
	public String getLastModifiedBy();
	public GregorianCalendar getCreationDate();
	public GregorianCalendar getLastModificationDate();

	public PropertyMap getDynamicProperties();
	public AllowableActions getAllowableActions();
	public List<Ace> getAccessControlEntries();
}
