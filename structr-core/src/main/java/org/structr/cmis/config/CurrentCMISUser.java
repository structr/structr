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

package org.structr.cmis.config;

/**
 * Provides information of currently logged in user in the CMIS Workbench. 
 * Probably only a temporary solution.
 * @author Marcel Romagnuolo
 */


public class CurrentCMISUser {
    
    private static String username;
    
    private CurrentCMISUser() {
	CurrentCMISUser.username = null;
    }
    
    private static CurrentCMISUser instance;
    
    public static CurrentCMISUser getInstance(String username) {
	if(instance == null) {
	    instance = new CurrentCMISUser();
	}
	if(username != null) {
	    CurrentCMISUser.username = username;
	}
	return instance;
    }
    
    public String getUsername() {
	return username;
    }
}
