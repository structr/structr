/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

/**
 *
 * @author Christian Morgner
 */
public class SyncCommand extends NodeServiceCommand implements MaintenanceCommand, Serializable {

	private static final Logger logger                 = Logger.getLogger(SyncCommand.class.getName());
	private static final String STRUCTR_ZIP_DB_NAME    = "db";

	private static final Map<Class, String> typeMap    = new LinkedHashMap<>();
	private static final Map<Class, Method> methodMap  = new LinkedHashMap<>();
	private static final Map<String, Class> classMap   = new LinkedHashMap<>();

	static {

		typeMap.put(Byte[].class,      "00");
		typeMap.put(Byte.class,        "01");
		typeMap.put(Short[].class,     "02");
		typeMap.put(Short.class,       "03");
		typeMap.put(Integer[].class,   "04");
		typeMap.put(Integer.class,     "05");
		typeMap.put(Long[].class,      "06");
		typeMap.put(Long.class,        "07");
		typeMap.put(Float[].class,     "08");
		typeMap.put(Float.class,       "09");
		typeMap.put(Double[].class,    "10");
		typeMap.put(Double.class,      "11");
		typeMap.put(Character[].class, "12");
		typeMap.put(Character.class,   "13");
		typeMap.put(String[].class,    "14");
		typeMap.put(String.class,      "15");
		typeMap.put(Boolean[].class,   "16");
		typeMap.put(Boolean.class,     "17");

		// build reverse mapping
		for (Entry<Class, String> entry : typeMap.entrySet()) {
			classMap.put(entry.getValue(), entry.getKey());
		}
	}


	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		GraphDatabaseService graphDb = Services.getInstance().getService(NodeService.class).getGraphDb();
		String mode                  = (String)attributes.get("mode");
		String fileName              = (String)attributes.get("file");
		String validate              = (String)attributes.get("validate");
		boolean doValidation         = true;

		// should we validate imported nodes?
		if (validate != null) {

			try {

				doValidation = Boolean.valueOf(validate);

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to parse value for validation flag: {0}", t.getMessage());
			}
		}

		if (fileName == null) {

			throw new FrameworkException(400, "Please specify sync file.");
		}

