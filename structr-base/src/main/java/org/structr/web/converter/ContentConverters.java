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
package org.structr.web.converter;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class ContentConverters {

	private static final Map<String, Adapter<String, String>> ContentConverters = new LinkedHashMap<>();
	private static final ThreadLocalAsciiDocProcessor asciiDocProcessor         = new ThreadLocalAsciiDocProcessor();
	private static final ThreadLocalTracWikiProcessor tracWikiProcessor         = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor           = new ThreadLocalTextileProcessor();
	private static final ThreadLocalFlexMarkProcessor flexMarkProcessor         = new ThreadLocalFlexMarkProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor       = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor     = new ThreadLocalConfluenceProcessor();

	static {

		ContentConverters.put("text/markdown", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {

					com.vladsch.flexmark.util.ast.Node document = flexMarkProcessor.get().parser.parse(s);
					return flexMarkProcessor.get().renderer.render(document);
				}

				return "";
			}

		});

		ContentConverters.put("text/textile", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return textileProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});

		ContentConverters.put("text/mediawiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return mediaWikiProcessor.get().parseToHtml(s);
				}

				return "";
			}

		});

		ContentConverters.put("text/tracwiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return tracWikiProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});

		ContentConverters.put("text/confluence", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return confluenceProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});

		ContentConverters.put("text/asciidoc", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return asciiDocProcessor.get().render(s, new HashMap<String, Object>());
				}

				return "";

			}

		});
	}

	public static Adapter<String, String> getConverterForType(final String contentType) {
		return ContentConverters.get(contentType);
	}

	//~--- inner classes --------------------------------------------------
	private static class ThreadLocalConfluenceProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new ConfluenceDialect());

		}

	}


	private static class ThreadLocalMediaWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new MediaWikiDialect());

		}

	}

	private static class ThreadLocalFlexMarkProcessor extends ThreadLocal<FlexMarkProcessor> {

		@Override
		protected FlexMarkProcessor initialValue() {

			final MutableDataSet options = new MutableDataSet();

            options.setAll(PegdownOptionsAdapter.flexmarkOptions(Extensions.ALL));
//			options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

			Parser parser = Parser.builder(options).build();
			HtmlRenderer renderer = HtmlRenderer.builder(options).build();

			return new FlexMarkProcessor(parser, renderer);
		}

	}

	private static class ThreadLocalTextileProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TextileDialect());

		}

	}

	private static class ThreadLocalTracWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TracWikiDialect());

		}
	}

	private static class ThreadLocalAsciiDocProcessor extends ThreadLocal<Asciidoctor> {

		@Override
		protected Asciidoctor initialValue() {

			return Factory.create();
		}
	}

	private static class FlexMarkProcessor {

		Parser parser;
		HtmlRenderer renderer;

		public FlexMarkProcessor(final Parser parser, final HtmlRenderer renderer) {
			this.parser = parser;
			this.renderer = renderer;
		}
	}
}
