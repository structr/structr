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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.helpers.Predicate;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.RangeSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
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
 */
public class LogResource extends Resource {

	private static final Logger logger = Logger.getLogger(LogResource.class.getName());

	private static final String SUBJECTS = "/s/";
	private static final String OBJECTS  = "/o/";

	private static final Property<String>    subjectProperty    = new StringProperty("subject");
	private static final Property<String>    objectProperty     = new StringProperty("object");
	private static final Property<String>    actionProperty     = new StringProperty("action");
	private static final Property<String>    actionsProperty    = new StringProperty("actions");
	private static final Property<String>    messageProperty    = new StringProperty("message");
	private static final Property<Integer>   entryCountProperty = new IntProperty("entryCount");
	private static final Property<Integer>   totalProperty      = new IntProperty("total");
	private static final ISO8601DateProperty timestampProperty  = new ISO8601DateProperty("timestamp");
	private static final ISO8601DateProperty firstEntryProperty = new ISO8601DateProperty("firstEntry");
	private static final ISO8601DateProperty lastEntryProperty  = new ISO8601DateProperty("lastEntry");

	private static final Set<String> ReservedRequestParameters = new LinkedHashSet<>(Arrays.asList( new String[] { "subject", "object", "action", "message", "timestamp", "aggregate" } ));

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

			final String filesPath             = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			final String subjectId             = request.getParameter(subjectProperty.jsonName());
			final String objectId              = request.getParameter(objectProperty.jsonName());
			final String action                = request.getParameter(actionProperty.jsonName());
			final GraphObjectMap overviewMap   = new GraphObjectMap();
			final Map<String, Integer> actions = new LinkedHashMap<>();
			final List<Path> files             = new LinkedList<>();
			final String aggregate             = request.getParameter("aggregate");

			boolean overview    = false;
			boolean inverse     = false;
			long beginTimestamp = Long.MAX_VALUE;
			long endTimestamp   = 0L;
			int entryCount      = 0;

