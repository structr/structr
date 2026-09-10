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
package org.structr.diff.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.structr.diff.export.ExportSource;
import org.structr.diff.model.Entity;
import org.structr.diff.model.Kind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A deployment export, decomposed into entities.
 *
 * This is where the identity policy lives, and it is the only place that knows what any given
 * export file means. Everything downstream, matcher and classifier alike, works on entities and
 * could not name a single export file.
 *
 * <p>The policy is not one rule. Most artifacts are keyed by uuid, because measurement says uuids
 * survive across instances almost perfectly. Schema is keyed by name, because schema.json carries
 * no ids at all. Text is keyed by parent and position, because it has nothing else. A handful of
 * kinds also declare a weaker fallback key for the matcher's second pass, for the case where a
 * seeder created the same logical node separately on each instance.
 *
 * <p>An unreadable or unexpected file is skipped rather than fatal. An export is written by a
 * version of Structr that may be older or newer than this parser, and refusing to compare two
 * exports because one of them contains a file this version has never heard of would make the tool
 * useless exactly when it is most wanted.
 */
public class ExportParser {

	private final ExportSource source;
	private final List<Entity> entities = new LinkedList<>();
	private final List<String> skipped  = new LinkedList<>();

	public ExportParser(final ExportSource source) throws IOException {

		this.source = source;

		manifest("pages.json",      Kind.PAGE);
		manifest("components.json", Kind.COMPONENT);
		manifest("templates.json",  Kind.TEMPLATE);
		manifest("files.json",      Kind.FILE);

		markup("pages/");
		markup("components/");
		markup("templates/");

		array("localizations.json",                  Kind.LOCALIZATION,      byId(),         null);
		array("widgets.json",                        Kind.WIDGET,            byId(),         null);
		array("application-configuration-data.json", Kind.APP_CONFIG,        byId(),         null);
		array("scratchpads.json",                    Kind.SCRATCHPAD,        byId(),         null);
		array("events/action-mapping.json",          Kind.ACTION_MAPPING,    byId(),         null);
		array("events/parameter-mapping.json",       Kind.PARAMETER_MAPPING, byId(),         null);
		array("security/schema-grants.json",         Kind.SCHEMA_GRANT,      byId(),         null);

		// Kinds where a seeder may have created the same logical node on each instance. The uuid
		// stays the primary key, because a user-created one is perfectly stable and discarding it
		// would cost far more than it saves: three grants share the signature
		// DOMElement/_id/event and are told apart only by their uuids.
		array("security/grants.json",         Kind.RESOURCE_ACCESS, byId(), byMember("signature"));
		array("modules/ai/llm-tools.json",    Kind.LLM_TOOL,        byId(), byMember("name"));
		// An agent carries no uuid: exportAgents writes none, because nothing references an agent
		// by id the way an agent references its tools. Name is the primary key here, not a fallback,
		// and keying it on a missing id would silently drop every agent from the comparison.
		array("modules/ai/llm-agents.json",   Kind.LLM_AGENT,       byIdOrElse("name"), byMember("name"));

		bpmnNodes();
		schema();
		schemaSources();
		filePayloads();

		for (final String path : new String[] { "deployment.conf", "pre-deploy.conf", "post-deploy.conf",
			"page-paths.json", "sites.json", "component-configurations.json", "data-adapters.json",
			"mail-templates.json", "modules/ai/llm-provider-configuration.json",
			"modules/ai/llm-agent-tool-configuration.json" }) {

			if (source.exists(path)) {

				entities.add(new Entity(Kind.CONFIG_FILE, path, null, null, path, null, read(path), null, path));
			}
		}
	}

	public List<Entity> getEntities() {

		return entities;
	}

	/** Files the parser did not understand. Reported, never silently forgotten. */
	public List<String> getSkipped() {

		return skipped;
	}

	// ----- artifact families -----

	/**
	 * pages.json and its siblings: an object keyed by name, whose values carry the uuid.
	 *
	 * Keyed on the uuid rather than the name, which is exactly what makes a rename visible as a
	 * rename. A file that changes path while keeping its uuid has been renamed; keyed by name, the
	 * same event reads as an unrelated deletion beside an unrelated addition.
	 */
	private void manifest(final String path, final String kind) {

		final JsonObject root = object(path);

		if (root == null) {

			return;
		}

		for (final Map.Entry<String, JsonElement> entry : root.entrySet()) {

			if (!entry.getValue().isJsonObject()) {

				continue;
			}

			final JsonObject value = entry.getValue().getAsJsonObject();
			final String id        = string(value, "id");

			entities.add(new Entity(kind, id != null ? id : "name:" + entry.getKey(), null, null, entry.getKey(),
				attributes(value, "id"), null, null, path));
		}
	}

