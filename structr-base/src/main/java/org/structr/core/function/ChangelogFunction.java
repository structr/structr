/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodeProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChangelogFunction extends AdvancedScriptingFunction {

	private static final Logger logger = LoggerFactory.getLogger(ChangelogFunction.class.getName());

	@Override
	public String getName() {
		return "changelog";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final String changelog = getChangelogForObject(sources[0]);

			if (changelog != null && !("".equals(changelog))) {

				final ChangelogFilter changelogFilter = new ChangelogFilter();
				changelogFilter.setIsUserCentricChangelog(isUserCentric());

				if (sources.length >= 3 && sources[2] != null) {

					if (sources[2] instanceof Map) {

						changelogFilter.processJavaScriptConfigurationObject((Map)sources[2]);

					} else {

						final int maxLength = sources.length;

						for (int i = 2; (i + 2) <= maxLength; i += 2) {

							if (sources[i] != null && sources[i+1] != null) {
								changelogFilter.addFilterEntry(sources[i].toString(), sources[i+1]);
							}
						}

						if (maxLength % 2 == 1 && sources[maxLength-1] != null) {
							logger.warn("Ignoring dangling filterKey: {}", sources[maxLength-1]);
						}
					}
				}

				if (sources.length >= 2 && Boolean.TRUE.equals(sources[1])) {
					changelogFilter.setResolveTargets(true);
				}

				return changelogFilter.getFilteredChangelog(changelog);
			}

			return new ArrayList();

		} catch (IOException ioex) {

			logger.error("Unable to create changelog file: {}", ioex.getMessage());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (IllegalArgumentException iae) {

			logger.warn(iae.getMessage());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${changelog(entity[, resolve=false[, filterKey, filterValue...]])}. Example: ${changelog(current, false, 'verb', 'change', 'timeTo', now)}"),
			Usage.javaScript("Usage: ${{ $.changelog(entity[, resolve=false[, filterObject]]); }}. Example: ${{ $.changelog($.current, false, {verb:'change', timeTo: new Date()})); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the changelog for a given entity.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `resolve` parameter controls if remote entities are resolved. Every changelog entry which has a `target` will be resolved as `targetObj` (if the remote entity still exists in the database).

		**Filtering**
		All filter options are chained using the boolean AND operator. Only changelog entries matching all of the specified filters will be returned.
		For filter keys which can occurr more than once, the filter values are combined using the boolean OR operator (see examples 1 and 2)

		| Filter Key | Applicable Changelog verbs (\\*) | Changelog Entry will be returned if | max. occurrences |
		|---|---|---|---|
		| timeFrom (\\*\\*) | create, delete, link, unlink, change | `timeFrom` <= `time` of the entry | 1 (\\*\\*\\*) |
		| timeTo (\\*\\*) | create, delete, link, unlink, change | `timeTo` >= `time` of the entry | 1 (\\*\\*\\*) |
		| verb | create, delete, link, unlink, change | `verb` of the entry matches at least one of the verbs | n (\\*\\*\\*\\*) |
		| userId | create, delete, link, unlink, change | `userId` of the entry matches at least one of the userIds     | n (\\*\\*\\*\\*) |
		| userName | create, delete, link, unlink, change | `userName` of the entry matches at least one of the userNames | n (\\*\\*\\*\\*) |
		| relType | link, unlink | `rel` of the entry matches at least one of the relTypes | n (\\*\\*\\*\\*) |
		| relDir | link, unlink | `relDir` of the entry matches the given relDir | 1 (\\*\\*\\*) |
		| target | create, delete, link, unlink | `target` of the entry matches at least one of the targets     | n (\\*\\*\\*\\*) |
		| key | change | `key` of the entry matches at least one of the keys | n (\\*\\*\\*\\*) |

		(\\*) If a filter parameter is supplied, only changelog entries can be returned to which it is applicable. (e.g. combining `key` and `relType` can never yield a result as they are mutually exclusive)
		(\\*\\*) timeFrom/timeTo can be specified as a Long (time in ms since epoch), as a JavaScript Date object, or as a String with the format `yyyy-MM-dd'T'HH:mm:ssZ`
		(\\*\\*\\*) The last supplied parameter takes precedence over the others
		(\\*\\*\\*\\*) The way we supply multiple occurrences of a keyword can differ from StructrScript to JavaScript
		""";
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("entity [, resolve=false [, filterKey, filterValue ]... ]"),
			Signature.structrScript("uuid [, resolve=false [, filterKey, filterValue ]... ]"),
			Signature.javaScript("entity [, resolve=false [, filterKey, filterValue ]... ]"),
			Signature.javaScript("uuid [, resolve=false [, filterKey, filterValue ]... ]")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("entityOrUUID", "entity to fetch changelog for"),
			Parameter.optional("resolve", "whether remote entities are resolved and returned"),
			Parameter.optional("filterKey", "filter key, see above table"),
			Parameter.optional("filterValue", "filter value, see above table")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${changelog(node, false, 'verb', 'link')}", "Return all changelog entries with verb=link"),
			Example.javaScript("${{ $.changelog(node, false, {verb: ['link', 'unlink']}); }}", "Return all changelog entries with verb=(link OR unlink)"),
			Example.javaScript("${{ $.changelog(node, false, {verb: ['link', 'unlink'], 'relType': 'OWNS'}); }}", "Return all changelog entries with (rel=OWNS) AND (verb=(link OR unlink))"),
			Example.javaScript("${{ $.changelog(node, false, {verb: ['link', 'unlink'], 'target': '<NODEID>'}); }}", "Return all changelog entries with (target=<NODEID>) AND (verb=(link OR unlink))")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The Changelog has to be enabled for this function to work properly. This can be done via the `application.changelog.enabled` key in configuration file structr.conf",
			"The `prev` and `val` keys in the `change` event contain JSON encoded elements since they can be strings or arrays.",
			"In a StructrScript environment parameters are passed as pairs of `'filterKey1', 'filterValue1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}

	private String getChangelogForObject (final Object obj) throws IOException {

		if (obj instanceof GraphObject) {

			return getChangelogForGraphObject((GraphObject)obj);

		} else if (obj instanceof String) {

			return getChangelogForString((String) obj);

		} else {

			throw new IllegalArgumentException("First parameter must be either graph object or UUID string. Was: " + obj);
		}
	}

	protected String getChangelogForGraphObject (final GraphObject obj) throws IOException {

		return getChangelogForUUID(obj.getUuid(), (obj.isNode() ? "n" : "r"));

	}

	protected String getChangelogForString (final String inputString) throws IOException {

		if (Settings.isValidUuid(inputString)) {

			String changelog = getChangelogForUUID(inputString, "n");

			if (changelog.equals("")) {
				changelog = getChangelogForUUID(inputString, "r");
			}

			return changelog;

		} else {

			throw new IllegalArgumentException("Given string is not a UUID: " + inputString);
		}
	}

	protected String getChangelogForUUID (final String uuid, final String changelogType) throws IOException {

		java.io.File file = getChangeLogFileOnDisk(changelogType, uuid, false);

		if (file.exists()) {

			return FileUtils.readFileToString(file, "utf-8");

		} else {

			return "";
		}
	}

	public static java.io.File getChangeLogFileOnDisk(final String typeFolderName, final String uuid, final boolean create) {

		final String changelogPath = Settings.ChangelogPath.getValue();
		final String uuidPath      = getDirectoryPath(uuid);
		final java.io.File file    = new java.io.File(changelogPath + java.io.File.separator + typeFolderName + java.io.File.separator + uuidPath + java.io.File.separator + uuid);

		// create parent directory tree
		file.getParentFile().mkdirs();

		// create file only if requested
		if (!file.exists() && create) {

			try {

				file.createNewFile();

			} catch (IOException ioex) {

				logger.error("Unable to create changelog file {}: {}", file, ioex.getMessage());
			}
		}

		return file;
	}

	public static SeekableByteChannel getWriterForChangeLogOnDisk(final String typeFolderName, final String uuid, final boolean create) {

		final String changelogPath    = Settings.ChangelogPath.getValue();
		final String uuidPath         = getDirectoryPath(uuid);
		final java.nio.file.Path path = Paths.get(changelogPath, typeFolderName, uuidPath, uuid);

		try {

			// create parent directory tree
			Files.createDirectories(path.getParent());

			// create file only if requested
			if (create) {

				if (!Files.exists(path)) {

					Files.createFile(path);
				}
			}

			//return Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			return Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);

		} catch (IOException ioex) {

			logger.error("Unable to create changelog file {}: {}", path.toString(), ioex.getMessage());
		}

		return null;
	}

	static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	protected boolean isUserCentric () {
		return false;
	}

	private class ChangelogFilter {

		private final JsonParser _jsonParser = new JsonParser();
		private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();
		private final App _app = StructrApp.getInstance();

		private final ArrayList<String> _filterVerbs    = new ArrayList();
		private Long _filterTimeFrom                    = null;
		private Long _filterTimeTo                      = null;
		private final ArrayList<String> _filterUserId   = new ArrayList();
		private final ArrayList<String> _filterUserName = new ArrayList();
		private final ArrayList<String> _filterRelType  = new ArrayList();
		private String _filterRelDir                    = null;
		private final ArrayList<String> _filterTarget   = new ArrayList();
		private final ArrayList<String> _filterKey      = new ArrayList();

		private boolean _resolveTargets = false;
		private boolean _noFilterConfig = true;
		private boolean _isUserCentricChangelog = false;

		// Properties for the changelog entries
		private final Property<String>  changelogVerb                        = new StringProperty("verb");
		private final Property<String>  changelogTime                        = new StringProperty("time");
		private final Property<String>  changelogUserId                      = new StringProperty("userId");
		private final Property<String>  changelogUserName                    = new StringProperty("userName");
		private final Property<String>  changelogTarget                      = new StringProperty("target");
		private final Property<NodeInterface> changelogTargetObj             = new EndNodeProperty<>("targetObj");
		private final Property<String>  changelogRel                         = new StringProperty("rel");
		private final Property<String>  changelogRelId                       = new StringProperty("relId");
		private final Property<String>  changelogRelDir                      = new StringProperty("relDir");
		private final Property<String>  changelogKey                         = new StringProperty("key");
		private final Property<String>  changelogPrev                        = new StringProperty("prev");
		private final Property<String>  changelogVal                         = new StringProperty("val");
		private final Property<String>  changelogType                        = new StringProperty("type");

		public void setIsUserCentricChangelog(final boolean userCentric) {
			_isUserCentricChangelog = userCentric;
		}

		public void addFilterEntry (final String filterKey, final Object filterValue) {

			switch (filterKey) {
				case "verb":
					_filterVerbs.add(filterValue.toString());
					break;

				case "timeFrom":
					_filterTimeFrom = toLong(filterValue);
					break;

				case "timeTo":
					_filterTimeTo = toLong(filterValue);
					break;

				case "userId":
					_filterUserId.add(filterValue.toString());
					break;

				case "userName":
					_filterUserName.add(filterValue.toString());
					break;

				case "relType":
					_filterRelType.add(filterValue.toString());
					break;

				case "relDir":
					_filterRelDir = filterValue.toString();
					break;

				case "target":
					_filterTarget.add(filterValue.toString());
					break;

				case "key":
					_filterKey.add(filterValue.toString());
					break;

				default:
					logger.warn("Unknown filter key: {}", filterKey);
			}
		}

		public void processJavaScriptConfigurationObject(final Map<String, Object> javascriptConfigObject) {

			assignStringsIfPresent(javascriptConfigObject.get("verb"), _filterVerbs);

			assignLongIfPresent(javascriptConfigObject.get("timeFrom"), _filterTimeFrom);
			assignLongIfPresent(javascriptConfigObject.get("timeTo"), _filterTimeTo);

			assignStringsIfPresent(javascriptConfigObject.get("userId"), _filterUserId);
			assignStringsIfPresent(javascriptConfigObject.get("userName"), _filterUserName);
			assignStringsIfPresent(javascriptConfigObject.get("relType"), _filterRelType);

			if (javascriptConfigObject.get("relDir") != null) {
				_filterRelDir = javascriptConfigObject.get("relDir").toString();
			}

			assignStringsIfPresent(javascriptConfigObject.get("target"), _filterTarget);
			assignStringsIfPresent(javascriptConfigObject.get("key"), _filterKey);
		}

		private void assignLongIfPresent (final Object possibleLong, Long targetLongReference) {

			if (possibleLong != null) {
				targetLongReference = ((Long)possibleLong);
			}
		}

		private void assignStringsIfPresent (final Object possibleListOrString, ArrayList<String> targetListReference) {

			if (possibleListOrString != null) {
				if (possibleListOrString instanceof List) {
					targetListReference.addAll((List)possibleListOrString);
				} else if (possibleListOrString instanceof String) {
					targetListReference.add((String)possibleListOrString);
				}
			}
		}

		public void setResolveTargets (final boolean resolve) {
			_resolveTargets = resolve;
		}

		public List getFilteredChangelog (final String changelog) throws FrameworkException {

			final List list = new ArrayList();

			_noFilterConfig = (
				_filterVerbs.isEmpty() && _filterTimeFrom == null && _filterTimeTo == null && _filterUserId.isEmpty() &&
				_filterUserName.isEmpty() && _filterRelType.isEmpty() && _filterRelDir == null && _filterTarget.isEmpty() && _filterKey.isEmpty()
			);

			for (final String entry : changelog.split("\n")) {

				final JsonObject jsonObj = _jsonParser.parse(entry).getAsJsonObject();
				final String verb     = jsonObj.get("verb").getAsString();
				final long time       = jsonObj.get("time").getAsLong();
				final String userId   = (jsonObj.has("userId") ? jsonObj.get("userId").getAsString() : null);
				final String userName = (jsonObj.has("userName") ? jsonObj.get("userName").getAsString() : null);
				final String relType  = (jsonObj.has("rel") ? jsonObj.get("rel").getAsString() : null);
				final String relId    = (jsonObj.has("relId") ? jsonObj.get("relId").getAsString() : null);
				final String relDir   = (jsonObj.has("relDir") ? jsonObj.get("relDir").getAsString() : null);
				final String target   = (jsonObj.has("target") ? jsonObj.get("target").getAsString() : null);
				final String key      = (jsonObj.has("key") ? jsonObj.get("key").getAsString() : null);
				final String type     = (jsonObj.has("type") ? jsonObj.get("type").getAsString() : null);

				if (doesFilterApply(verb, time, userId, userName, relType, relDir, target, key)) {

					final GraphObjectMap obj = new GraphObjectMap();

					obj.put(changelogVerb, verb);
					obj.put(changelogTime, time);

					if (!_isUserCentricChangelog) {
						obj.put(changelogUserId, userId);
						obj.put(changelogUserName, userName);
					}

					switch (verb) {
						case "create":
						case "delete":
							obj.put(changelogTarget, target);
							obj.put(changelogType, type);

							if (_resolveTargets) {
								obj.put(changelogTargetObj, resolveTarget(target));
							}

							list.add(obj);

							break;

						case "link":
						case "unlink":
							obj.put(changelogRel, relType);
							obj.put(changelogRelId, relId);
							obj.put(changelogRelDir, relDir);
							obj.put(changelogTarget, target);

							if (_resolveTargets) {
								obj.put(changelogTargetObj, resolveTarget(target));
							}

							list.add(obj);

							break;

						case "change":
							obj.put(changelogKey, key);
							obj.put(changelogPrev, _gson.toJson(jsonObj.get("prev")));
							obj.put(changelogVal, _gson.toJson(jsonObj.get("val")));

							if (_isUserCentricChangelog) {

								obj.put(changelogTarget, target);
								if (_resolveTargets) {
									obj.put(changelogTargetObj, resolveTarget(target));
								}
							}

							list.add(obj);

							break;

						default:
							logger.warn("Unknown verb in changelog: \"{}\"", verb);
							break;
					}
				}
			}

			return list;
		}

		private Object resolveTarget(final String targetId) throws FrameworkException {

			if (Principal.SUPERUSER_ID.equals(targetId)) {
				return null;
			} else if (Principal.ANONYMOUS.equals(targetId)) {
				return null;
			}

			return _app.getNodeById(targetId);
		}

		public boolean doesFilterApply (final String verb, final long time, final String userId, final String userName, final String relType, final String relDir, final String target, final String key) {

			return (
				(_noFilterConfig == true) ||
				(
					(_filterVerbs.isEmpty()    || _filterVerbs.contains(verb)       ) &&
					(_filterTimeFrom == null   || _filterTimeFrom <= time           ) &&
					(_filterTimeTo == null     || _filterTimeTo >= time             ) &&
					(_filterUserId.isEmpty()   || _filterUserId.contains(userId)    ) &&
					(_filterUserName.isEmpty() || _filterUserName.contains(userName)) &&
					(_filterRelType.isEmpty()  || _filterRelType.contains(relType)  ) &&
					(_filterRelDir == null     || _filterRelDir.equals(relDir)      ) &&
					(_filterTarget.isEmpty()   || _filterTarget.contains(target)    ) &&
					(_filterKey.isEmpty()      || _filterKey.contains(key)          )
				)
			);
		}

		private Long toLong (final Object possibleLong) {

			if (possibleLong instanceof Date) {

				return ((Date)possibleLong).getTime();

			} else if (possibleLong instanceof Number) {

				return ((Number)possibleLong).longValue();

			} else {

				try {
					// parse with format from IS
					return (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(possibleLong.toString())).getTime();

				} catch (ParseException ignore) {
					// silently fail as this can be any string
				}
			}

			logger.warn("Cannot convert object to long: {}", possibleLong);

			return null;
		}
	}
}
