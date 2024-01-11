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
package org.structr.rest.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.logging.entity.LogEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 *
 */
public class LogResource extends ExactMatchEndpoint {

	private static final Logger logger                          = LoggerFactory.getLogger(LogResource.class.getName());
	private static final Pattern RangeQueryPattern              = Pattern.compile("\\[(.+) TO (.+)\\]");

	private static final String SUBJECTS                        = "/s/";
	private static final String CORRELATION_SEPARATOR           = "::";

	private static final Property<String> subjectProperty       = new StringProperty("subject");
	private static final Property<String> objectProperty        = new StringProperty("object");
	private static final Property<String> actionProperty        = new StringProperty("action");
	private static final Property<String> actionsProperty       = new StringProperty("actions");
	private static final Property<String> messageProperty       = new StringProperty("message");
	private static final Property<Integer> entryCountProperty   = new IntProperty("entryCount");
	private static final Property<Integer> totalProperty        = new IntProperty("total");
	private static final ISO8601DateProperty timestampProperty  = new ISO8601DateProperty("timestamp");
	private static final ISO8601DateProperty firstEntryProperty = new ISO8601DateProperty("firstEntry");
	private static final ISO8601DateProperty lastEntryProperty  = new ISO8601DateProperty("lastEntry");

	private static final Set<String> ReservedRequestParameters  = new LinkedHashSet<>(Arrays.asList(new String[]{"subject", "object", "action", "message", "timestamp", "aggregate", "histogram", "correlate"}));

	public static final String LOG_RESOURCE_URI                 = "log";

	public LogResource() {
		super(RESTParameter.forStaticString("log"));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new LogResourceHandler(securityContext, call);
	}

	private class LogResourceHandler extends RESTCallHandler {

		public LogResourceHandler(final SecurityContext securityContext, final RESTCall call) {

			super(securityContext, call);

			subjectProperty.setDeclaringClass(LogResource.class);
			objectProperty.setDeclaringClass(LogResource.class);
			actionProperty.setDeclaringClass(LogResource.class);
			messageProperty.setDeclaringClass(LogResource.class);
			timestampProperty.setDeclaringClass(LogResource.class);
		}

		@Override
		public Class<? extends GraphObject> getEntityClass() {
			return GraphObject.class;
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final HttpServletRequest request = securityContext.getRequest();
			if (request != null) {

				final String subjectId           = request.getParameter(subjectProperty.jsonName());
				final String objectId            = request.getParameter(objectProperty.jsonName());
				final GraphObjectMap overviewMap = new GraphObjectMap();
				final LogState logState          = new LogState(request);

				if (StringUtils.isNotEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

					processData(logState, StructrApp.getInstance(securityContext)
						.nodeQuery(LogEvent.class)
						.and(LogEvent.subjectProperty, subjectId)
						.and(LogEvent.objectProperty, objectId)
						.and(LogEvent.actionProperty, logState.logAction)
						.andRange(LogEvent.timestampProperty, new Date(logState.beginTimestamp()), new Date(logState.endTimestamp()))
						.getAsList()
					);

				} else if (StringUtils.isNotEmpty(subjectId) && StringUtils.isEmpty(objectId)) {

					processData(logState, StructrApp.getInstance(securityContext)
						.nodeQuery(LogEvent.class)
						.and(LogEvent.subjectProperty, subjectId)
						.and(LogEvent.actionProperty, logState.logAction)
						.andRange(LogEvent.timestampProperty, new Date(logState.beginTimestamp()), new Date(logState.endTimestamp()))
						.getAsList()
					);

				} else if (StringUtils.isEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

					logState.inverse(true);

					processData(logState, StructrApp.getInstance(securityContext)
						.nodeQuery(LogEvent.class)
						.and(LogEvent.objectProperty, objectId)
						.and(LogEvent.actionProperty, logState.logAction)
						.andRange(LogEvent.timestampProperty, new Date(logState.beginTimestamp()), new Date(logState.endTimestamp()))
						.getAsList()
					);

				} else if (logState.doActionQuery()) {

					processData(logState);

				} else {

					// create overview of existing logs
					logState.overview(true);

					processData(logState, StructrApp.getInstance(securityContext)
						.nodeQuery(LogEvent.class)
						.getAsList()
					);
				}

				if (logState.overview()) {

					overviewMap.put(actionsProperty, logState.actions());
					overviewMap.put(entryCountProperty, logState.actionCount());
					overviewMap.put(firstEntryProperty, new Date(logState.beginTimestamp()));
					overviewMap.put(lastEntryProperty, new Date(logState.endTimestamp()));

					return new PagingIterable<>(getURL(), Arrays.asList(overviewMap));

				} else if (logState.doHistogram()) {

					// aggregate results
					return histogram(logState);

				} else if (logState.doAggregate()) {

					// aggregate results
					return aggregate(logState);

				} else {

					// sort result
					logState.sortEntries();

					return new PagingIterable<>(getURL(), wrap(logState.entries()));
					//return new ResultStream(wrap(logState.entries()), true, false);
				}
			}

			// no request object, this is fatal
			throw new FrameworkException(500, "No request object present, aborting.");
		}

