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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
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
	private static final ISO8601DateProperty timestampProperty  = new ISO8601DateProperty("timestamp");
	private static final ISO8601DateProperty firstEntryProperty = new ISO8601DateProperty("firstEntry");
	private static final ISO8601DateProperty lastEntryProperty  = new ISO8601DateProperty("lastEntry");

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
			final Predicate datePredicate   = getTimestampPredicate();

			for (final Path path : files) {

				try (final BufferedReader reader = Files.newBufferedReader(path, Charset.forName("utf-8"))) {

					final String fileName = path.getFileName().toString();
					String pathSubjectId  = inverse ? fileName.substring(33, 64) : fileName.substring(0, 32);
					String pathObjectId   = inverse ? fileName.substring(0, 32)  : fileName.substring(33, 64);

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

							if (overview) {

								Integer actionCount = actions.get(entryAction);
								if (actionCount == null) {

									actions.put(entryAction, 1);

								} else {

									actions.put(entryAction, actionCount + 1);
								}

								if (timestamp <= beginTimestamp) {
									beginTimestamp = timestamp;
								}

								if (timestamp >= endTimestamp) {
									endTimestamp = timestamp;
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

			} else if (StringUtils.isNotBlank(aggregate) && entries.size() > 1) {

				// sort result
				Collections.sort(entries, new GraphObjectComparator(timestampProperty, false));

				// aggregate results
				return aggregate(entries, aggregate);

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

	private Predicate getTimestampPredicate() throws FrameworkException {

		final Query dummyQuery = StructrApp.getInstance(securityContext).nodeQuery();
		timestampProperty.extractSearchableAttribute(securityContext, securityContext.getRequest(), dummyQuery);

		return dummyQuery.toPredicate();
	}

	private Result aggregate(final List<GraphObject> entries, final String dateFormat) {

		final GraphObjectMap result           = new GraphObjectMap();
		final SimpleDateFormat format         = new SimpleDateFormat(dateFormat);
		final GraphObject firstEntry          = entries.get(0);
		final GraphObject lastEntry           = entries.get(entries.size() - 1);
		final Date firstDate                  = firstEntry.getProperty(timestampProperty);
		final Date lastDate                   = lastEntry.getProperty(timestampProperty);
		final long startTimestamp             = firstDate.getTime();
		final long endTimestamp               = lastDate.getTime();
		final long interval                   = findInterval(dateFormat);
		final long start                      = alignDateOnFormat(dateFormat, startTimestamp);
		final TreeMap<Long, Integer> countMap = toCountMap(entries);


		for (long current = start; current <= endTimestamp; current += interval) {

			final Map<Long, Integer> counts = countMap.subMap(current, true, current+interval, false);
			final String formattedDate      = format.format(current);
			long sum                        = 0;

			for (final Integer count : counts.values()) {
				sum += count;
			}

			result.put(new LongProperty(Long.toString(current)), sum);
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


		final long max                = TimeUnit.DAYS.toMillis(365);
		final long step               = TimeUnit.SECONDS.toMillis(60);

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

	private TreeMap<Long, Integer> toCountMap(final List<GraphObject> entries) {

		final TreeMap<Long, Integer> countMap = new TreeMap<>();

		for (final GraphObject entry : entries) {

			final long timestamp = entry.getProperty(timestampProperty).getTime();
			Integer count        = countMap.get(timestamp);

			if (count == null) {
				count = 1;
			} else {
				count = count + 1;
			}

			countMap.put(timestamp, count);
		}

		return countMap;
	}
}