			if (StringUtils.isNotEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

				final String fileName       = mergeIds(subjectId, objectId);
				final String path           = getDirectoryPath(fileName, 8);
				final Path filePath         = new File(filesPath + SUBJECTS + path + fileName).toPath();

				if (Files.exists(filePath)) {
					files.add(filePath);
				}


			} else if (StringUtils.isNotEmpty(subjectId) && StringUtils.isEmpty(objectId)) {

				final String path           = getDirectoryPath(subjectId, 8);
				final Path directoryPath    = new File(filesPath + SUBJECTS + path).toPath();

				try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, subjectId + "????????????????????????????????")) {

					for (final Path p : stream) {
						files.add(p);
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}

			} else if (StringUtils.isEmpty(subjectId) && StringUtils.isNotEmpty(objectId)) {

				inverse = true;

				final String path           = getDirectoryPath(objectId, 8);
				final Path directoryPath    = new File(filesPath + OBJECTS + path).toPath();

				try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, objectId + "????????????????????????????????")) {

					for (final Path p : stream) {
						files.add(p);
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}

			} else if (StringUtils.isNotEmpty(action)) {

				collectFiles(new File(filesPath + SUBJECTS).toPath(), files);

			} else {

				collectFiles(new File(filesPath + SUBJECTS).toPath(), files);

				// create overview of existing logs
				overview = true;

			}

			final List<GraphObject> entries = new LinkedList<>();
			final Query query               = getTimestampQuery();
			final Range<Long> range         = getRangeFromQuery(query);
			final Predicate datePredicate   = query.toPredicate();

			for (final Path path : files) {

				try (final BufferedReader reader = Files.newBufferedReader(path, Charset.forName("utf-8"))) {

					final String fileName = path.getFileName().toString();
					String pathSubjectId  = inverse ? fileName.substring(32, 64) : fileName.substring(0, 32);
					String pathObjectId   = inverse ? fileName.substring(0, 32)  : fileName.substring(32, 64);

					String line = reader.readLine();
					while (line != null) {

						try {

							final int pos0            = line.indexOf(",");
							final int pos1            = line.indexOf(",", pos0+1);

							final String part0        = line.substring(0, pos0);
							final String part1        = line.substring(pos0+1, pos1);
							final String part2        = line.substring(pos1+1);

							final long timestamp      = Long.valueOf(part0);
							final Date date           = new Date(timestamp);
							final String entryAction  = part1;
							final String entryMessage = part2;

							// determine first timestamp
							if (timestamp <= beginTimestamp) {
								beginTimestamp = timestamp;
							}

							// determine last timestamp
							if (timestamp >= endTimestamp) {
								endTimestamp = timestamp;
							}

							if (overview) {

								Integer actionCount = actions.get(entryAction);
								if (actionCount == null) {

									actions.put(entryAction, 1);

								} else {

									actions.put(entryAction, actionCount + 1);
								}

								entryCount++;

							} else {

								// action present or matching?
								if (action == null || action.equals(entryAction)) {

									final GraphObjectMap map = new GraphObjectMap();
									map.put(subjectProperty, pathSubjectId);
									map.put(objectProperty, pathObjectId);
									map.put(actionProperty, entryAction);
									map.put(timestampProperty, date);
									map.put(messageProperty, entryMessage);

									// date predicate present?
									if (date == null || datePredicate.accept(map)) {
										entries.add(map);
									}
								}
							}

						} catch (Throwable t) {
							t.printStackTrace();
						}

						line = reader.readLine();
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}

			if (overview) {

				overviewMap.put(actionsProperty, actions);
				overviewMap.put(entryCountProperty, entryCount);
				overviewMap.put(firstEntryProperty, new Date(beginTimestamp));
				overviewMap.put(lastEntryProperty, new Date(endTimestamp));

				return new Result(overviewMap, false);

			} else if (StringUtils.isNotBlank(aggregate)) {

				final Map<String, Pattern> aggregationPatterns = getAggregationPatterns(request);

				// sort result
				Collections.sort(entries, new GraphObjectComparator(timestampProperty, false));

				final long intervalStart = range != null ? range.start : beginTimestamp;
				final long intervalEnd   = range != null ? range.end : endTimestamp;

				// aggregate results
				return aggregate(entries, aggregate, intervalStart, intervalEnd, aggregationPatterns);

			} else {

				// sort result
				Collections.sort(entries, new GraphObjectComparator(timestampProperty, false));

				return new Result(entries, entries.size(), true, false);
			}
		}

		// no request object, this is fatal
		throw new FrameworkException(500, "No request object present, aborting.");
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final String filesPath  = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			final String subjectId  = (String)propertySet.get(subjectProperty.jsonName());
			final String objectId   = (String)propertySet.get(objectProperty.jsonName());
			final String action     = (String)propertySet.get(actionProperty.jsonName());
			final String message    = (String)propertySet.get(messageProperty.jsonName());

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
	private void collectFiles(final Path dir, final List<Path> files) {

		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*")) {

			for (final Path p : stream) {

				if (Files.isDirectory(p)) {

					collectFiles(p, files);

				} else {

					files.add(p);
				}

			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

	}

	private Path write(final String basePath, final String fileName, final String data) throws IOException {

		final String path           = getDirectoryPath(fileName, 8);
		final Path directoryPath    = new File(basePath + path).toPath();
		final Path filePath         = new File(basePath + path + fileName).toPath();

		if (!Files.exists(filePath)) {
			Files.createDirectories(directoryPath);
		}

		final FileOutputStream fos = new FileOutputStream(filePath.toString(), true);
		final FileChannel channel  = fos.getChannel();

		channel.write(ByteBuffer.wrap(data.getBytes()));

		fos.flush();
		fos.close();

		return filePath;
	}

	private void link(final String basePath, final String fileName, final Path existing) throws IOException {

		final String path           = getDirectoryPath(fileName, 8);
		final Path directoryPath    = new File(basePath + path).toPath();
		final Path filePath         = new File(basePath + path + fileName).toPath();

		if (Files.notExists(filePath)) {
			Files.createDirectories(directoryPath);
		}

		if (Files.notExists(filePath)) {
			Files.createLink(filePath, existing);
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

			for (int i=0; i<depth; i++) {

				buf.append(uuid.substring(i, i+1));
				buf.append("/");
			}
		}

		return buf.toString();
	}

	private Query getTimestampQuery() throws FrameworkException {

		final Query dummyQuery = StructrApp.getInstance(securityContext).nodeQuery();
		timestampProperty.extractSearchableAttribute(securityContext, securityContext.getRequest(), dummyQuery);

		return dummyQuery;
	}

	private Range<Long> getRangeFromQuery(final Query query) throws FrameworkException {

		final SearchAttributeGroup rootGroup = query.getRootAttributeGroup();
		final RangeSearchAttribute range     = findRange(rootGroup);

		if (range != null) {

			final Object start = range.getRangeStart();
			final Object end   = range.getRangeEnd();

			if (start instanceof Date && end instanceof Date) {
				return new Range<>(((Date)start).getTime(), ((Date)end).getTime());
			}
		}

		return null;
	}

	private RangeSearchAttribute findRange(final SearchAttributeGroup group) {

		for (final SearchAttribute attr : group.getSearchAttributes()) {

			if (attr instanceof RangeSearchAttribute) {
				return (RangeSearchAttribute)attr;
			}

			if (attr instanceof SearchAttributeGroup) {

				final RangeSearchAttribute result = findRange((SearchAttributeGroup)attr);
				if (result != null) {

					return result;
				}
			}
		}

		return null;
	}

	private Result aggregate(final List<GraphObject> entries, final String dateFormat, final long startTimestamp, final long endTimestamp, final Map<String, Pattern> aggregationPatterns) throws FrameworkException {

		final GraphObjectMap result               = new GraphObjectMap();
		final long interval                       = findInterval(dateFormat);
		final long start                          = alignDateOnFormat(dateFormat, startTimestamp);
		final TreeMap<Long, GraphObject> countMap = toCountMap(entries, aggregationPatterns);
		final Set<IntProperty> countProperties    = getCountProperties(countMap);

		for (long current = start; current <= endTimestamp; current += interval) {

			final Map<Long, GraphObject> counts = countMap.subMap(current, true, current+interval, false);
			final GraphObject sum               = new GraphObjectMap();

			// initialize interval sums with 0 (so each
			// interval contains all keys regardless of
			// whether there are actual values or not)
			for (final IntProperty key : countProperties) {
				sum.setProperty(key, 0);
			}

			// evaluate counts
			for (final GraphObject count : counts.values()) {

				for (final IntProperty key : countProperties) {

					Integer sumValue   = sum.getProperty(key);
					if (sumValue == null) {
						sumValue = 0;
					}

					Integer entryValue = count.getProperty(key);
					if (entryValue == null) {
						entryValue = 0;
					}

					sum.setProperty(key, sumValue + entryValue);
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
	 * This method takes a date format and finds the time interval
	 * that it represents.
	 */
	private long findInterval(final String dateFormat) {


		final long max  = TimeUnit.DAYS.toMillis(365);
		final long step = TimeUnit.SECONDS.toMillis(60);

		try {

			final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
			final long initial            = format.parse(format.format(3600)).getTime();

			for (long i=initial; i<max; i+=step) {

				final long current = format.parse(format.format(i)).getTime();

				if (initial != current) {
					return i-initial;
				}
			}

			return max;

		} catch (ParseException pex) {
			pex.printStackTrace();
		}

		return max;
	}

	private TreeMap<Long, GraphObject> toCountMap(final List<GraphObject> entries, final Map<String, Pattern> aggregationPatterns) throws FrameworkException {

		final TreeMap<Long, GraphObject> countMap = new TreeMap<>();

		for (final GraphObject entry : entries) {

			final String message = entry.getProperty(messageProperty);
			final long timestamp = entry.getProperty(timestampProperty).getTime();
			GraphObject obj      = countMap.get(timestamp);

			if (obj == null) {
				obj = new GraphObjectMap();
			}

			Integer count = obj.getProperty(totalProperty);
			if (count == null) {
				count = 1;
			} else {
				count = count + 1;
			}
			obj.setProperty(totalProperty, count);

			// iterate over patterns
			for (final Entry<String, Pattern> patternEntry : aggregationPatterns.entrySet()) {

				if (patternEntry.getValue().matcher(message).matches()) {

					final IntProperty patternKeyProperty = new IntProperty(patternEntry.getKey());
					Integer c = obj.getProperty(patternKeyProperty);
					if (c == null) {
						c = 1;
					} else {
						c = c + 1;
					}

					obj.setProperty(patternKeyProperty, c);
				}
			}

			countMap.put(timestamp, obj);
		}

		return countMap;
	}

	private Set<IntProperty> getCountProperties(final Map<Long, GraphObject> entries) {

		final Set<IntProperty> result = new LinkedHashSet<>();

		for (final GraphObject obj : entries.values()) {

			for (final PropertyKey key : obj.getPropertyKeys(null)) {

				if (key instanceof IntProperty) {

					result.add((IntProperty)key);
				}
			}
		}

		return result;
	}

	private Map<String, Pattern> getAggregationPatterns(final HttpServletRequest request) {

		final Map<String, Pattern> patterns = new LinkedHashMap<>();

		for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

			final String key     = entry.getKey();
			final String[] value = entry.getValue();

			if (value.length > 0 && !ReservedRequestParameters.contains(key)) {
				patterns.put(key, Pattern.compile(value[0]));
			}
		}

		return patterns;
	}

	private static class Range<T> {

		private T start = null;
		private T end   = null;

		public Range(final T start, final T end) {
			this.start = start;
			this.end   = end;
		}
	}
}