		@Override
		public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

			final HttpServletRequest request = securityContext.getRequest();
			if (request != null) {

				// initialize?!
				if ("true".equals(request.getParameter("initialize"))) {

					final String filesPath = Settings.FilesPath.getValue();

					try (final Context context  = new Context(1000)) {

						collectFilesAndStore(context, new File(filesPath + SUBJECTS).toPath(), 0);

					} catch (FrameworkException fex) {
						logger.warn("", fex);
					}

					return new RestMethodResult(200);
				}

				final String subjectId = (String) propertySet.get(subjectProperty.jsonName());
				final String objectId  = (String) propertySet.get(objectProperty.jsonName());
				final String action    = (String) propertySet.get(actionProperty.jsonName());
				final String message   = (String) propertySet.get(messageProperty.jsonName());

				if (subjectId != null && objectId != null && action != null) {

					final App app  = StructrApp.getInstance(securityContext);
					LogEvent event = null;

					try (final Tx tx = app.tx()) {

						final PropertyMap properties = new PropertyMap();
						properties.put(LogEvent.timestampProperty,           new Date());
						properties.put(LogEvent.actionProperty,              action);
						properties.put(LogEvent.subjectProperty,             subjectId);
						properties.put(LogEvent.objectProperty,              objectId);
						properties.put(LogEvent.messageProperty,             message);
						properties.put(LogEvent.visibleToPublicUsers,        true);
						properties.put(LogEvent.visibleToAuthenticatedUsers, true);

						event = app.create(LogEvent.class, properties);

						tx.success();
					}

					final RestMethodResult result = new RestMethodResult(201);
					result.addContent(event);

					return result;

				} else {

					final ErrorBuffer errorBuffer = new ErrorBuffer();

					if (StringUtils.isEmpty(subjectId)) {
						errorBuffer.add(new EmptyPropertyToken("LogFile", subjectProperty.jsonName()));
					}

					if (StringUtils.isEmpty(objectId)) {
						errorBuffer.add(new EmptyPropertyToken("LogFile", objectProperty.jsonName()));
					}

					if (StringUtils.isEmpty(action)) {
						errorBuffer.add(new EmptyPropertyToken("LogFile", actionProperty.jsonName()));
					}

					throw new FrameworkException(422, "Log entry must consist of at least subjectId, objectId and action", errorBuffer);
				}
			}

			// no request object, this is fatal
			throw new FrameworkException(500, "No request object present, aborting.");
		}

