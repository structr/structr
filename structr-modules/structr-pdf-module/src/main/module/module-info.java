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
module structr.pdf.module {

    requires structr.base;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;
    requires org.apache.pdfbox.io;

    // PdfRenderer is the module's public entry point: render a page, convert HTML to PDF bytes. Callers
    // outside this module reach it reflectively and add the read edge themselves, so that they need no
    // requires and this module stays optional, but the export has to be here for that to resolve.
    exports org.structr.pdf;

    // instantiated reflectively by HttpService in structr.base (configured by class name)
    exports org.structr.pdf.servlet;

    provides org.structr.module.StructrModule with
        org.structr.pdf.PDFModule;
}
