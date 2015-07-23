/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.graph;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class SyncCommand extends NodeServiceCommand implements MaintenanceCommand, Serializable {

	private static final Logger logger                = Logger.getLogger(SyncCommand.class.getName());
	private static final String STRUCTR_ZIP_DB_NAME   = "db";

	private static final Map<Class, Byte> typeMap     = new HashMap<>();
	private static final Map<Byte, Class> classMap    = new HashMap<>();

	static {

		typeMap.put(Byte[].class,      (byte) 0);
		typeMap.put(Byte.class,        (byte) 1);
		typeMap.put(Short[].class,     (byte) 2);
		typeMap.put(Short.class,       (byte) 3);
		typeMap.put(Integer[].class,   (byte) 4);
		typeMap.put(Integer.class,     (byte) 5);
		typeMap.put(Long[].class,      (byte) 6);
		typeMap.put(Long.class,        (byte) 7);
		typeMap.put(Float[].class,     (byte) 8);
		typeMap.put(Float.class,       (byte) 9);
		typeMap.put(Double[].class,    (byte)10);
		typeMap.put(Double.class,      (byte)11);
		typeMap.put(Character[].class, (byte)12);
		typeMap.put(Character.class,   (byte)13);
		typeMap.put(String[].class,    (byte)14);
		typeMap.put(String.class,      (byte)15);
		typeMap.put(Boolean[].class,   (byte)16);
		typeMap.put(Boolean.class,     (byte)17);

		// build reverse mapping
		for (Entry<Class, Byte> entry : typeMap.entrySet()) {
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
				app.nodeQuery(AbstractNode.class).includeDeletedAndHidden().getAsList(),
				app.relationshipQuery(AbstractRelationship.class).includeDeletedAndHidden().getAsList(),
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
			exportDatabase(zos, new BufferedOutputStream(zos), nodes, relationships);

			// finish ZIP file
			zos.finish();

			// close stream
			zos.flush();
			zos.close();

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
	 * @param outputStream
	 * @param obj
	 */
	public static void serializeData(DataOutputStream outputStream, byte[] data) throws IOException {

		outputStream.writeInt(data.length);
		outputStream.write(data);

		outputStream.flush();
	}

	public static void serialize(DataOutputStream outputStream, Object obj) throws IOException {

		if (obj != null) {

			Class clazz = obj.getClass();
			Byte type   = typeMap.get(clazz);

			if (type != null)  {

				if (clazz.isArray()) {

					Object[] array    = (Object[])obj;

					outputStream.writeByte(type);
					outputStream.writeInt(array.length);

					// serialize array
					for (Object o : (Object[])obj) {
						serialize(outputStream, o);
					}

				} else {

					outputStream.writeByte(type);
					writeObject(outputStream, type, obj);

					//outputStream.writeUTF(obj.toString());
				}

			} else {

				logger.log(Level.WARNING, "Unable to serialize object of type {0}, type not supported", obj.getClass());
			}

		} else {

			// null value
			outputStream.writeByte((byte)127);
		}

		outputStream.flush();
	}

	public static byte[] deserializeData(final DataInputStream inputStream) throws IOException {

		final int len       = inputStream.readInt();
		final byte[] buffer = new byte[len];

		inputStream.read(buffer, 0, len);

		return buffer;
	}

	public static Object deserialize(final DataInputStream inputStream) throws IOException {

		Object serializedObject = null;
		final byte type         = inputStream.readByte();
		Class clazz             = classMap.get(type);

		if (clazz != null) {

			if (clazz.isArray()) {

				// len is the length of the underlying array
				final int len        = inputStream.readInt();
				final Object[] array = (Object[])Array.newInstance(clazz.getComponentType(), len);

				for (int i=0; i<len; i++) {

					array[i] = deserialize(inputStream);
				}

				// set array
				serializedObject = array;

			} else {

				serializedObject = readObject(inputStream, type);
			}

		} else if (type != 127) {

			logger.log(Level.WARNING, "Unsupported type \"{0}\" in input", type);
		}




		return serializedObject;
	}

	private static void exportDirectory(ZipOutputStream zos, File dir, String path, Set<String> filesToInclude) throws IOException {

		final String nestedPath = path + dir.getName() + "/";
		final ZipEntry dirEntry = new ZipEntry(nestedPath);
		zos.putNextEntry(dirEntry);

		final File[] contents = dir.listFiles();
		if (contents != null) {

			for (File file : contents) {

				if (file.isDirectory()) {

					exportDirectory(zos, file, nestedPath, filesToInclude);

				} else {

					final String fileName     = file.getName();
					final String relativePath = nestedPath + fileName;
					boolean includeFile = true;

					if (filesToInclude != null) {

						includeFile = false;

						if  (filesToInclude.contains(fileName)) {

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

	private static void exportDatabase(final ZipOutputStream zos, final OutputStream outputStream,  final Iterable<? extends NodeInterface> nodes, final Iterable<? extends RelationshipInterface> relationships) throws IOException, FrameworkException {

		// start database zip entry
		final ZipEntry dbEntry        = new ZipEntry(STRUCTR_ZIP_DB_NAME);
		final DataOutputStream dos    = new DataOutputStream(outputStream);
		final String uuidPropertyName = GraphObject.id.dbName();
		int nodeCount                 = 0;
		int relCount                  = 0;

		zos.putNextEntry(dbEntry);

		for (NodeInterface nodeObject : nodes) {

			final Node node = nodeObject.getNode();

			// ignore non-structr nodes
			if (node.hasProperty(GraphObject.id.dbName())) {

				outputStream.write('N');

				for (String key : node.getPropertyKeys()) {

					serialize(dos, key);
					serialize(dos, node.getProperty(key));
				}

				// do not use platform-specific line ending here!
				dos.write('\n');

				nodeCount++;
			}
		}

		dos.flush();

		for (RelationshipInterface relObject : relationships) {

			final Relationship rel = relObject.getRelationship();

			// ignore non-structr nodes
			if (rel.hasProperty(GraphObject.id.dbName())) {

				final Node startNode = rel.getStartNode();
				final Node endNode   = rel.getEndNode();

				if (startNode.hasProperty(uuidPropertyName) && endNode.hasProperty(uuidPropertyName)) {

					String startId = (String)startNode.getProperty(uuidPropertyName);
					String endId   = (String)endNode.getProperty(uuidPropertyName);

					outputStream.write('R');
					serialize(dos, startId);
					serialize(dos, endId);
					serialize(dos, rel.getType().name());

					for (String key : rel.getPropertyKeys()) {

						serialize(dos, key);
						serialize(dos, rel.getProperty(key));
					}

					// do not use platform-specific line ending here!
					dos.write('\n');

					relCount++;
				}
			}

		}

		dos.flush();

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
		final DataInputStream dis            = new DataInputStream(new BufferedInputStream(zis));
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

		do {

			try (final Tx tx = app.tx(doValidation)) {

				final List<Relationship> rels = new LinkedList<>();
				final List<Node> nodes        = new LinkedList<>();
				long nodeCount                = 0;
				long relCount                 = 0;

				do {

					try {

						// store current position
						dis.mark(4);

						// read one byte
						byte objectType = dis.readByte();

						// skip newlines
						if (objectType == '\n') {
							continue;
						}

						if (objectType == 'N') {

							// break look after 200 objects, commit and restart afterwards
							 if(nodeCount + relCount >= 200) {
								 dis.reset();
								 break;
							 }

							currentObject = graphDb.createNode();
							nodeCount++;

							// store for later use
							nodes.add((Node)currentObject);

						} else if (objectType == 'R') {

							// break look after 200 objects, commit and restart afterwards
							 if(nodeCount + relCount >= 200) {
								 dis.reset();
								 break;
							 }

							String startId     = (String)deserialize(dis);
							String endId       = (String)deserialize(dis);
							String relTypeName = (String)deserialize(dis);

							Node endNode   = uuidMap.get(endId);
							Node startNode = uuidMap.get(startId);

							if (startNode != null && endNode != null) {

								RelationshipType relType = DynamicRelationshipType.withName(relTypeName);
								currentObject = startNode.createRelationshipTo(endNode, relType);

								// store for later use
								rels.add((Relationship)currentObject);

								relCount++;

							} else {

								System.out.println("NOT creating relationship of type " + relTypeName + ", start: " + startId + ", end: " + endId);
							}

						} else {

							// reset if not at the beginning of a line
							dis.reset();

							if (currentKey == null) {

								currentKey = (String)deserialize(dis);

							} else {

								if (currentObject != null) {

									Object obj = deserialize(dis);

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

				} while (!finished);

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

	private static Object readObject(final DataInputStream inputStream, final byte type) throws IOException {

		switch (type) {

			case  0:
			case  1:
				return inputStream.readByte();

			case  2:
			case  3:
				return inputStream.readShort();

			case  4:
			case  5:
				return inputStream.readInt();

			case  6:
			case  7:
				return inputStream.readLong();

			case  8:
			case  9:
				return inputStream.readFloat();

			case 10:
			case 11:
				return inputStream.readDouble();

			case 12:
			case 13:
				return inputStream.readChar();

			case 14:
			case 15:
				return new String(deserializeData(inputStream), "UTF-8");

				// this doesn't work with very long strings
				//return inputStream.readUTF();

			case 16:
			case 17:
				return inputStream.readBoolean();
		}

		return null;
	}

	private static void writeObject(final DataOutputStream outputStream, final byte type, final Object value) throws IOException {

		switch (type) {

			case  0:
			case  1:
				outputStream.writeByte((byte)value);
				break;

			case  2:
			case  3:
				outputStream.writeShort((short)value);
				break;

			case  4:
			case  5:
				outputStream.writeInt((int)value);
				break;

			case  6:
			case  7:
				outputStream.writeLong((long)value);
				break;

			case  8:
			case  9:
				outputStream.writeFloat((float)value);
				break;

			case 10:
			case 11:
				outputStream.writeDouble((double)value);
				break;

			case 12:
			case 13:
				outputStream.writeChar((char)value);
				break;

			case 14:
			case 15:
				serializeData(outputStream, ((String)value).getBytes("UTF-8"));

				// this doesn't work with very long strings
				//outputStream.writeUTF((String)value);
				break;

			case 16:
			case 17:
				outputStream.writeBoolean((boolean)value);
				break;
		}
	}
}