		@Override
		public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("PUT not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doDelete() throws FrameworkException {
			throw new IllegalMethodException("DELETE not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doHead() throws FrameworkException {
			throw new IllegalMethodException("HEAD not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doOptions() throws FrameworkException {

			final RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_OK);

			result.addHeader("Allow", "GET,POST,OPTIONS");

			return result;
		}

		@Override
		public boolean createPostTransaction() {
			return false;
		}

		// ----- private methods -----
		private void collectFilesAndStore(final Context context, final Path dir, final int level) throws FrameworkException {

			if (level == 1) {
				logger.info("Path {}", dir);
			}

			try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

				for (final Path p : stream) {

					if (Files.isDirectory(p)) {

						collectFilesAndStore(context, p, level+1);

					} else {

						context.update(storeLogEntry(p));

						// update object count and commit
						context.commit(true);
					}

					Files.delete(p);

				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

		private void processData(final LogState state) throws FrameworkException {

			if (state.doCorrelate()) {

				// get the basic correlation set (pds_click in the test case)
				final List<LogEvent> correlationResult = StructrApp.getInstance(securityContext)
					.nodeQuery(LogEvent.class)
					.and(LogEvent.actionProperty, state.correlationAction)
					.getAsList();

				for (final LogEvent entry : correlationResult) {

					final String pathSubjectId = state.inverse() ? entry.getObjectId() : entry.getSubjectId();
					final String pathObjectId  = state.inverse() ? entry.getSubjectId() : entry.getObjectId();
					final String entryMessage  = entry.getMessage();

					if (state.correlationPattern != null) {

						final Matcher matcher = state.correlationPattern.matcher(entryMessage);
						if (matcher.matches()) {

							state.addCorrelationEntry(matcher.group(1), entry);

						}

					} else {
						// fallback: subjectId and objectId
						state.addCorrelationEntry(key(pathSubjectId, pathObjectId), entry);
					}
				}
			}

			logger.debug("No. of correlations: {}", state.getCorrelations().entrySet().size());

			final List<LogEvent> result = StructrApp.getInstance(securityContext).nodeQuery(LogEvent.class)
				.and(LogEvent.actionProperty, state.logAction)
				.andRange(LogEvent.timestampProperty, new Date(state.beginTimestamp()), new Date(state.endTimestamp()))
				.getAsList();

			processData(state, result);
		}

		private void processData(final LogState state, final Iterable<LogEvent> result) throws FrameworkException {

			int count = 0;

			for (final LogEvent event : result) {

				final String pathSubjectId = state.inverse() ? event.getObjectId() : event.getSubjectId();
				final String pathObjectId  = state.inverse() ? event.getSubjectId() : event.getObjectId();
				final long timestamp       = event.getTimestamp();
				final String entryAction   = event.getAction();
				final String entryMessage  = event.getMessage();

				// determine first timestamp
				if (timestamp <= state.beginTimestamp()) {
					state.beginTimestamp(timestamp);
				}

				// determine last timestamp
				if (timestamp >= state.endTimestamp()) {
					state.endTimestamp(timestamp);
				}

				if (state.overview()) {

					if (entryAction != null) {

						state.countAction(entryAction);

					} else {

						state.countAction("null");
					}

				} else {

					// passes filter? action present or matching?
					if (state.passesFilter(entryMessage) && state.correlates(pathSubjectId, pathObjectId, entryMessage)) {

						final Map<String, Object> map = new HashMap<>();

						map.put(subjectProperty.jsonName(), pathSubjectId);
						map.put(objectProperty.jsonName(), pathObjectId);
						map.put(actionProperty.jsonName(), entryAction);
						map.put(timestampProperty.jsonName(), timestamp);
						map.put(messageProperty.jsonName(), entryMessage);

						state.addEntry(map);
					}
				}
			}
		}

		private int storeLogEntry(final Path path) throws IOException, FrameworkException {

			final App app          = StructrApp.getInstance(securityContext);
			final String fileName  = path.getFileName().toString();
			int count              = 0;

			if (fileName.length() == 64) {

				final String subjectId = fileName.substring(0, 32);
				final String objectId  = fileName.substring(32, 64);

				for (final String line : Files.readAllLines(path, Charset.forName("utf-8"))) {

					final int pos1               = line.indexOf(",", 14);

					final String part0           = line.substring(0, 13);
					final String part1           = line.substring(14, pos1);
					final String part2           = line.substring(pos1 + 1);

					final long timestamp         = Long.valueOf(part0);
					final String action          = part1;
					final String message         = part2;

					final PropertyMap properties = new PropertyMap();

					properties.put(LogEvent.messageProperty,             message);
					properties.put(LogEvent.actionProperty,              action);
					properties.put(LogEvent.subjectProperty,             subjectId);
					properties.put(LogEvent.objectProperty,              objectId);
					properties.put(LogEvent.timestampProperty,           new Date(timestamp));
					properties.put(LogEvent.visibleToPublicUsers,        true);
					properties.put(LogEvent.visibleToAuthenticatedUsers, true);

					app.create(LogEvent.class, properties);

					count++;
				}

			} else {

				logger.warn("Skipping entry {}", fileName);
			}

			return count;
		}

		private String getDirectoryPath(final String uuid, final int depth) {

			final StringBuilder buf = new StringBuilder();

			if (StringUtils.isNotEmpty(uuid) && uuid.length() > depth) {

				for (int i = 0; i < depth; i++) {

					buf.append(uuid.substring(i, i + 1));
					buf.append("/");
				}
			}

			return buf.toString();
		}

		private ResultStream aggregate(final LogState state) throws FrameworkException {

			// sort entries before aggregation
			state.sortEntries();

			final long startTimestamp                         = state.beginTimestamp();
			final long endTimestamp                           = state.endTimestamp();
			final GraphObjectMap result                       = new GraphObjectMap();
			final long interval                               = findInterval(state.aggregate());
			final long start                                  = alignDateOnFormat(state.aggregate(), startTimestamp);
			final TreeMap<Long, Map<String, Object>> countMap = toAggregatedCountMap(state);
			final Set<String> countProperties                 = getCountProperties(countMap);

			for (long current = start; current <= endTimestamp; current += interval) {

				final Map<Long, Map<String, Object>> counts = countMap.subMap(current, true, current + interval, false);
				final GraphObjectMap sum = new GraphObjectMap();

				// initialize interval sums with 0 (so each
				// interval contains all keys regardless of
				// whether there are actual values or not)
				for (final String key : countProperties) {
					sum.put(new IntProperty(key), 0);
				}

				// evaluate counts
				for (final Map<String, Object> count : counts.values()) {

					for (final String key : countProperties) {

						final IntProperty prop = new IntProperty(key);
						Integer sumValue = sum.get(prop);
						if (sumValue == null) {
							sumValue = 0;
						}

						Integer entryValue = (Integer) count.get(key);
						if (entryValue == null) {
							entryValue = 0;
						}

						sum.put(prop, sumValue + entryValue);
					}
				}

				result.put(new GenericProperty(Long.toString(current)), sum);
			}

			return new PagingIterable<>(getURL(), Arrays.asList(result));
		}

		private ResultStream histogram(final LogState state) throws FrameworkException {

			// sort entries before creating the histogram
			state.sortEntries();

			final String dateFormat                           = state.aggregate();
			final long startTimestamp                         = state.beginTimestamp();
			final long endTimestamp                           = state.endTimestamp();
			final GraphObjectMap result                       = new GraphObjectMap();
			final long interval                               = findInterval(dateFormat);
			final long start                                  = alignDateOnFormat(dateFormat, startTimestamp);
			final TreeMap<Long, Map<String, Object>> countMap = toHistogramCountMap(state);
			final Set<String> countProperties                 = getCountProperties(countMap);

			for (long current = start; current <= endTimestamp; current += interval) {

				final Map<Long, Map<String, Object>> counts = countMap.subMap(current, true, current + interval, false);
				final GraphObjectMap sum = new GraphObjectMap();

				// initialize interval sums with 0 (so each
				// interval contains all keys regardless of
				// whether there are actual values or not)
				for (final String key : countProperties) {
					sum.put(new IntProperty(key), 0);
				}

				// evaluate counts
				for (final Map<String, Object> count : counts.values()) {

					for (final String key : countProperties) {

						final IntProperty prop = new IntProperty(key);
						Integer sumValue = sum.get(prop);
						if (sumValue == null) {
							sumValue = 0;
						}

						Integer entryValue = (Integer) count.get(key);
						if (entryValue == null) {
							entryValue = 0;
						}

						sum.put(prop, sumValue + entryValue);
					}
				}

				result.put(new GenericProperty(Long.toString(current)), sum);
			}

			return new PagingIterable<>(getURL(), Arrays.asList(result));
		}

		private long alignDateOnFormat(final String dateFormat, final long timestamp) {

			try {

				final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
				return format.parse(format.format(timestamp)).getTime();

			} catch (ParseException pex) {
				logger.warn("", pex);
			}

			return 0L;
		}

		/**
		 * This method takes a date format and finds the time interval that it
		 * represents.
		 */
		private long findInterval(final String dateFormat) {

			final long max = TimeUnit.DAYS.toMillis(365);
			final long step = TimeUnit.SECONDS.toMillis(60);

			try {

				final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
				final long initial = format.parse(format.format(3600)).getTime();

				for (long i = initial; i < max; i += step) {

					final long current = format.parse(format.format(i)).getTime();

					if (initial != current) {
						return i - initial;
					}
				}

				return max;

			} catch (ParseException pex) {
				logger.warn("", pex);
			}

			return max;
		}

		private TreeMap<Long, Map<String, Object>> toAggregatedCountMap(final LogState state) throws FrameworkException {

			final TreeMap<Long, Map<String, Object>> countMap = new TreeMap<>();

			for (final Map<String, Object> entry : state.entries()) {

				final String message = (String) entry.get(messageProperty.jsonName());
				final long timestamp = (Long) entry.get(timestampProperty.jsonName());
				Map<String, Object> obj = countMap.get(timestamp);

				if (obj == null) {
					obj = new LinkedHashMap<>();
				}

				Integer count = (Integer) obj.get(totalProperty.jsonName());
				if (count == null) {
					count = 1;
				} else {
					count = count + 1;
				}
				obj.put(totalProperty.jsonName(), count);

				// iterate over patterns
				for (final Entry<String, Pattern> patternEntry : state.aggregationPatterns().entrySet()) {

					if (patternEntry.getValue().matcher(message).matches()) {

						final String key = patternEntry.getKey();

						final int multiplier = getMultiplier(message, state);

						Integer c = (Integer) obj.get(key);
						if (c == null) {
							c = multiplier;
						} else {
							c = c + multiplier;
						}

						obj.put(key, c);
					}
				}

				countMap.put(timestamp, obj);
			}

			return countMap;
		}

		private TreeMap<Long, Map<String, Object>> toHistogramCountMap(final LogState state) throws FrameworkException {

			final Pattern pattern = Pattern.compile(state.histogram());
			final Matcher matcher = pattern.matcher("");
			final TreeMap<Long, Map<String, Object>> countMap = new TreeMap<>();

			for (final Map<String, Object> entry : state.entries()) {

				final String message = (String) entry.get(messageProperty.jsonName());
				final long timestamp = (Long) entry.get(timestampProperty.jsonName());
				Map<String, Object> obj = countMap.get(timestamp);

				if (obj == null) {
					obj = new LinkedHashMap<>();
				}

				Integer count = (Integer) obj.get(totalProperty.jsonName());
				if (count == null) {
					count = 1;
				} else {
					count = count + 1;
				}
				obj.put(totalProperty.jsonName(), count);

				// iterate over patterns
				matcher.reset(message);
				if (matcher.matches()) {

					final String key = matcher.group(1);

					final int multiplier = getMultiplier(message, state);

					Integer c = (Integer) obj.get(key);
					if (c == null) {
						c = multiplier;
					} else {
						c = c + multiplier;
					}

					obj.put(key, c);
				}

				countMap.put(timestamp, obj);
			}

			return countMap;
		}

		private int getMultiplier(final String message, final LogState state) {

			int multiplier = 1;

			if (state.multiplier != null) {

				final Matcher matcher = Pattern.compile(state.multiplier).matcher(message);

				if (matcher.matches()) {

					final String g = matcher.group(1);
					multiplier = Integer.parseInt(g);
				}

			}

			return multiplier;

		}

		private Set<String> getCountProperties(final Map<Long, Map<String, Object>> entries) {

			final Set<String> result = new LinkedHashSet<>();

			for (final Map<String, Object> obj : entries.values()) {

				for (final Entry<String, Object> entry : obj.entrySet()) {

					// collect the key names of integer values
					if (entry.getValue() instanceof Integer) {
						result.add(entry.getKey());
					}
				}
			}

			return result;
		}

		private List<GraphObjectMap> wrap(final List<Map<String, Object>> entries) {

			final List<GraphObjectMap> result = new LinkedList<>();

			for (final Map<String, Object> entry : entries) {

				final GraphObjectMap map = new GraphObjectMap();
				for (final Entry<String, Object> e : entry.entrySet()) {

					final String key = e.getKey();

					if (timestampProperty.jsonName().equals(key)) {

						map.put(timestampProperty, new Date((Long) e.getValue()));

					} else {

						map.put(new GenericProperty(key), e.getValue());
					}
				}

				result.add(map);
			}

			return result;
		}

		private static class LogState {

			private final Map<String, Pattern> aggregationPatterns       = new HashMap<>();
			private final List<Map<String, Object>> entries              = new LinkedList<>();
			private final Map<String, LinkedList<LogEvent>> correlations = new ConcurrentHashMap<>();
			private final Map<String, Integer> actions                   = new HashMap<>();
			private long beginTimestamp                                  = Long.MAX_VALUE;
			private long endTimestamp                                    = 0L;
			private String logAction                                     = null;
			private String aggregate                                     = null;
			private String histogram                                     = null;
			private String multiplier                                    = null;
			private String correlate                                     = null;
			private String correlationAction                             = null;
			private String correlationOp                                 = null;
			private Pattern correlationPattern                           = null;
			private String[] filters                                     = null;
			private boolean inverse                                      = false;
			private boolean overview                                     = false;
			private Range range                                          = null;
			private int actionCount                                      = 0;
			private boolean doCorrelate                                  = false;

			public LogState(final HttpServletRequest request) {

				aggregationPatterns.putAll(getAggregationPatterns(request));

				this.logAction  = request.getParameter(actionProperty.jsonName());
				this.aggregate  = request.getParameter("aggregate");
				this.histogram  = request.getParameter("histogram");
				this.correlate  = request.getParameter("correlate");
				this.multiplier = request.getParameter("multiplier");
				this.filters    = getFilterPatterns(request);
				this.range      = getRange(request);

				if (StringUtils.isNotBlank(correlate)) {

					final String[] parts = correlate.split(CORRELATION_SEPARATOR);

					if (parts.length > 0) {
						correlationAction = parts[0];
					}

					if (parts.length > 1) {
						correlationOp = parts[1];
					}

					if (parts.length > 2) {
						correlationPattern = Pattern.compile(parts[2]);
					}

					doCorrelate = true;
				}

			}

			public List<Map<String, Object>> entries() {
				return entries;
			}

			public void addEntry(final Map<String, Object> entry) {
				entries.add(entry);
			}

			public void addCorrelationEntry(final String key, final LogEvent event) {

				logger.debug("No. of correllation entry lists: {}, adding action: {} {}", new Object[]{correlations.keySet().size(), key, event.getMessage()});

				LinkedList<LogEvent> existingEventList = correlations.get(key);

				if (existingEventList == null) {
					existingEventList = new LinkedList<>();
				}

				existingEventList.add(event);

				correlations.put(key, existingEventList);
			}

			public Map<String, LinkedList<LogEvent>> getCorrelations() {
				return correlations;
			}

			public Map<String, Integer> actions() {
				return actions;
			}

			public Map<String, Pattern> aggregationPatterns() {
				return aggregationPatterns;
			}

			public void countAction(final String action) {

				Integer actionCount = actions.get(action);
				if (actionCount == null) {

					actions.put(action, 1);

				} else {

					actions.put(action, actionCount + 1);
				}

				this.actionCount++;
			}

			public int actionCount() {
				return actionCount;
			}

			public boolean isRequestedActionOrNull(final String action) {
				return logAction == null || logAction.equals(action);
			}

			public void sortEntries() {
				Collections.sort(entries, new TimestampComparator());
			}

			public int size() {
				return entries.size();
			}

			public void inverse(final boolean inverse) {
				this.inverse = inverse;
			}

			public boolean inverse() {
				return inverse;
			}

			public void overview(final boolean overview) {
				this.overview = overview;
			}

			public boolean overview() {
				return overview;
			}

			public long beginTimestamp() {
				return range != null ? range.start : beginTimestamp;
			}

			public long endTimestamp() {
				return range != null ? range.end : endTimestamp;
			}

			public void beginTimestamp(final long beginTimestamp) {
				this.beginTimestamp = beginTimestamp;
			}

			public void endTimestamp(final long endTimestamp) {
				this.endTimestamp = endTimestamp;
			}

			public boolean isInRangeOrNull(final long timestamp) {
				return range == null || range.contains(timestamp);
			}

			public String histogram() {
				return histogram;
			}

			public String aggregate() {
				return aggregate;
			}

			public boolean passesFilter(final String message) {

				if (filters == null) {
					return true;
				}

				boolean passes = true;

				for (final String filter : filters) {

					passes &= Pattern.compile(filter).matcher(message).matches();

				}

				return passes;

			}

			public boolean correlates(final String pathSubjectId, final String pathObjectId, final String message) {

				if (correlations.isEmpty()) {

					return true;
				}

				LinkedList<LogEvent> correlationEntries;

				if (correlationOp != null && correlationPattern != null) {

					final Matcher matcher = correlationPattern.matcher(message);

					if (matcher.matches()) {

						final String value = matcher.group(1);

						switch (correlationOp) {

							case "and":

								return correlations.containsKey(value);

							case "andSubject":

								correlationEntries = correlations.get(value);

								if (correlationEntries != null) {

									for (LogEvent correlationEntry : correlationEntries) {

										if (correlationEntry.getSubjectId().equals(pathSubjectId)) {

											return true;

										}

									}

								}

								return false;

							case "andObject":

								correlationEntries = correlations.get(value);

								if (correlationEntries != null) {

									for (LogEvent correlationEntry : correlationEntries) {

										if (correlationEntry.getObjectId().equals(pathObjectId)) {

											return true;

										}

									}

								}

								return false;

							case "not":

								return !correlations.containsKey(value);

							default:
								return false;
						}

					}

					return "not".equals(correlationOp);

				} else {

					// fallback
					return correlations.containsKey(key(pathSubjectId, pathObjectId));

				}

			}

			public boolean isCorrelatedAction(final String input) {
				return (correlationAction == null || correlationAction.equals(input));
			}

			public boolean doHistogram() throws FrameworkException {

				if (StringUtils.isNotBlank(histogram)) {

					if (StringUtils.isBlank(aggregate)) {
						throw new FrameworkException(400, "To use the histogram function, please supply an aggregation pattern.");
					}

					return true;
				}

				return false;
			}

			public boolean doAggregate() {
				return StringUtils.isNotBlank(aggregate);
			}

			public boolean doCorrelate() {
				return doCorrelate;
			}

			public boolean doActionQuery() {
				return StringUtils.isNotBlank(logAction);
			}

			public boolean includeFile(final File file) {
				return range == null || range.contains(file.lastModified());
			}

			// ----- private methods -----
			private Range getRange(final HttpServletRequest request) {

				final String value = request.getParameter(timestampProperty.jsonName());
				if (value != null) {

					if (StringUtils.startsWith(value, "[") && StringUtils.endsWith(value, "]")) {

						// check for existance of range query string
						Matcher matcher = RangeQueryPattern.matcher(value);
						if (matcher.matches()) {

							if (matcher.groupCount() == 2) {

								final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
								String rangeStart = matcher.group(1);
								String rangeEnd = matcher.group(2);

								try {

									final Date startDate = parser.parse(rangeStart);
									final Date endDate = parser.parse(rangeEnd);

									return new Range(startDate.getTime(), endDate.getTime());

								} catch (ParseException pex) {

									logger.warn("", pex);
								}
							}
						}
					}

				}

				return null;
			}

			private Map<String, Pattern> getAggregationPatterns(final HttpServletRequest request) {

				final Map<String, Pattern> patterns = new LinkedHashMap<>();

				for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

					final String key = entry.getKey();
					final String[] value = entry.getValue();

					if (value.length > 0 && !ReservedRequestParameters.contains(key)) {
						patterns.put(key, Pattern.compile(value[0]));
					}
				}

				return patterns;
			}

			private String[] getFilterPatterns(final HttpServletRequest request) {

				final String filterString = request.getParameter("filters");
				if (StringUtils.isNotBlank(filterString)) {
					return filterString.split(CORRELATION_SEPARATOR);
				}

				return null;
			}
		}

		private static String key(final String subjectId, final String objectId) {

			if (subjectId != null && objectId != null) {
				return subjectId.concat(objectId);
			}

			return "NULLNULL";
		}

		private static class Range {

			private long start = 0L;
			private long end = 0L;

			public Range(final long start, final long end) {
				this.start = start;
				this.end = end;
			}

			public boolean contains(final long timestamp) {
				return timestamp >= start && timestamp <= end;
			}
		}

		private static class TimestampComparator implements Comparator<Map<String, Object>> {

			@Override
			public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {

				final Long timestamp1 = (Long) o1.get(timestampProperty.jsonName());
				final Long timestamp2 = (Long) o2.get(timestampProperty.jsonName());

				return timestamp1.compareTo(timestamp2);
			}
		}

		private static class Context implements AutoCloseable {

			private final App app   = StructrApp.getInstance();
			private Tx tx           = null;
			private int total       = 0;
			private int count       = 0;
			private int commitCount = 0;

			public Context(final int commitCount) throws FrameworkException {

				this.commitCount = commitCount;
				this.tx          = app.tx(false, false, false);
			}

			public void commit(final boolean intermediate) throws FrameworkException {

				if (count > commitCount) {

					logger.info("Committing transaction after {} objects..", total);

					tx.success();
					tx.close();

					if (intermediate) {

						tx = app.tx(false, false, false);
					}

					count = 0;
				}
			}

			public int getTotal() {
				return total;
			}

			public void update(final int count) {
				this.count += count;
				this.total += count;
			}

			@Override
			public void close() throws FrameworkException {

				tx.success();
				tx.close();
			}
		}
	}
}
