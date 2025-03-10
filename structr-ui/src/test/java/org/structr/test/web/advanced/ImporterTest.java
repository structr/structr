/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.web.advanced;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Test the import of external pages
 *
 */
public class ImporterTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ImporterTest.class.getName());

	@Test
	public void testBootstrapJumbotronEditModeNone() {

		Settings.JsonIndentation.setValue(true);
		Settings.HtmlIndentation.setValue(true);

		final String actual   = testImport("http://structr.github.io/structr/getbootstrap.com/docs/3.3/examples/jumbotron/", RenderContext.EditMode.NONE);
		final String expected = "<!DOCTYPE html>\n"
			+ "<html lang=\"en\">\n"
			+ "	<head>\n"
			+ "		<meta charset=\"utf-8\">\n"
			+ "		<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
			+ "		<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->\n"
			+ "		<meta name=\"description\" content=\"\">\n"
			+ "		<meta name=\"author\" content=\"\">\n"
			+ "		<link rel=\"icon\" href=\"/favicon.ico?1\">\n"
			+ "		<title>Jumbotron Template for Bootstrap</title><!-- Bootstrap core CSS -->\n"
			+ "		<link href=\"/dist/css/bootstrap.min.css?1\" rel=\"stylesheet\"><!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->\n"
			+ "		<link href=\"/assets/css/ie10-viewport-bug-workaround.css?1\" rel=\"stylesheet\"><!-- Custom styles for this template -->\n"
			+ "		<link href=\"/jumbotron.css?1\" rel=\"stylesheet\"><!-- Just for debugging purposes. Don't actually copy these 2 lines! --><!--[if lt IE 9]><script src=\"../../assets/js/ie8-responsive-file-warning.js\"></script><![endif]-->\n"
			+ "		<script src=\"/assets/js/ie-emulation-modes-warning.js\" type=\"text/javascript\"></script><!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries --><!--[if lt IE 9]>\n"
			+ "      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\"></script>\n"
			+ "      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>\n"
			+ "    <![endif]-->\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<nav class=\"navbar navbar-inverse navbar-fixed-top\">\n"
			+ "			<div class=\"container\">\n"
			+ "				<div class=\"navbar-header\">\n"
			+ "					<button class=\"navbar-toggle collapsed\" type=\"button\" aria-controls=\"navbar\" aria-expanded=\"false\" data-target=\"#navbar\" data-toggle=\"collapse\"><span class=\"sr-only\">Toggle navigation</span><span class=\"icon-bar\"></span><span class=\"icon-bar\"></span><span class=\"icon-bar\"></span></button><a class=\"navbar-brand\" href=\"#\">Project name</a>\n"
			+ "				</div>\n"
			+ "				<div id=\"navbar\" class=\"navbar-collapse collapse\">\n"
			+ "					<form class=\"navbar-form navbar-right\">\n"
			+ "						<div class=\"form-group\">\n"
			+ "							<input class=\"form-control\" type=\"text\" placeholder=\"Email\">\n"
			+ "						</div>\n"
			+ "						<div class=\"form-group\">\n"
			+ "							<input class=\"form-control\" type=\"password\" placeholder=\"Password\">\n"
			+ "						</div>\n"
			+ "						<button class=\"btn btn-success\" type=\"submit\">Sign in</button>\n"
			+ "					</form>\n"
			+ "				</div><!--/.navbar-collapse -->\n"
			+ "			</div>\n"
			+ "		</nav><!-- Main jumbotron for a primary marketing message or call to action -->\n"
			+ "		<div class=\"jumbotron\">\n"
			+ "			<div class=\"container\">\n"
			+ "				<h1>Hello, world!</h1>\n"
			+ "				<p>This is a template for a simple marketing or informational website. It includes a large callout called a jumbotron and three supporting pieces of content. Use it as a starting point to create something more unique.</p>\n"
			+ "				<p><a class=\"btn btn-primary btn-lg\" href=\"#\" role=\"button\">Learn more »</a></p>\n"
			+ "			</div>\n"
			+ "		</div>\n"
			+ "		<div class=\"container\"><!-- Example row of columns -->\n"
			+ "			<div class=\"row\">\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec sed odio dui. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Vestibulum id ligula porta felis euismod semper. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "			</div>\n"
			+ "			<hr>\n"
			+ "			<footer>\n"
			+ "				<p>© 2016 Company, Inc.</p>\n"
			+ "			</footer>\n"
			+ "		</div><!-- /container --><!-- Bootstrap core JavaScript\n"
			+ "    ================================================== --><!-- Placed at the end of the document so the pages load faster -->\n"
			+ "		<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\" type=\"text/javascript\"></script>\n"
			+ "		<script type=\"text/javascript\">window.jQuery || document.write('<script src=\"../../assets/js/vendor/jquery.min.js\"><\\/script>')</script>\n"
			+ "		<script src=\"/dist/js/bootstrap.min.js\" type=\"text/javascript\"></script><!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->\n"
			+ "		<script src=\"/assets/js/ie10-viewport-bug-workaround.js\" type=\"text/javascript\"></script>\n"
			+ "	</body>\n"
			+ "</html>";

		compare(expected, actual);

		//assertFileExists("/favicon.ico", 1);
		assertFileExists("/dist/css/bootstrap.min.css", 0);
		assertFileExists("/assets/css/ie10-viewport-bug-workaround.css", 0);
		assertFileExists("/jumbotron.css", 0);
		assertFileExists("/assets/js/ie-emulation-modes-warning.js", 0);
		assertFileExists("/assets/js/ie10-viewport-bug-workaround.js", 0);

		assertFileNotExists("html5shiv.min.js");
		assertFileNotExists("respond.min.js");

	}

	@Test
	public void testBootstrapJumbotronEditModeWidget() {

		Settings.JsonIndentation.setValue(true);
		Settings.HtmlIndentation.setValue(true);

		final String actual   = testImport("http://structr.github.io/structr/getbootstrap.com/docs/3.3/examples/jumbotron/", RenderContext.EditMode.WIDGET);
		final String expected = "<!DOCTYPE html>\n"
			+ "<html lang=\"en\">\n"
			+ "	<head>\n"
			+ "		<meta charset=\"utf-8\">\n"
			+ "		<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
			+ "		<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->\n"
			+ "		<meta name=\"description\" content=\"\">\n"
			+ "		<meta name=\"author\" content=\"\">\n"
			+ "		<link rel=\"icon\" href=\"${link.path}?${link.version}\">\n"
			+ "		<title>Jumbotron Template for Bootstrap</title><!-- Bootstrap core CSS -->\n"
			+ "		<link href=\"${link.path}?${link.version}\" rel=\"stylesheet\"><!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->\n"
			+ "		<link href=\"${link.path}?${link.version}\" rel=\"stylesheet\"><!-- Custom styles for this template -->\n"
			+ "		<link href=\"${link.path}?${link.version}\" rel=\"stylesheet\"><!-- Just for debugging purposes. Don't actually copy these 2 lines! --><!--[if lt IE 9]><script src=\"../../assets/js/ie8-responsive-file-warning.js\"></script><![endif]-->\n"
			+ "		<script src=\"${link.path}\" type=\"text/javascript\"></script><!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries --><!--[if lt IE 9]>\n"
			+ "      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\"></script>\n"
			+ "      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>\n"
			+ "    <![endif]-->\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<nav class=\"navbar navbar-inverse navbar-fixed-top\">\n"
			+ "			<div class=\"container\">\n"
			+ "				<div class=\"navbar-header\">\n"
			+ "					<button class=\"navbar-toggle collapsed\" type=\"button\" aria-controls=\"navbar\" aria-expanded=\"false\" data-target=\"#navbar\" data-toggle=\"collapse\"><span class=\"sr-only\">Toggle navigation</span><span class=\"icon-bar\"></span><span class=\"icon-bar\"></span><span class=\"icon-bar\"></span></button><a class=\"navbar-brand\" href=\"#\">Project name</a>\n"
			+ "				</div>\n"
			+ "				<div id=\"navbar\" class=\"navbar-collapse collapse\">\n"
			+ "					<form class=\"navbar-form navbar-right\">\n"
			+ "						<div class=\"form-group\">\n"
			+ "							<input class=\"form-control\" type=\"text\" placeholder=\"Email\">\n"
			+ "						</div>\n"
			+ "						<div class=\"form-group\">\n"
			+ "							<input class=\"form-control\" type=\"password\" placeholder=\"Password\">\n"
			+ "						</div>\n"
			+ "						<button class=\"btn btn-success\" type=\"submit\">Sign in</button>\n"
			+ "					</form>\n"
			+ "				</div><!--/.navbar-collapse -->\n"
			+ "			</div>\n"
			+ "		</nav><!-- Main jumbotron for a primary marketing message or call to action -->\n"
			+ "		<div class=\"jumbotron\">\n"
			+ "			<div class=\"container\">\n"
			+ "				<h1>Hello, world!</h1>\n"
			+ "				<p>This is a template for a simple marketing or informational website. It includes a large callout called a jumbotron and three supporting pieces of content. Use it as a starting point to create something more unique.</p>\n"
			+ "				<p><a class=\"btn btn-primary btn-lg\" href=\"#\" role=\"button\">Learn more »</a></p>\n"
			+ "			</div>\n"
			+ "		</div>\n"
			+ "		<div class=\"container\"><!-- Example row of columns -->\n"
			+ "			<div class=\"row\">\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "				<div class=\"col-md-4\">\n"
			+ "					<h2>Heading</h2>\n"
			+ "					<p>Donec sed odio dui. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Vestibulum id ligula porta felis euismod semper. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>\n"
			+ "					<p><a class=\"btn btn-default\" href=\"#\" role=\"button\">View details »</a></p>\n"
			+ "				</div>\n"
			+ "			</div>\n"
			+ "			<hr>\n"
			+ "			<footer>\n"
			+ "				<p>© 2016 Company, Inc.</p>\n"
			+ "			</footer>\n"
			+ "		</div><!-- /container --><!-- Bootstrap core JavaScript\n"
			+ "    ================================================== --><!-- Placed at the end of the document so the pages load faster -->\n"
			+ "		<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\" type=\"text/javascript\"></script>\n"
			+ "		<script type=\"text/javascript\">window.jQuery || document.write('<script src=\"../../assets/js/vendor/jquery.min.js\"><\\/script>')</script>\n"
			+ "		<script src=\"${link.path}\" type=\"text/javascript\"></script><!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->\n"
			+ "		<script src=\"${link.path}\" type=\"text/javascript\"></script>\n"
			+ "	</body>\n"
			+ "</html>";

		compare(expected, actual);
	}

	@Test
	public void testNewsfeedWidget() {

		Settings.JsonIndentation.setValue(true);
		Settings.HtmlIndentation.setValue(true);

		final String actual = testImportWidget("<div class=\"row\">\n"
			+ "	<div class=\"col-lg-12\">\n"
			+ "		<h2>Feeds</h2>\n"
			+ "		<div class=\"table-responsive\">\n"
			+ "			<table class=\"table table-bordered table-striped\">\n"
			+ "				<thead>\n"
			+ "					<tr>\n"
			+ "						<th>Name</th>\n"
			+ "						<th>URL</th>\n"
			+ "						<th>Created</th>\n"
			+ "						<th>Last modified</th>\n"
			+ "						<th>Actions</th>\n"
			+ "					</tr>\n"
			+ "				</thead>\n"
			+ "				<tbody>\n"
			+ "					<tr data-structr-id=\"${feed.id}\" data-structr-meta-data-key=\"feed\" data-structr-meta-rest-query=\"DataFeed?sort=createdDate&amp;order=desc\">\n"
			+ "						<td><a href=\"/feed/${feed.id}\">${feed.name}</a></td>\n"
			+ "						<td><a target=\"_blank\" href=\"${feed.url}\">${feed.url}</a></td>\n"
			+ "						<td>${date_format(feed.createdDate, 'dd.MM.YYYY HH:mm')}</td>\n"
			+ "						<td>${date_format(feed.lastModifiedDate, 'dd.MM.YYYY HH:mm')}</td>\n"
			+ "						<td>\n"
			+ "							<button class=\"btn btn-xs btn-default\" data-structr-id=\"${feed.id}\"> Edit </button>\n"
			+ "							<button class=\"btn btn-xs btn-danger\" data-structr-id=\"${feed.id}\"> Delete </button>\n"
			+ "							<button class=\"btn btn-xs btn-info\" data-structr-id=\"${feed.id}\"> Update</button>\n"
			+ "						</td>\n"
			+ "					</tr>\n"
			+ "					<tr>\n"
			+ "						<td>\n"
			+ "							<input type=\"text\" class=\"form-control\" placeholder=\"Name\">\n"
			+ "						</td>\n"
			+ "						<td colspan=\"3\">\n"
			+ "							<input type=\"text\" class=\"form-control\" placeholder=\"Feed URL\">\n"
			+ "						</td>\n"
			+ "						<td>\n"
			+ "							<button class=\"btn btn-success\">Add Feed</button>\n"
			+ "						</td>\n"
			+ "					</tr>\n"
			+ "				</tbody>\n"
			+ "			</table>\n"
			+ "		</div>\n"
			+ "	</div>\n"
			+ "</div>",
			RenderContext.EditMode.WIDGET);

		//System.out.println(source);

		final String expected = "<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head></head>\n"
			+ "	<body>\n"
			+ "		<div class=\"row\">\n"
			+ "			<div class=\"col-lg-12\">\n"
			+ "				<h2>Feeds</h2>\n"
			+ "				<div class=\"table-responsive\">\n"
			+ "					<table class=\"table table-bordered table-striped\">\n"
			+ "						<thead>\n"
			+ "							<tr>\n"
			+ "								<th>Name</th>\n"
			+ "								<th>URL</th>\n"
			+ "								<th>Created</th>\n"
			+ "								<th>Last modified</th>\n"
			+ "								<th>Actions</th>\n"
			+ "							</tr>\n"
			+ "						</thead>\n"
			+ "						<tbody>\n"
			+ "							<tr data-structr-id=\"${feed.id}\" data-structr-meta-data-key=\"feed\" data-structr-meta-rest-query=\"DataFeed?sort=createdDate&amp;order=desc\">\n"
			+ "								<td><a href=\"/feed/${feed.id}\">${feed.name}</a></td>\n"
			+ "								<td><a target=\"_blank\" href=\"${feed.url}\">${feed.url}</a></td>\n"
			+ "								<td>${date_format(feed.createdDate, 'dd.MM.YYYY HH:mm')}</td>\n"
			+ "								<td>${date_format(feed.lastModifiedDate, 'dd.MM.YYYY HH:mm')}</td>\n"
			+ "								<td>\n"
			+ "									<button class=\"btn btn-xs btn-default\" data-structr-id=\"${feed.id}\"> Edit </button>\n"
			+ "									<button class=\"btn btn-xs btn-danger\" data-structr-id=\"${feed.id}\"> Delete </button>\n"
			+ "									<button class=\"btn btn-xs btn-info\" data-structr-id=\"${feed.id}\"> Update</button>\n"
			+ "								</td>\n"
			+ "							</tr>\n"
			+ "							<tr>\n"
			+ "								<td>\n"
			+ "									<input class=\"form-control\" type=\"text\" placeholder=\"Name\">\n"
			+ "								</td>\n"
			+ "								<td colspan=\"3\">\n"
			+ "									<input class=\"form-control\" type=\"text\" placeholder=\"Feed URL\">\n"
			+ "								</td>\n"
			+ "								<td>\n"
			+ "									<button class=\"btn btn-success\">Add Feed</button>\n"
			+ "								</td>\n"
			+ "							</tr>\n"
			+ "						</tbody>\n"
			+ "					</table>\n"
			+ "				</div>\n"
			+ "			</div>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>";

		compare(expected, actual);
	}

	@Test
	public void testWidgetWithScriptTags() {

		try (final Tx tx = app.tx()) {

			Settings.JsonIndentation.setValue(true);
			Settings.HtmlIndentation.setValue(true);

			final String actual = testImportWidget(
					"<div>\n"
							+ "      <script type=\"text/javascript\" src=\"/structr/js/lib/jquery-1.11.1.min.js\"></script>\n"
							+ "      <script type=\"text/javascript\"></script>\n"
							+ "</div>",
					RenderContext.EditMode.WIDGET, "https://widgets.structr.org/structr/rest/widgets");

			final String expected = "<!DOCTYPE html>\n"
					+ "<html>\n"
					+ "	<head></head>\n"
					+ "	<body>\n"
					+ "		<div>\n"
					+ "			<script type=\"text/javascript\" src=\"/structr/js/lib/jquery-1.11.1.min.js\"></script>\n"
					+ "			<script type=\"text/javascript\"></script>\n"
					+ "		</div>\n"
					+ "	</body>\n"
					+ "</html>";

			compare(expected, actual);

			final NodeInterface secondScriptElement = app.nodeQuery("Script").blank(Traits.of("Script").key("_html_src")).getFirst();

			assertNull(secondScriptElement.getOutgoingRelationship(StructrTraits.LINK_SOURCE_LINK_LINKABLE));

			tx.success();

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}
	}

	@Test
	public void testScriptAttributes() {

		try (final Tx tx = app.tx()) {

			Settings.JsonIndentation.setValue(true);
			Settings.HtmlIndentation.setValue(true);

			final String actual = testImportWidget("<div><script type=\"module\"></script></div>", RenderContext.EditMode.WIDGET);
			final String expected = "<!DOCTYPE html>\n"
					+ "<html>\n"
					+ "	<head></head>\n"
					+ "	<body>\n"
					+ "		<div>\n"
					+ "			<script type=\"module\"></script>\n"
					+ "		</div>\n"
					+ "	</body>\n"
					+ "</html>";

			compare(expected, actual);

			NodeInterface script = app.nodeQuery("Script").getFirst();

			assertEquals("Script type is not imported correctly", "module", script.getProperty(Traits.of("Script").key("_html_type")));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private String testImport(final String address, final RenderContext.EditMode editMode) {

		String sourceHtml = null;

		try {

			// render page into HTML string
			try (final Tx tx = app.tx()) {

				final Importer importer = new Importer(securityContext, null, address, "testpage", true, true, false, false);

				importer.parse();

				// create page from source
				final Page sourcePage = importer.readPage();
				sourceHtml = sourcePage.getContent(editMode);
				tx.success();
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return sourceHtml;
	}

	private String testImportWidget(final String code, final RenderContext.EditMode editMode) {

		return testImportWidget(code, editMode, null);
	}

	private String testImportWidget(final String code, final RenderContext.EditMode editMode, final String address) {

		String sourceHtml = null;

		try {

			// render page into HTML string
			try (final Tx tx = app.tx()) {

				final Importer importer = new Importer(securityContext, code, address, "widget", true, true, false, false);

				importer.parse(true);

				// create page from source
				final Page sourcePage = importer.readPage();
				sourceHtml = sourcePage.getContent(editMode);
				tx.success();
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return sourceHtml;
	}

	private void assertFileExists(final String expectedPath, final int expectedVersion) {

		final NodeInterface file;

		try (final Tx tx = app.tx()) {

			final String filename = PathHelper.getName(expectedPath);
			file = app.nodeQuery(StructrTraits.FILE).andName(filename).getFirst();

			assertNotNull(filename + " file not found", file);
			assertEquals("Wrong path of " + filename + " file", (String) file.getProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY)), expectedPath);
			assertEquals("Wrong version of " + filename + " file", (int) file.getProperty(Traits.of(StructrTraits.FILE).key("version")), expectedVersion);

			tx.success();

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}
	}

	private void assertFileNotExists(final String expectedPath) {

		final NodeInterface file;
		try (final Tx tx = app.tx()) {

			final String filename = PathHelper.getName(expectedPath);
			file = app.nodeQuery(StructrTraits.FILE).andName(filename).getFirst();

			assertNull("File " + filename + " found", file);

			tx.success();

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}
	}

	private void compare(final String expected, final String actual) {

		// creates a flat list of all nodes that the given documents contain
		// and compares the lists node by node in order to avoid different
		// attribute orders to break the tests

		final List<Node> expectedNodes = new ArrayList<>();
		final List<Node> actualNodes   = new ArrayList<>();

		collectNodes(expectedNodes, Jsoup.parse(expected));
		collectNodes(actualNodes, Jsoup.parse(actual));

		for (int i=0; i < expectedNodes.size(); i++) {

			final Node expectedNode = expectedNodes.get(i);
			final Node actualNode   = actualNodes.get(i);
			final String acName     = actualNode.nodeName();

			assertEquals("Tag name mismatch", expectedNode.nodeName(), actualNode.nodeName());
			assertEquals("Attribute mismatch in " + acName, expectedNode.attributes(), actualNode.attributes());
		}
	}

	private void collectNodes(final List<Node> target, final Node current) {

		target.add(current);

		for (final Node child : current.childNodes()) {
			collectNodes(target, child);
		}
	}
}
