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
module structr.content.extraction.module {

    requires structr.base;

    // The Tika jars are automatic modules. structr.base "requires transitive" several automatic modules,
    // and an automatic module grants implied readability of every other automatic module, so code in
    // this module could use Tika without these lines.
    // We declare them anyway: relying on that side effect breaks silently as soon as structr.base stops
    // re-exporting its automatic modules.
    requires org.apache.tika.core;
    requires org.apache.tika.parser.ocr;
    requires org.apache.tika.parser.pdf;
    requires org.apache.tika.parser.microsoft;
    requires org.apache.tika.langdetect.optimaize;

    // page count of a PDF before parsing, to keep long documents from OCR (Tika has no per-document limit),
    // and finding and rendering the figures of a PDF
    requires org.apache.pdfbox;

    // figure images (BufferedImage, ImageIO)
    requires java.desktop;

    // filename-based automatic module name of jtokkit-<version>.jar (the jar has no Automatic-Module-Name)
    requires jtokkit;

    // Java API for other modules, e.g. the Enterprise AI module
    exports org.structr.extraction.api;
    exports org.structr.extraction.api.chunk;
    exports org.structr.extraction.api.document;
    exports org.structr.extraction.api.options;
    exports org.structr.extraction.api.processing;
    exports org.structr.extraction.api.search;
    exports org.structr.extraction.entity;

    // type, index and property names, settings, and the configuration type the AI module adds properties to
    exports org.structr.extraction;
    exports org.structr.extraction.traits.definitions;

    provides org.structr.module.StructrModule with
        org.structr.extraction.ContentExtractionModule;
}
