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
package org.structr.web.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.schema.action.ActionContext;

import java.util.HashMap;
import java.util.Map;

public class MaintenanceModeCommand extends NodeServiceCommand implements MaintenanceCommand {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceModeCommand.class.getName());

    static {

        MaintenanceResource.registerMaintenanceCommand("maintenanceMode", MaintenanceModeCommand.class);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws FrameworkException {

        final String action = (String) parameters.get("action");
        boolean success     = false;

        if ("enable".equals(action)) {

            success = Services.getInstance().setMaintenanceMode(true);

        } else if ("disable".equals(action)) {

            success = Services.getInstance().setMaintenanceMode(false);

        } else {

            logger.warn("Unsupported action '{}'", action);
        }

        if (success) {

            final Map<String, Object> msgData = new HashMap();
            msgData.put(MaintenanceCommand.COMMAND_TYPE_KEY, "MAINTENANCE");
            msgData.put("enabled",                           Settings.MaintenanceModeEnabled.getValue());
            msgData.put("baseUrl",                           ActionContext.getBaseUrl(securityContext.getRequest(), true));
            TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
        }
    }

    @Override
    public boolean requiresEnclosingTransaction() {
        return false;
    }

    @Override
    public boolean requiresFlushingOfCaches() {
        return false;
    }
}
