/*
 * Copyright (C) 2010-2026 Structr GmbH
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
module structr.geo.transformations.module {

    requires structr.base;
    requires java.desktop;
    requires org.apache.commons.io;
    requires jai.core;
    requires jt.utils;
    requires org.geotools.api;
    requires org.geotools.coverage;
    requires org.geotools.main;
    requires org.geotools.metadata;
    requires org.geotools.process_raster;
    requires org.geotools.referencing;
    requires org.geotools.render;
    requires org.geotools.process;
    requires org.geotools.shapefile;
    requires org.geotools.swing;
    requires org.geotools.xsd.xsd_core;
    requires org.locationtech.jts;

    provides org.structr.module.StructrModule with
        org.structr.geo.GeoTransformationsModule;
}