	private void array(final String path, final String kind, final Function<JsonObject, String> key,
			final Function<JsonObject, String> alternateKey) {

		final JsonArray root = jsonArray(path);

		if (root == null) {

			return;
		}

		for (final JsonElement element : root) {

			if (!element.isJsonObject()) {

				continue;
			}

			final JsonObject value = element.getAsJsonObject();
			final String primary   = key.apply(value);

			if (primary == null) {

				continue;
			}

			entities.add(new Entity(kind, primary, alternateKey != null ? alternateKey.apply(value) : null,
				null, string(value, "name"), attributes(value, "id"), null, null, path));
		}
	}

	private void markup(final String folder) throws IOException {

		for (final String path : source.paths()) {

			if (path.startsWith(folder) && path.endsWith(".html")) {

				new DomParser(read(path), path, entities);
			}
		}
	}

	/**
	 * schema.json: types, their properties and methods, and the global methods.
	 *
	 * Name-keyed throughout, and not by choice. The file carries no id field anywhere, so a
	 * renamed type is indistinguishable from one deleted and another added. Recovering that would
	 * need a content-similarity fallback and a threshold, which is a decision not yet taken.
	 */
	private void schema() {

		final JsonObject root = object("schema/schema.json");

		if (root == null) {

			return;
		}

		final JsonObject definitions = root.getAsJsonObject("definitions");

		if (definitions != null) {

			for (final Map.Entry<String, JsonElement> entry : definitions.entrySet()) {

				if (!entry.getValue().isJsonObject()) {

					continue;
				}

				final String typeName    = entry.getKey();
				final JsonObject typeDef = entry.getValue().getAsJsonObject();

				entities.add(new Entity(Kind.SCHEMA_TYPE, typeName, null, null, typeName,
					attributes(typeDef, "properties", "methods", "views"), null, null, "schema/schema.json"));

				members(typeDef, "properties", Kind.SCHEMA_PROPERTY, typeName);
				members(typeDef, "methods",    Kind.SCHEMA_METHOD,   typeName);
			}
		}

		final JsonElement methods = root.get("methods");

		if (methods != null && methods.isJsonArray()) {

			for (final JsonElement element : methods.getAsJsonArray()) {

				if (element.isJsonObject()) {

					final JsonObject method = element.getAsJsonObject();
					final String name       = string(method, "name");

					if (name != null) {

						entities.add(new Entity(Kind.GLOBAL_METHOD, name, null, null, name,
							attributes(method, "name"), null, null, "schema/schema.json"));
					}
				}
			}
		}
	}

