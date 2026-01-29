/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class RestartRequiredChangeHandler<T> implements SettingChangeHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(RestartRequiredChangeHandler.class.getName());

    @Override
    public void execute(final Setting setting, final T oldValue, final T newValue) {

        logRestartRequiredMessage(setting);
    }

    public static void logRestartRequiredMessage (final Setting setting, String... moreInfo) {

        logger.info("Changing this setting ({}) requires a restart to take effect!", setting.getKey());

        Arrays.stream(moreInfo).forEach(info -> logger.info(info));
    }
}
