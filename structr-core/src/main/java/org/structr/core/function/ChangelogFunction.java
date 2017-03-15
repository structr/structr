/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class ChangelogFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CHANGELOG = "Usage: ${changelog(entity[, resolve=false[, filterKey, filterValue...]])}. Example: ${changelog(current, false, 'verb', 'change', 'timeTo', now)}";
	public static final String ERROR_MESSAGE_CHANGELOG_JS = "Usage: ${{Structr.changelog(entity[, resolve=false[, filterObject]])}}. Example: ${{Structr.changelog(Structr.get('current', false, {verb:\"change\", timeTo: new Date()}))}}";

	private static final Logger logger = LoggerFactory.getLogger(ChangelogFunction.class.getName());


	@Override
	public String getName() {
		return "changelog()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources.length >= 1) {

			if (sources[0] instanceof GraphObject) {

				final String changelog = ((GraphObject) sources[0]).getProperty(GraphObject.structrChangeLog);

				if (changelog != null && !("".equals(changelog))) {

					final ChangelogFilter changelogFilter = new ChangelogFilter();

					if (sources.length >= 3 && sources[2] != null) {

						if (sources[2] instanceof NativeObject) {

							changelogFilter.processJavaScriptConfigurationObject((NativeObject) sources[2]);

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

			} else {

				logger.warn("First parameter must be of type GraphObject: \"{}\"", sources[0]);
				return usage(ctx.isJavaScriptContext());
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CHANGELOG_JS : ERROR_MESSAGE_CHANGELOG);
	}

	@Override
	public String shortDescription() {
		return "Returns the changelog object";
	}

	private class ChangelogFilter {

		private final JsonParser _jsonParser = new JsonParser();
		private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();
		private final App _app = StructrApp.getInstance();

		private final ArrayList<String> _filterVerbs = new ArrayList();
		private Long _filterTimeFrom = null;
		private Long _filterTimeTo = null;
		private final ArrayList<String> _filterUserId = new ArrayList();
		private final ArrayList<String> _filterUserName = new ArrayList();
		private final ArrayList<String> _filterRelType = new ArrayList();
		private final ArrayList<String> _filterTarget = new ArrayList();
		private final ArrayList<String> _filterKey = new ArrayList();

		private boolean _resolveTargets = false;
		private boolean _noFilterConfig = true;

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

		public void processJavaScriptConfigurationObject(final NativeObject javascriptConfigObject) {

			assignStringsIfPresent(javascriptConfigObject.get("verb"), _filterVerbs);

			assignLongIfPresent(javascriptConfigObject.get("timeFrom"), _filterTimeFrom);
			assignLongIfPresent(javascriptConfigObject.get("timeTo"), _filterTimeTo);

			assignStringsIfPresent(javascriptConfigObject.get("userId"), _filterUserId);
			assignStringsIfPresent(javascriptConfigObject.get("userName"), _filterUserName);
			assignStringsIfPresent(javascriptConfigObject.get("relType"), _filterRelType);
			assignStringsIfPresent(javascriptConfigObject.get("target"), _filterTarget);
			assignStringsIfPresent(javascriptConfigObject.get("key"), _filterKey);

		}

		private void assignLongIfPresent (final Object possibleLong, Long targetLongReference) {

			if (possibleLong != null) {
				targetLongReference = new Double(ScriptRuntime.toNumber(possibleLong)).longValue();
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
					_filterUserName.isEmpty() && _filterRelType.isEmpty() && _filterTarget.isEmpty() && _filterKey.isEmpty()
			);

			for (final String entry : changelog.split("\n")) {

				final JsonObject jsonObj = _jsonParser.parse(entry).getAsJsonObject();
				final String verb = jsonObj.get("verb").getAsString();
				final long time = jsonObj.get("time").getAsLong();
				final String userId = jsonObj.get("userId").getAsString();
				final String userName = jsonObj.get("userName").getAsString();
				final String relType = (jsonObj.has("rel") ? jsonObj.get("rel").getAsString() : null);
				final String target = (jsonObj.has("target") ? jsonObj.get("target").getAsString() : null);
				final String key = (jsonObj.has("key") ? jsonObj.get("key").getAsString() : null);

				if (doesFilterApply(verb, time, userId, userName, relType, target, key)) {

					final TreeMap<String, Object> obj = new TreeMap<>();

					obj.put("verb", verb);
					obj.put("time", time);
					obj.put("userId", userId);
					obj.put("userName", userName);

					switch (verb) {
						case "create":
						case "delete":
							obj.put("target", target);
							if (_resolveTargets) {
								obj.put("targetObj", _app.getNodeById(target));
							}
							list.add(obj);
							break;

						case "link":
						case "unlink":
							obj.put("rel", relType);
							obj.put("target", target);
							if (_resolveTargets) {
								obj.put("targetObj", _app.getNodeById(target));
							}
							list.add(obj);
							break;

						case "change":
							obj.put("key", key);
							obj.put("prev", _gson.toJson(jsonObj.get("prev")));
							obj.put("val", _gson.toJson(jsonObj.get("val")));
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

		public boolean doesFilterApply (final String verb, final long time, final String userId, final String userName, final String relType, final String target, final String key) {

			return (
				(_noFilterConfig == true) ||
				(
					(_filterVerbs.isEmpty()    || _filterVerbs.contains(verb)       ) &&
					(_filterTimeFrom == null   || _filterTimeFrom <= time           ) &&
					(_filterTimeTo == null     || _filterTimeTo >= time             ) &&
					(_filterUserId.isEmpty()   || _filterUserId.contains(userId)    ) &&
					(_filterUserName.isEmpty() || _filterUserName.contains(userName)) &&
					(_filterRelType.isEmpty()  || _filterRelType.contains(relType)  ) &&
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