	/**
	 * The properties or methods of one type.
	 *
	 * Both appear as an object keyed by name in the exports seen so far, but methods have also
	 * been written as an array of objects carrying their own name, so both shapes are accepted.
	 * Guessing wrong here would drop every method of every type without any error.
	 */
	private void members(final JsonObject typeDef, final String member, final String kind, final String typeName) {

		final JsonElement value = typeDef.get(member);

		if (value == null) {

			return;
		}

		if (value.isJsonObject()) {

			for (final Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {

				entities.add(new Entity(kind, typeName + "." + entry.getKey(), null, typeName, entry.getKey(),
					entry.getValue().isJsonObject() ? attributes(entry.getValue().getAsJsonObject()) : null,
					entry.getValue().isJsonObject() ? null : entry.getValue().toString(), null, "schema/schema.json"));
			}

		} else if (value.isJsonArray()) {

			for (final JsonElement element : value.getAsJsonArray()) {

				if (element.isJsonObject()) {

					final JsonObject entry = element.getAsJsonObject();
					final String name      = string(entry, "name");

					if (name != null) {

						entities.add(new Entity(kind, typeName + "." + name, null, typeName, name,
							attributes(entry, "name"), null, null, "schema/schema.json"));
					}
				}
			}
		}
	}

	/** Method and function bodies written beside schema.json, addressed by their path under schema/. */
	private void schemaSources() throws IOException {

		for (final String path : source.paths()) {

			if (path.startsWith("schema/") && !"schema/schema.json".equals(path)) {

				final String key = path.substring("schema/".length());

				entities.add(new Entity(Kind.SCHEMA_SOURCE, key, null, null, key, null, read(path), null, path));
			}
		}
	}

	/**
	 * The bytes under files/, keyed by the uuid of the File node that owns them.
	 *
	 * Keyed by path instead, renaming a file reads as one payload removed and another added, and
	 * the rename the manifest correctly reported is contradicted two lines further down. The path
	 * survives as the fallback key, for a payload whose manifest entry is missing.
	 */
	private void filePayloads() throws IOException {

		final JsonObject manifest    = object("files.json");
		final Map<String, String> id = new TreeMap<>();

		if (manifest != null) {

			for (final Map.Entry<String, JsonElement> entry : manifest.entrySet()) {

				if (entry.getValue().isJsonObject()) {

					final String uuid = string(entry.getValue().getAsJsonObject(), "id");

					if (uuid != null) {

						id.put(stripLeadingSlash(entry.getKey()), uuid);
					}
				}
			}
		}

		for (final String path : source.paths()) {

			if (!path.startsWith("files/")) {

				continue;
			}

			final String relative                = path.substring("files/".length());
			final Map<String, Object> attributes = new TreeMap<>();
			final byte[] bytes                   = bytes(path);

			attributes.put("sha256", digest(bytes));
			attributes.put("size", bytes.length);

			entities.add(new Entity(Kind.FILE_PAYLOAD, id.getOrDefault(relative, "path:" + relative),
				"path:" + relative, null, relative, attributes, null, null, path));
		}
	}

	private void bpmnNodes() {

		final JsonObject root = object("modules/process/bpmn-deployment.json");

		if (root == null || !root.has("nodes") || !root.get("nodes").isJsonArray()) {

			return;
		}

		for (final JsonElement element : root.getAsJsonArray("nodes")) {

			if (element.isJsonObject()) {

				final JsonObject node = element.getAsJsonObject();
				final String id       = string(node, "id");

				if (id != null) {

					entities.add(new Entity(Kind.BPMN_NODE, id, null, null, string(node, "name"),
						attributes(node, "id"), null, null, "modules/process/bpmn-deployment.json"));
				}
			}
		}
	}

	// ----- helpers -----

	private static Function<JsonObject, String> byId() {

		return o -> string(o, "id");
	}

	/** The uuid when the export carries one, the named member when it does not. */
	private static Function<JsonObject, String> byIdOrElse(final String member) {

		return o -> {

			final String id = string(o, "id");

			return id != null ? id : string(o, member);
		};
	}

	private static Function<JsonObject, String> byMember(final String member) {

		return o -> string(o, member);
	}

	private static String string(final JsonObject object, final String member) {

		final JsonElement value = object.get(member);

		return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
	}

	private static Map<String, Object> attributes(final JsonObject object, final String... exclude) {

		final Map<String, Object> result = new TreeMap<>();
		final List<String> excluded      = new ArrayList<>(List.of(exclude));

		for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {

			if (!excluded.contains(entry.getKey())) {

				// the JSON text rather than a mapped value: the comparison is equality, and a
				// faithful string beats a lossy conversion that could report a change where the
				// export holds the same thing written differently
				result.put(entry.getKey(), entry.getValue().toString());
			}
		}

		return result;
	}

	private JsonObject object(final String path) {

		final JsonElement element = json(path);

		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private JsonArray jsonArray(final String path) {

		final JsonElement element = json(path);

		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	private JsonElement json(final String path) {

		try {

			if (!source.exists(path)) {

				return null;
			}

			return JsonParser.parseString(read(path));

		} catch (Exception cause) {

			skipped.add(path);

			return null;
		}
	}

	private String read(final String path) throws IOException {

		return new String(bytes(path), StandardCharsets.UTF_8);
	}

	private byte[] bytes(final String path) throws IOException {

		try (final InputStream in = source.open(path)) {

			return in.readAllBytes();
		}
	}

	private static String stripLeadingSlash(final String s) {

		return s.startsWith("/") ? s.substring(1) : s;
	}

	private static String digest(final byte[] bytes) {

		try {

			final MessageDigest sha = MessageDigest.getInstance("SHA-256");
			final StringBuilder buf = new StringBuilder();

			for (final byte b : sha.digest(bytes)) {

				buf.append(String.format("%02x", b));
			}

			return buf.substring(0, 32);

		} catch (NoSuchAlgorithmException cause) {

			throw new IllegalStateException("SHA-256 is required by the platform", cause);
		}
	}
}
