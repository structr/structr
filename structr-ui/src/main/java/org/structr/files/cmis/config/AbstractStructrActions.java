/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.files.cmis.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.Principal;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.structr.cmis.common.CMISExtensionsData;

/**
 * Abstract class which maps allowable actions to specific objects like
 * files and folders.
 * @author Marcel Romagnuolo
 */


public abstract class AbstractStructrActions extends CMISExtensionsData implements AllowableActions {

    protected final Set<Action> actions = new LinkedHashSet<>();

    public AbstractStructrActions(List <Ace> aces, String username) {

	//represents the two flags, which are also used in the Structr filesystem
	boolean visibleToPublic = false;
	boolean visibleToAuth = false;

	List<String> permissions = null;
	boolean readFlag = false;

	for(Ace ace : aces) {

		Principal p = ace.getPrincipal(); //Principal from CMIS Framework

		if(p.getId().equals(org.structr.core.entity.Principal.ANONYMOUS)) {

			visibleToPublic = true;
		}

		if(p.getId().equals(org.structr.core.entity.Principal.ANYONE)) {

			visibleToAuth = true;
		}

		if(username.equals(p.getId())) {

			permissions = ace.getPermissions();
		}
	}

	if(permissions != null) {

		for(String pm : permissions) {
			switch (pm) {

				case "read": { setReadPermissions(); readFlag = true; break; }
				case "write": { setWritePermissions(); break; }
				case "delete": { setDeletePermissions(); break; }
				case "accessControl": { setAccessControlPermissions(); break; }
				default: throw new CmisInvalidArgumentException("A problem occured setting allowable actions.");
			}
		}
	}

	//readFlag is false, if the current user doesn't have any
	//individual read-rights
	if(!readFlag) {

		//check if any visible-flag is set instead
		if(visibleToAuth || visibleToPublic) {

			setReadPermissions();
		}
	}
    }

    abstract void setReadPermissions();
    abstract void setWritePermissions();
    abstract void setDeletePermissions();
    abstract void setAccessControlPermissions();
}