		if ("export".equals(mode)) {

			exportToFile(graphDb, fileName, true);

		} else if ("exportDb".equals(mode)) {

			exportToFile(graphDb, fileName, false);

		} else if ("import".equals(mode)) {

			importFromFile(graphDb, securityContext, fileName, doValidation);

		} else {

			throw new FrameworkException(400, "Please specify sync mode (import|export).");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	// ----- static methods -----
	/**
	 * Exports the whole structr database to a file with the given name.
	 *
	 * @param graphDb
	 * @param fileName
	 * @param includeFiles
	 * @throws FrameworkException
	 */
	public static void exportToFile(final GraphDatabaseService graphDb, final String fileName, final boolean includeFiles) throws FrameworkException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			exportToStream(
				new FileOutputStream(fileName),
				app.nodeQuery(NodeInterface.class).getAsList(),
				app.relationshipQuery(RelationshipInterface.class).getAsList(),
				null,
				includeFiles
			);

			tx.success();

		} catch (Throwable t) {

			throw new FrameworkException(500, t.getMessage());
		}

	}

	/**
	 * Exports the given part of the structr database to a file with the given name.
	 *
	 * @param fileName
	 * @param nodes
	 * @param relationships
	 * @param filePaths
	 * @param includeFiles
	 * @throws FrameworkException
	 */
	public static void exportToFile(final String fileName, final Iterable<? extends NodeInterface> nodes, final Iterable<? extends RelationshipInterface> relationships, final Iterable<String> filePaths, final boolean includeFiles) throws FrameworkException {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			exportToStream(new FileOutputStream(fileName), nodes, relationships, filePaths, includeFiles);

			tx.success();

		} catch (Throwable t) {

			throw new FrameworkException(500, t.getMessage());
		}
	}

	/**
	 * Exports the given part of the structr database to the given output stream.
	 *
	 * @param outputStream
	 * @param nodes
	 * @param relationships
	 * @param filePaths
	 * @param includeFiles
	 * @throws FrameworkException
	 */
	public static void exportToStream(final OutputStream outputStream, final Iterable<? extends NodeInterface> nodes, final Iterable<? extends RelationshipInterface> relationships, final Iterable<String> filePaths, final boolean includeFiles) throws FrameworkException {

		try {

			Set<String> filesToInclude = new LinkedHashSet<>();
			ZipOutputStream zos        = new ZipOutputStream(outputStream);
			PrintWriter writer         = new PrintWriter(new BufferedWriter(new OutputStreamWriter(zos)));

			// collect files to include in export
			if (filePaths != null) {

				for (String file : filePaths) {

					filesToInclude.add(file);
				}
			}

			// set compression
			zos.setLevel(6);

			if (includeFiles) {
				// export files first
				exportDirectory(zos, new File("files"), "", filesToInclude.isEmpty() ? null : filesToInclude);
			}

			// export database
			exportDatabase(zos, writer, nodes, relationships);

			// finish ZIP file
			zos.finish();

			// close stream
			writer.close();

		} catch (Throwable t) {

			t.printStackTrace();

			throw new FrameworkException(500, t.getMessage());
		}
	}

	public static void importFromFile(final GraphDatabaseService graphDb, final SecurityContext securityContext, final String fileName, boolean doValidation) throws FrameworkException {

		try {
			importFromStream(graphDb, securityContext, new FileInputStream(fileName), doValidation);

		} catch (Throwable t) {

			t.printStackTrace();

			throw new FrameworkException(500, t.getMessage());
		}
	}

	public static void importFromStream(final GraphDatabaseService graphDb, final SecurityContext securityContext, final InputStream inputStream, boolean doValidation) throws FrameworkException {

		try {
			ZipInputStream zis = new ZipInputStream(inputStream);
			ZipEntry entry     = zis.getNextEntry();

			while (entry != null) {

				if (STRUCTR_ZIP_DB_NAME.equals(entry.getName())) {

					importDatabase(graphDb, securityContext, zis, doValidation);

				} else {

					// store other files in "files" dir..
					importDirectory(zis, entry);
				}

				entry = zis.getNextEntry();
			}

		} catch (IOException ioex) {

			ioex.printStackTrace();
		}
	}

	/**
	 * Serializes the given object into the given writer. The following format will
	 * be used to serialize objects. The first two characters are the type index, see
	 * typeMap above. After that, a single digit that indicates the length of the following
	 * length field follows. After that, the length field is serialized, followed by the
	 * string value of the given object and a space character for human readability.
	 *
	 * @param writer
	 * @param obj
	 */
	private static void serialize(PrintWriter writer, Object obj) {

		if (obj != null) {

			Class clazz = obj.getClass();
			String type = typeMap.get(clazz);

			if (type != null)  {

				if (clazz.isArray()) {

					Object[] array    = (Object[])obj;
					int len           = array.length;
					int log           = Integer.valueOf(len).toString().length();

					writer.print(type);	// type: 00-nn:  data type
					writer.print(log);	// log:  1-10: length of next int field
					writer.print(len);	// len:  length of value

					// serialize array
					for (Object o : (Object[])obj) {
						serialize(writer, o);
					}

				} else {

					String str        = obj.toString();
					int len           = str.length();
					int log           = Integer.valueOf(len).toString().length();

					writer.print(type);	// type: 00-nn:  data type
					writer.print(log);	// log:  1-10: length of next int field
					writer.print(len);	// len:  length of value
					writer.print(str);	// str:  the value
				}

				// make it more readable for the human eye
				writer.print(" ");

			} else {

				logger.log(Level.WARNING, "Unable to serialize object of type {0}, type not supported", obj.getClass());
			}
		}
	}

	private static Object deserialize(Reader reader) throws IOException {

		Object serializedObject = null;
		String type             = read(reader, 2);
		String lenLenSrc        = read(reader, 1);
		int lenLen              = Integer.parseInt(lenLenSrc);	// lenght of "length" field :)
		Class clazz             = classMap.get(type);

		if (clazz != null) {

			String lenSrc = read(reader, lenLen);
			int len       = Integer.parseInt(lenSrc);	// now we've got the "real" length of the following value

			if (clazz.isArray()) {

				// len is the length of the underlying array
				Object[] array = (Object[])Array.newInstance(clazz.getComponentType(), len);
				for (int i=0; i<len; i++) {

					array[i] = deserialize(reader);
				}

				// set array
				serializedObject = array;

			} else {

				// len is the length of the string representation of the real value
				String value = read(reader, len);

				if (clazz.equals(String.class)) {

					// strings can be returned immediately
					serializedObject = value;

				} else {

					try {

						Method valueOf = methodMap.get(clazz);
						if (valueOf == null) {

							valueOf = clazz.getMethod("valueOf", String.class);
							methodMap.put(clazz, valueOf);
						}

						// invoke static valueOf method
						if (valueOf != null) {

							serializedObject = valueOf.invoke(null, value);

						} else {

							logger.log(Level.WARNING, "Unable to find static valueOf method for type {0}", clazz);
						}

					} catch (Throwable t) {

						logger.log(Level.WARNING, "Unable to deserialize value {0} of type {1}, Class has no static valueOf method.", new Object[] { value, clazz } );
					}
				}
			}

		} else {

			logger.log(Level.WARNING, "Unsupported type {0} in input", type);
		}

		// skip white space after object (see serialize method)
		reader.skip(1);

		return serializedObject;
	}

	private static String read(Reader reader, int len) throws IOException {

		char[] buf = new char[len];

		// only continue if the desired number of chars could be read
		if (reader.read(buf, 0, len) == len) {

			return new String(buf, 0, len);
		}

		// end of stream
		throw new EOFException();
	}

	private static void exportDirectory(ZipOutputStream zos, File dir, String path, Set<String> filesToInclude) throws IOException {

		String nestedPath = path + dir.getName() + "/";
		ZipEntry dirEntry = new ZipEntry(nestedPath);
		zos.putNextEntry(dirEntry);

		File[] contents = dir.listFiles();
		if (contents != null) {

			for (File file : contents) {

				if (file.isDirectory()) {

					exportDirectory(zos, file, nestedPath, filesToInclude);

				} else {

					String relativePath = nestedPath + file.getName();
					boolean includeFile = true;

					if (filesToInclude != null) {

						includeFile = false;

						if  (filesToInclude.contains(relativePath)) {

							includeFile = true;
						}

					}

					if (includeFile) {

						// create ZIP entry
						ZipEntry fileEntry  = new ZipEntry(relativePath);
						fileEntry.setTime(file.lastModified());
						zos.putNextEntry(fileEntry);

						// copy file into stream
						FileInputStream fis = new FileInputStream(file);
						IOUtils.copy(fis, zos);
						fis.close();

						// flush and close entry
						zos.flush();
						zos.closeEntry();
					}
				}
			}
		}

		zos.closeEntry();

	}

	private static void exportDatabase(final ZipOutputStream zos, final PrintWriter writer,  final Iterable<? extends NodeInterface> nodes, final Iterable<? extends RelationshipInterface> relationships) throws IOException, FrameworkException {

		// start database zip entry
		final ZipEntry dbEntry        = new ZipEntry(STRUCTR_ZIP_DB_NAME);
		final String uuidPropertyName = GraphObject.id.dbName();
		int nodeCount                 = 0;
		int relCount                  = 0;

		zos.putNextEntry(dbEntry);

		for (NodeInterface nodeObject : nodes) {

			final Node node = nodeObject.getNode();

			System.out.println(node.getId() + ": " + (node.hasProperty("id") ? node.getProperty("id") : "null"));

			// ignore non-structr nodes
			if (node.hasProperty(GraphObject.id.dbName())) {

				writer.print("N");

				for (String key : node.getPropertyKeys()) {

					serialize(writer, key);
					serialize(writer, node.getProperty(key));
				}

				// do not use platform-specific line ending here!
				writer.print("\n");

				nodeCount++;
			}
		}

		writer.flush();

		for (RelationshipInterface relObject : relationships) {

			final Relationship rel = relObject.getRelationship();

			// ignore non-structr nodes
			if (rel.hasProperty(GraphObject.id.dbName())) {

				final Node startNode = rel.getStartNode();
				final Node endNode   = rel.getEndNode();

				if (startNode.hasProperty(uuidPropertyName) && endNode.hasProperty(uuidPropertyName)) {

					String startId = (String)startNode.getProperty(uuidPropertyName);
					String endId   = (String)endNode.getProperty(uuidPropertyName);

					writer.print("R");
					serialize(writer, startId);
					serialize(writer, endId);
					serialize(writer, rel.getType().name());

					for (String key : rel.getPropertyKeys()) {

						serialize(writer, key);
						serialize(writer, rel.getProperty(key));
					}

					// do not use platform-specific line ending here!
					writer.print("\n");

					relCount++;
				}

			}

		}

		writer.flush();

		// finish db entry
		zos.closeEntry();

		logger.log(Level.INFO, "Exported {0} nodes and {1} rels", new Object[] { nodeCount, relCount } );
	}

	private static void importDirectory(ZipInputStream zis, ZipEntry entry) throws IOException {

		if (entry.isDirectory()) {

			File newDir = new File(entry.getName());
			if (!newDir.exists()) {

				newDir.mkdirs();
			}

		} else {

			File newFile      = new File(entry.getName());
			boolean overwrite = false;

			if (!newFile.exists()) {

				overwrite = true;

			} else {

				if (newFile.lastModified() < entry.getTime()) {

					logger.log(Level.INFO, "Overwriting existing file {0} because import file is newer.", entry.getName());
					overwrite = true;
				}
			}

			if (overwrite) {

				FileOutputStream fos = new FileOutputStream(newFile);
				IOUtils.copy(zis, fos);

				fos.flush();
				fos.close();
			}
		}
	}

	private static void importDatabase(final GraphDatabaseService graphDb, final SecurityContext securityContext, final ZipInputStream zis, boolean doValidation) throws FrameworkException, IOException {

		final App app                        = StructrApp.getInstance();
		final RelationshipFactory relFactory = new RelationshipFactory(securityContext);
		final NodeFactory nodeFactory        = new NodeFactory(securityContext);
		final String uuidPropertyName        = GraphObject.id.dbName();
		double t0                            = System.nanoTime();
		Map<String, Node> uuidMap            = new LinkedHashMap<>();
		PropertyContainer currentObject      = null;
		String currentKey                    = null;
		boolean finished                     = false;
		long totalNodeCount                  = 0;
		long totalRelCount                   = 0;

		final BufferedReader reader = new BufferedReader(new InputStreamReader(zis));

		do {

			try (final Tx tx = app.tx(doValidation)) {

				final List<Relationship> rels = new LinkedList<>();
				final List<Node> nodes        = new LinkedList<>();
				long nodeCount                = 0;
				long relCount                 = 0;

				do {

					try {

						// store current position
						reader.mark(4);

						// read one byte
						String objectType = read(reader, 1);

						// skip newlines
						if ("\n".equals(objectType)) {
							continue;
						}

						if ("N".equals(objectType)) {

							currentObject = graphDb.createNode();
							nodeCount++;

							// store for later use
							nodes.add((Node)currentObject);

						} else if ("R".equals(objectType)) {

							String startId     = (String)deserialize(reader);
							String endId       = (String)deserialize(reader);
							String relTypeName = (String)deserialize(reader);

							Node endNode   = uuidMap.get(endId);
							Node startNode = uuidMap.get(startId);

							if (startNode != null && endNode != null) {

								RelationshipType relType = DynamicRelationshipType.withName(relTypeName);
								currentObject = startNode.createRelationshipTo(endNode, relType);

								// store for later use
								rels.add((Relationship)currentObject);
							}

							relCount++;

						} else {

							// reset if not at the beginning of a line
							reader.reset();

							if (currentKey == null) {

								currentKey = (String)deserialize(reader);

							} else {

								if (currentObject != null) {

									Object obj = deserialize(reader);

									if (uuidPropertyName.equals(currentKey) && currentObject instanceof Node) {

										String uuid = (String)obj;
										uuidMap.put(uuid, (Node)currentObject);
									}

									if (currentKey.length() != 0) {

										// store object in DB
										currentObject.setProperty(currentKey, obj);

										// set type label
										if (currentObject instanceof Node && NodeInterface.type.dbName().equals(currentKey)) {
											((Node) currentObject).addLabel(DynamicLabel.label((String) obj));
										}

									} else {

										logger.log(Level.SEVERE, "Invalid property key for value {0}, ignoring", obj);
									}

									currentKey = null;

								} else {

									logger.log(Level.WARNING, "No current object to store property in.");
								}
							}
						}

					} catch (EOFException eofex) {

						finished = true;
					}

				} while (!finished && (nodeCount + relCount < 200));

				totalNodeCount += nodeCount;
				totalRelCount  += relCount;

				for (Node node : nodes) {

					NodeInterface entity = nodeFactory.instantiate(node);
					TransactionCommand.nodeCreated(entity);
					entity.addToIndex();
				}

				for (Relationship rel : rels) {

					RelationshipInterface entity = relFactory.instantiate(rel);
					TransactionCommand.relationshipCreated(entity);
					entity.addToIndex();
				}

				logger.log(Level.INFO, "Imported {0} nodes and {1} rels, committing transaction..", new Object[] { totalNodeCount, totalRelCount } );

				tx.success();

			}

		} while (!finished);

		double t1   = System.nanoTime();
		double time = ((t1 - t0) / 1000000000.0);

		DecimalFormat decimalFormat  = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		logger.log(Level.INFO, "Import done in {0} s", decimalFormat.format(time));
	}
}
