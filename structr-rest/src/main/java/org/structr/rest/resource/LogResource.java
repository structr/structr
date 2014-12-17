/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class LogResource extends Resource {

	private static final Logger logger = Logger.getLogger(LogResource.class.getName());
	private static final Pattern RangeQueryPattern = Pattern.compile("\\[(.+) TO (.+)\\]");

	private static final String SUBJECTS = "/s/";
	private static final String OBJECTS = "/o/";
	private static final String CORRELATION_SEPARATOR = "::";

	private static final Property<String> subjectProperty = new StringProperty("subject");
	private static final Property<String> objectProperty = new StringProperty("object");
	private static final Property<String> actionProperty = new StringProperty("action");
	private static final Property<String> actionsProperty = new StringProperty("actions");
	private static final Property<String> messageProperty = new StringProperty("message");
	private static final Property<Integer> entryCountProperty = new IntProperty("entryCount");
	private static final Property<Integer> totalProperty = new IntProperty("total");
	private static final ISO8601DateProperty timestampProperty = new ISO8601DateProperty("timestamp");
	private static final ISO8601DateProperty firstEntryProperty = new ISO8601DateProperty("firstEntry");
	private static final ISO8601DateProperty lastEntryProperty = new ISO8601DateProperty("lastEntry");

	private static final Set<String> ReservedRequestParameters = new LinkedHashSet<>(Arrays.asList(new String[]{"subject", "object", "action", "message", "timestamp", "aggregate", "histogram", "correlate"}));

	public static final String LOG_RESOURCE_URI = "log";

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		subjectProperty.setDeclaringClass(LogResource.class);
		objectProperty.setDeclaringClass(LogResource.class);
		actionProperty.setDeclaringClass(LogResource.class);
		messageProperty.setDeclaringClass(LogResource.class);
		timestampProperty.setDeclaringClass(LogResource.class);

		this.securityContext = securityContext;
		this.securityContext.setRequest(request);

		return LOG_RESOURCE_URI.equals(part);
	}

	@Override
	public String getUriPart() {
		return LOG_RESOURCE_URI;
	}

	@Override
	public Class<? extends GraphObject> getEntityClass() {
		return GraphObject.class;
	}

	@Override
	public String getResourceSignature() {
		return "Log";
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return true;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			final String subjectId = request.getParameter(subjectProperty.jsonName());
			final String objectId = request.getParameter(objectProperty.jsonName());
			final GraphObjectMap overviewMap = new GraphObjectMap();
			final LogState logState = new LogState(request);

			if (StringUtils.isNotEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

				final String fileName = mergeIds(subjectId, objectId);
				final String path = getDirectoryPath(fileName, 8);
				final Path filePath = new File(filesPath + SUBJECTS + path + fileName).toPath();

				if (Files.exists(filePath)) {
					read(toMap(filePath, logState), logState);
				}

			} else if (StringUtils.isNotEmpty(subjectId) && StringUtils.isEmpty(objectId)) {

				final String path = getDirectoryPath(subjectId, 8);
				final Path directoryPath = new File(filesPath + SUBJECTS + path).toPath();

				try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, subjectId + "????????????????????????????????")) {

					final Map<String, ArrayList<String>> fileLines = new LinkedHashMap<>();

					for (final Path p : stream) {
						fileLines.putAll(toMap(p, logState));
					}

					read(fileLines, logState);

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}

			} else if (StringUtils.isEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

				logState.inverse(true);

				final String path = getDirectoryPath(objectId, 8);
				final Path directoryPath = new File(filesPath + OBJECTS + path).toPath();

				try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, objectId + "????????????????????????????????")) {

					final Map<String, ArrayList<String>> fileLines = new LinkedHashMap<>();

					for (final Path p : stream) {
						fileLines.putAll(toMap(p, logState));
					}

					read(fileLines, logState);

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}

			} else if (logState.doActionQuery()) {

				final Map<String, ArrayList<String>> fileLines = collectFiles(new File(filesPath + SUBJECTS).toPath(), logState);

				read(fileLines, logState);

			} else {

				// create overview of existing logs
				logState.overview(true);

				final Map<String, ArrayList<String>> fileLines = collectFiles(new File(filesPath + SUBJECTS).toPath(), logState);

				read(fileLines, logState);
			}

			if (logState.overview()) {

				overviewMap.put(actionsProperty, logState.actions());
				overviewMap.put(entryCountProperty, logState.actionCount());
				overviewMap.put(firstEntryProperty, new Date(logState.beginTimestamp()));
				overviewMap.put(lastEntryProperty, new Date(logState.endTimestamp()));

				return new Result(overviewMap, false);

			} else if (logState.doHistogram()) {

				// aggregate results
				return histogram(logState);

			} else if (logState.doAggregate()) {

				// aggregate results
				return aggregate(logState);

			} else {

				// sort result
				logState.sortEntries();

				return new Result(wrap(logState.entries()), logState.size(), true, false);
			}
		}

		// no request object, this is fatal
		throw new FrameworkException(500, "No request object present, aborting.");
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			final String subjectId = (String) propertySet.get(subjectProperty.jsonName());
			final String objectId = (String) propertySet.get(objectProperty.jsonName());
			final String action = (String) propertySet.get(actionProperty.jsonName());
			final String message = (String) propertySet.get(messageProperty.jsonName());

			if (subjectId != null && objectId != null && action != null) {

				try {

					final StringBuilder data = new StringBuilder();
					data.append(System.currentTimeMillis());
					data.append(",");
					data.append(action);
					data.append(",");
					data.append(message);
					data.append("\n");

					// write data and create link
					final Path actualPath = write(filesPath + SUBJECTS, mergeIds(subjectId, objectId), data.toString());
					link(filesPath + OBJECTS, mergeIds(objectId, subjectId), actualPath);

					return new RestMethodResult(200);

				} catch (IOException ioex) {

					ioex.printStackTrace();
					throw new FrameworkException(500, ioex.getMessage());
				}

			} else {

				final ErrorBuffer errorBuffer = new ErrorBuffer();

				if (StringUtils.isEmpty(subjectId)) {
					errorBuffer.add("LogFile", new EmptyPropertyToken(subjectProperty));
				}

				if (StringUtils.isEmpty(objectId)) {
					errorBuffer.add("LogFile", new EmptyPropertyToken(objectProperty));
				}

				if (StringUtils.isEmpty(action)) {
					errorBuffer.add("LogFile", new EmptyPropertyToken(actionProperty));
				}

				throw new FrameworkException(422, errorBuffer);
			}
		}

		// no request object, this is fatal
		throw new FrameworkException(500, "No request object present, aborting.");
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new IllegalMethodException();
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
	private Map<String, ArrayList<String>> collectFiles(final Path dir, final LogState state) {

		final Map<String, ArrayList<String>> fileLines = new LinkedHashMap<>();
		final List<Path> paths = new LinkedList<>();

		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*")) {

			for (final Path p : stream) {

				if (Files.isDirectory(p)) {

					fileLines.putAll(collectFiles(p, state));

				} else {

					paths.add(p);
				}

			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		for (final Path path : paths) {

			fileLines.putAll(toMap(path, state));

		}

		return fileLines;

	}

	private Map<String, ArrayList<String>> toMap(final Path path, final LogState state) {

		final Map<String, ArrayList<String>> fileLines = new LinkedHashMap<>();
		try (final BufferedReader reader = Files.newBufferedReader(path, Charset.forName("utf-8"))) {

			// skip older files
			if (!state.includeFile(path.toFile())) {
				return Collections.EMPTY_MAP;
			}

			final String fileName = path.getFileName().toString();
			if (fileName.length() != 64) {

				logger.log(Level.WARNING, "Invalid log file name {0}, ignoring.", path.toAbsolutePath().getFileName());
				return Collections.EMPTY_MAP;
			}

			String l;

			final ArrayList<String> lines = new ArrayList<>();

			// Read rows
			while ((l = reader.readLine()) != null) {
				lines.add(l);
			}

			fileLines.put(fileName, lines);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return fileLines;

	}

	private void read(final Map<String, ArrayList<String>> fileLines, final LogState state) {

		for (final String fileName : fileLines.keySet()) {

			final String pathSubjectId = state.inverse() ? fileName.substring(32, 64) : fileName.substring(0, 32);
			final String pathObjectId = state.inverse() ? fileName.substring(0, 32) : fileName.substring(32, 64);

			if (state.doCorrelate()) {

				for (String line : fileLines.get(fileName)) {

					try {

						final int pos0 = line.indexOf(",");
						final int pos1 = line.indexOf(",", pos0 + 1);

						final String part0 = line.substring(0, pos0);
						final String part1 = line.substring(pos0 + 1, pos1);
						final String part2 = line.substring(pos1 + 1);

//						final long timestamp = Long.valueOf(part0);
						final String entryAction = part1;
						final String entryMessage = part2;

//						// determine first timestamp
//						if (timestamp <= state.beginTimestamp()) {
//							state.beginTimestamp(timestamp);
//						}
//
//						// determine last timestamp
//						if (timestamp >= state.endTimestamp()) {
//							state.endTimestamp(timestamp);
//						}
						// If correlation with another action was requested,
						// and the entry matches this action, store entry
						if (state.isCorrelatedAction(entryAction)) {

							if (state.correlationPattern != null) {

								final Matcher matcher = state.correlationPattern.matcher(line);

								if (matcher.matches()) {

									state.addCorrelationEntry(matcher.group(1), entryMessage);

								}

							} else {
								// fallback: subjectId and objectId
								state.addCorrelationEntry(key(pathSubjectId, pathObjectId), entryMessage);
							}

						}

					} catch (Throwable t) {
						t.printStackTrace();
					}

				}

			}

		}

		logger.log(Level.FINE, "No. of correlations: {0}", state.getCorrelations().entrySet().size());

		for (final String fileName : fileLines.keySet()) {

			final String pathSubjectId = state.inverse() ? fileName.substring(32, 64) : fileName.substring(0, 32);
			final String pathObjectId = state.inverse() ? fileName.substring(0, 32) : fileName.substring(32, 64);

			for (String line : fileLines.get(fileName)) {

				try {

					final int pos0 = line.indexOf(",");
					final int pos1 = line.indexOf(",", pos0 + 1);

					final String part0 = line.substring(0, pos0);
					final String part1 = line.substring(pos0 + 1, pos1);
					final String part2 = line.substring(pos1 + 1);

					final long timestamp = Long.valueOf(part0);
					final String entryAction = part1;
					final String entryMessage = part2;

					// determine first timestamp
					if (timestamp <= state.beginTimestamp()) {
						state.beginTimestamp(timestamp);
					}

					// determine last timestamp
					if (timestamp >= state.endTimestamp()) {
						state.endTimestamp(timestamp);
					}

					if (state.overview()) {

						state.countAction(entryAction);

					} else {

						// action present or matching?
						if (state.isRequestedActionOrNull(entryAction)) {

							final Map<String, Object> map = new LinkedHashMap<>();
							map.put(subjectProperty.jsonName(), pathSubjectId);
							map.put(objectProperty.jsonName(), pathObjectId);
							map.put(actionProperty.jsonName(), entryAction);
							map.put(timestampProperty.jsonName(), timestamp);
							map.put(messageProperty.jsonName(), entryMessage);

							// date predicate present?
							if (state.isInRangeOrNull(timestamp) && state.correlates(key(pathSubjectId, pathObjectId), entryMessage)) {
								state.addEntry(map);
							}
						}

					}

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

	}

	private Path write(final String basePath, final String fileName, final String data) throws IOException {

		final String path = getDirectoryPath(fileName, 8);
		final Path directoryPath = new File(basePath + path).toPath();
		final Path filePath = new File(basePath + path + fileName).toPath();

		if (!Files.exists(filePath)) {
			Files.createDirectories(directoryPath);
		}

		final FileOutputStream fos = new FileOutputStream(filePath.toString(), true);
		final FileChannel channel = fos.getChannel();

		channel.write(ByteBuffer.wrap(data.getBytes()));

		fos.flush();
		fos.close();

		return filePath;
	}

	private void link(final String basePath, final String fileName, final Path existing) {

		try {
			final String path = getDirectoryPath(fileName, 8);
			final Path directoryPath = new File(basePath + path).toPath();
			final Path filePath = new File(basePath + path + fileName).toPath();

			if (Files.notExists(filePath)) {
				Files.createDirectories(directoryPath);
			}

			if (Files.notExists(filePath)) {
				Files.createLink(filePath, existing);
			}

		} catch (IOException ignore) {

			// can be safely ignored because the actual work (linking
			// the files) is already done when an exception is thrown
		}
	}

	private String mergeIds(final String id1, final String id2) {

		final StringBuilder buf = new StringBuilder();

		buf.append(id1);
		buf.append(id2);

		return buf.toString();
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

	private Result aggregate(final LogState state) throws FrameworkException {

		// sort entries before aggregation
		state.sortEntries();

		final long startTimestamp = state.beginTimestamp();
		final long endTimestamp = state.endTimestamp();
		final GraphObjectMap result = new GraphObjectMap();
		final long interval = findInterval(state.aggregate());
		final long start = alignDateOnFormat(state.aggregate(), startTimestamp);
		final TreeMap<Long, Map<String, Object>> countMap = toAggregatedCountMap(state);
		final Set<String> countProperties = getCountProperties(countMap);

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

		return new Result(result, false);
	}

	private Result histogram(final LogState state) throws FrameworkException {

		// sort entries before creating the histogram
		state.sortEntries();

		final String dateFormat = state.aggregate();
		final long startTimestamp = state.beginTimestamp();
		final long endTimestamp = state.endTimestamp();
		final GraphObjectMap result = new GraphObjectMap();
		final long interval = findInterval(dateFormat);
		final long start = alignDateOnFormat(dateFormat, startTimestamp);
		final TreeMap<Long, Map<String, Object>> countMap = toHistogramCountMap(state);
		final Set<String> countProperties = getCountProperties(countMap);

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

		return new Result(result, false);
	}

	private long alignDateOnFormat(final String dateFormat, final long timestamp) {

		try {

			final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
			return format.parse(format.format(timestamp)).getTime();

		} catch (ParseException pex) {
			pex.printStackTrace();
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
			pex.printStackTrace();
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

		Integer multiplier = 1;

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

		private final Map<String, Pattern> aggregationPatterns = new LinkedHashMap<>();
		private final List<Map<String, Object>> entries = new LinkedList<>();
		private final Map<String, String> correlations = new ConcurrentHashMap<>();
		private final Map<String, Integer> actions = new LinkedHashMap<>();
		private long beginTimestamp = Long.MAX_VALUE;
		private long endTimestamp = 0L;
		private String logAction = null;
		private String aggregate = null;
		private String histogram = null;
		private String multiplier = null;
		private String correlate = null;
		private String correlationAction = null;
		private String correlationOp = null;
		private Pattern correlationPattern = null;
		private boolean inverse = false;
		private boolean overview = false;
		private Range range = null;
		private int actionCount = 0;

		public LogState(final HttpServletRequest request) {

			aggregationPatterns.putAll(getAggregationPatterns(request));

			this.logAction = request.getParameter(actionProperty.jsonName());
			this.aggregate = request.getParameter("aggregate");
			this.histogram = request.getParameter("histogram");
			this.correlate = request.getParameter("correlate");
			this.multiplier = request.getParameter("multiplier");
			this.range = getRange(request);

		}

		public List<Map<String, Object>> entries() {
			return entries;
		}

		public void addEntry(final Map<String, Object> entry) {
			entries.add(entry);
		}

		public void addCorrelationEntry(final String key, final String message) {

			logger.log(Level.FINE, "No. of correllation entries: {0}, adding action: {1} {2}", new Object[]{correlations.keySet().size(), key, message});

			correlations.put(key, message != null ? message : "");
		}

		public Map<String, String> getCorrelations() {
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

		public boolean correlates(final String key, final String message) {

			if (correlations.isEmpty()) {

				return true;

			}

			if (correlationOp != null && correlationPattern != null) {

				final Matcher matcher = correlationPattern.matcher(message);

				if (matcher.matches()) {
					
					final String value = matcher.group(1);

					switch (correlationOp) {

						case "and":

							return correlations.containsKey(value);

						case "not":

							return !correlations.containsKey(value);

						default:
							return false;
					}

				}
				
				return "not".equals(correlationOp);

			} else {

				// fallback
				return correlations.containsKey(key);

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

				return true;

			}

			return false;
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

							final SimpleDateFormat parser = new SimpleDateFormat(ISO8601DateProperty.PATTERN);
							String rangeStart = matcher.group(1);
							String rangeEnd = matcher.group(2);

							try {

								final Date startDate = parser.parse(rangeStart);
								final Date endDate = parser.parse(rangeEnd);

								return new Range(startDate.getTime(), endDate.getTime());

							} catch (ParseException pex) {

								pex.printStackTrace();
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
	}

	private static String key(final String subjectId, final String objectId) {
		return subjectId.concat(objectId);
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
}
