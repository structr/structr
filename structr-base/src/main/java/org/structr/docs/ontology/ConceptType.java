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
package org.structr.docs.ontology;

public enum ConceptType {

	// structural topics
	Topic("topic"), Concept("concept"), Component("component"), Feature("feature"), Mechanism("mechanism"),
	Provider("provider"), Service("service"), Capability("capability"), UseCase("use-case"), Type("type"),
	Category("category"),

	// output formats
	Table("table"), List("list"), Heading("heading"), SortedChildren("sorted-children"),

	// external sources
	MarkdownFolder("markdown-folder"), MarkdownFile("markdown-file"), MarkdownHeading("markdown-heading"),
	MarkdownTopic("markdown-topic"), CodeSource("code-source"), EnumSource("enum-source"), JavascriptFile("javascript-file"),

	// concepts for user interface elements
	Screen("screen"), Form("form"), Area("area"), Tab("tab"), Flyout("flyout"), Menu("menu"), Dialog("dialog"),
	Button("button"),

	// technical concepts
	Logfile("logfile"), Value("value"), LifecycleMethod("lifecycle-method"), HttpVerb("http-verb"),
	Function("function"), Setting("setting"), Helper("helper"), EventAction("event-action"),
	EventNotification("event-notification"), EventBehaviour("event-behaviour"),
	ParameterType("parameter-type"), Constant("constant"),

	// metadata
	Hint("hint"), Note("note"), Description("description"), Info("info"), Configuration("configuration"),
	Synonym("synonym"), Text("text"), Glossary("glossary"), GlossaryEntry("glossary-entry"),

	// java types
	Keyword("keyword"), Method("method"), Property("property"), UserDefinedFunction("user-defined-function"),
	MaintenanceCommand("maintenance-command"), SystemType("system-type"), RequestParameter("request-parameter"),
	RequestHeader("request-header"), Class("class"), RestEndpoint("rest-endpoint"),

	Verb("verb"), Blacklist("blacklist"), Unknown("unknown");

	private final String identifier;

	ConceptType(final String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}
}
