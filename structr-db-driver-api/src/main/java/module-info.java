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
module structr.db.driver.api {
    requires java.management;
    requires org.slf4j;
    requires org.apache.commons.configuration2;
    requires org.apache.commons.collections4;
    requires org.apache.commons.lang3;

    exports org.structr.api;
    exports org.structr.api.config;
    exports org.structr.api.graph;
    exports org.structr.api.index;
    exports org.structr.api.schema;
    exports org.structr.api.search;
    exports org.structr.api.service;
    exports org.structr.api.util;
    exports org.structr.api.util.html;
    exports org.structr.api.util.html.attr;

}
