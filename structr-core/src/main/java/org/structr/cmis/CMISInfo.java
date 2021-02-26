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
package org.structr.cmis;

import java.util.GregorianCalendar;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;

/**
 * Optional interface for CMIS support in Structr core.
 *
 *
 */
public interface CMISInfo {

	// Structr initial commit: Tue Feb 1 23:12:27 2011 +0100

	public static final GregorianCalendar ROOT_FOLDER_DATE = new GregorianCalendar(2011, 1, 1, 23, 12, 27);
	public static final String ROOT_FOLDER_ID              = "/";

	public BaseTypeId getBaseTypeId();

	public CMISFolderInfo getFolderInfo();
	public CMISDocumentInfo getDocumentInfo();
	public CMISItemInfo getItemInfo();
	public CMISRelationshipInfo getRelationshipInfo();
	public CMISPolicyInfo getPolicyInfo();
	public CMISSecondaryInfo getSecondaryInfo();
}
