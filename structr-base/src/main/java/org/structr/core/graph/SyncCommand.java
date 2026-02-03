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
package org.structr.core.graph;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;
import org.structr.schema.SchemaHelper;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 *
 */
public class SyncCommand extends NodeServiceCommand implements MaintenanceCommand, Serializable {

	private static final Logger logger                = LoggerFactory.getLogger(SyncCommand.class.getName());
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

		final DatabaseService graphDb = Services.getInstance().getDatabaseService();
		final String mode             = (String)attributes.get("mode");
		final String fileName         = (String)attributes.get("file");
		final Boolean validate        = toStringOrBoolean(attributes.get("validate"));
		final String query            = (String)attributes.get("query");
		final Long batchSize          = (Long)attributes.get("batchSize");
		boolean doValidation          = true;

		// should we validate imported nodes?
		if (validate != null) {

			doValidation = validate;
		}

		if (fileName == null) {

			throw new FrameworkException(400, "Please specify sync file.");
		}

		if ("export".equals(mode)) {

			exportToFile(graphDb, fileName, query, true);

		} else if ("exportDb".equals(mode)) {

			exportToFile(graphDb, fileName, query, false);

		} else if ("import".equals(mode)) {

			importFromFile(graphDb, securityContext, fileName, doValidation, batchSize);

		} else {

			throw new FrameworkException(400, "Please specify sync mode (import|export).");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	// ----- private methods -----
	private Boolean toStringOrBoolean(final Object value) {

		if (value != null) {
			return Boolean.valueOf(value.toString());
		}

		return null;
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
	public static void exportToFile(final DatabaseService graphDb, final String fileName, final String query, final boolean includeFiles) throws FrameworkException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final NodeFactory nodeFactory         = new NodeFactory(SecurityContext.getSuperUserInstance());
			final RelationshipFactory relFactory  = new RelationshipFactory(SecurityContext.getSuperUserInstance());
			final Set<NodeInterface> nodes        = new HashSet<>();
			final Set<RelationshipInterface> rels = new HashSet<>();
			boolean conditionalIncludeFiles       = includeFiles;

			if (query != null) {

				logger.info("Using Cypher query {} to determine export set, disabling export of files", query);

				conditionalIncludeFiles = false;

				for (final GraphObject obj : StructrApp.getInstance().query(query, null)) {

					if (obj.isNode()) {
						nodes.add(obj.getSyncNode());
					} else {
						rels.add(obj.getSyncRelationship());
					}
				}

				logger.info("Query returned {} nodes and {} relationships.", nodes.size(), rels.size());

			} else {

				Iterables.addAll(nodes, nodeFactory.bulkInstantiate(graphDb.getAllNodes()));
				Iterables.addAll(rels, relFactory.bulkInstantiate(graphDb.getAllRelationships()));
			}

			try (final FileOutputStream fos = new FileOutputStream(fileName)) {

				exportToStream(fos, nodes, rels, null, conditionalIncludeFiles);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
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

			try (final FileOutputStream fos = new FileOutputStream(fileName)) {

				exportToStream(fos, nodes, relationships, filePaths, includeFiles);
			}

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

		try (final ZipOutputStream zos = new ZipOutputStream(outputStream)) {

			final Set<String> filesToInclude = new LinkedHashSet<>();
			if (filePaths != null) {

				for (String file : filePaths) {

					filesToInclude.add(file);
				}
			}

			// set compression
			zos.setLevel(6);

			if (includeFiles) {

				logger.info("Exporting files..");

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

			logger.warn("", t);

			throw new FrameworkException(500, t.getMessage());
		}
	}

	public static void importFromFile(final DatabaseService graphDb, final SecurityContext securityContext, final String fileName, boolean doValidation) throws FrameworkException {
		importFromFile(graphDb, securityContext, fileName, doValidation, 400L);
	}

	public static void importFromFile(final DatabaseService graphDb, final SecurityContext securityContext, final String fileName, boolean doValidation, final Long batchSize) throws FrameworkException {

		try (final InputStream fis = new FileInputStream(fileName)) {

			importFromStream(graphDb, securityContext, fis, doValidation, batchSize);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
			throw new FrameworkException(500, t.getMessage());
		}
	}

	public static void importFromStream(final DatabaseService graphDb, final SecurityContext securityContext, final InputStream inputStream, boolean doValidation, final Long batchSize) throws FrameworkException {

		try (final ZipInputStream zis = new ZipInputStream(inputStream)) {

			ZipEntry entry = zis.getNextEntry();

			while (entry != null) {

				if (STRUCTR_ZIP_DB_NAME.equals(entry.getName())) {

					importDatabase(graphDb, securityContext, zis, doValidation, batchSize);

				} else {

					// store other files in "files" dir..
					importDirectory(zis, entry);
				}

				entry = zis.getNextEntry();
			}

		} catch (IOException ioex) {

			logger.warn("", ioex);
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

				logger.warn("Unable to serialize object of type {}, type not supported", obj.getClass());
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

			logger.warn("Unsupported type \"{}\" in input", type);
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

						includeFile = filesToInclude.contains(fileName);
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

	private static void exportDatabase(final ZipOutputStream zos, final OutputStream outputStream, final Iterable<? extends NodeInterface> nodes, final Iterable<? extends RelationshipInterface> relationships) throws IOException, FrameworkException {

		// start database zip entry
		final ZipEntry dbEntry        = new ZipEntry(STRUCTR_ZIP_DB_NAME);
		final DataOutputStream dos    = new DataOutputStream(outputStream);
		final String uuidPropertyName = "id";
		int nodeCount                 = 0;
		int relCount                  = 0;

		zos.putNextEntry(dbEntry);

		for (NodeInterface nodeObject : nodes) {

			// skip schema
			if (nodeObject.is(StructrTraits.SCHEMA_RELOADING_NODE)) {
				continue;
			}

			final Node node = nodeObject.getNode();

			// ignore non-structr nodes
			if (node.hasProperty(uuidPropertyName)) {

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
			if (rel.hasProperty(uuidPropertyName)) {

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

		logger.info("Exported {} nodes and {} rels", nodeCount, relCount);
	}

	private static void importDirectory(ZipInputStream zis, ZipEntry entry) throws IOException {

		// Fix ZIP slip security problem
		final Path checkPath = Path.of(entry.getName());

		if (!checkPath.normalize().equals(checkPath) || checkPath.isAbsolute()) {

			throw new RuntimeException("Refusing to extract unsafe ZIP entry " + entry.getName());
		}

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

					logger.info("Overwriting existing file {} because import file is newer.", entry.getName());
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

	private static void importDatabase(final DatabaseService graphDb, final SecurityContext securityContext, final ZipInputStream zis, boolean doValidation, final Long batchSize) throws FrameworkException, IOException {

		final App app                             = StructrApp.getInstance();
		final Set<String> labels                  = new LinkedHashSet<>();
		final DataInputStream dis                 = new DataInputStream(new BufferedInputStream(zis));
		final long internalBatchSize              = batchSize != null ? batchSize : 200;
		final String uuidPropertyName             = "id";
		double t0                                 = System.nanoTime();
		EntityCreation currentObject              = null;
		String currentKey                         = null;
		boolean finished                          = false;
		long totalNodeCount                       = 0;
		long totalRelCount                        = 0;

		labels.add(org.structr.core.graph.NodeInterface.class.getSimpleName());

		// add tenant identifier to all nodes
		if (graphDb.getTenantIdentifier() != null) {
			labels.add(graphDb.getTenantIdentifier());
		}

		do {

			try (final Tx tx = app.tx(doValidation)) {

				final List<RelationshipCreation> rels = new LinkedList<>();
				final List<NodeCreation> nodes        = new LinkedList<>();
				long nodeCount                        = 0;
				long relCount                         = 0;

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

							// break loop after 200 objects, commit and restart afterwards
							if (nodeCount + relCount >= internalBatchSize) {
								dis.reset();
								break;
							}

							final NodeCreation newNode = new NodeCreation(labels);
							nodeCount++;

							// store for later use
							nodes.add(newNode);

							currentObject = newNode;

						} else if (objectType == 'R') {

							// break look after 200 objects, commit and restart afterwards
							if (nodeCount + relCount >= internalBatchSize) {
								dis.reset();
								break;
							}

							final String startId              = (String)deserialize(dis);
							final String endId                = (String)deserialize(dis);
							final String relTypeName          = (String)deserialize(dis);
							final RelationshipType relType    = RelationshipType.forName(relTypeName);
							final RelationshipCreation newRel = new RelationshipCreation(startId, endId, relType);

							// store for later use
							rels.add(newRel);

							currentObject = newRel;

							relCount++;

						} else {

							// reset if not at the beginning of a line
							dis.reset();

							if (currentKey == null) {

								try {
									currentKey = (String)deserialize(dis);

								} catch (Throwable t) {
									logger.warn("", t);
								}

							} else {

								final Object obj = deserialize(dis);
								if (obj != null) {

									if (currentObject != null) {

										if (uuidPropertyName.equals(currentKey)) {

											final String uuid = (String)obj;
											currentObject.setId(uuid);
										}

										if (currentKey.length() != 0) {

											// store object in DB
											currentObject.put(currentKey, obj);

											// set type label
											if (currentObject instanceof NodeCreation && "type".equals(currentKey)) {

												((NodeCreation)currentObject).addLabel((String) obj);
											}

										} else {

											logger.error("Invalid property key for value {}, ignoring", obj);
										}
									}
								}

								currentKey = null;
							}
						}

					} catch (EOFException eofex) {
						finished = true;
					}

				} while (!finished);

				final Map<String, Node> nodeMap = new LinkedHashMap<>();

				// create nodes and rels
				nodes.stream().forEach(e -> e.create(graphDb, nodeMap));
				rels.stream().forEach(e -> e.create(graphDb, nodeMap));

				totalNodeCount += nodeCount;
				totalRelCount  += relCount;

				logger.info("Imported {} nodes and {} rels, committing transaction..", totalNodeCount, totalRelCount);

				tx.success();
			}

		} while (!finished);

		// build schema
		try (final Tx tx = app.tx()) {

			SchemaHelper.reloadSchema(new ErrorBuffer(), securityContext.getSessionId(), true, true);
			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		final Map<String, Object> params = new HashMap<>();
		params.put("removeUnused", false);

		// set correct labels after schema has been compiled
		app.command(BulkCreateLabelsCommand.class).execute(params);

		double t1   = System.nanoTime();
		double time = ((t1 - t0) / 1000000000.0);

		DecimalFormat decimalFormat  = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		logger.info("Import done in {} s", decimalFormat.format(time));
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
				return new String(deserializeData(inputStream), StandardCharsets.UTF_8);

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
				serializeData(outputStream, ((String)value).getBytes(StandardCharsets.UTF_8));

				// this doesn't work with very long strings
				//outputStream.writeUTF((String)value);
				break;

			case 16:
			case 17:
				outputStream.writeBoolean((boolean)value);
				break;
		}
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Hidden;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}

	@Override
	public final List<ConceptReference> getParentConcepts() {
		return List.of(ConceptReference.of(ConceptType.Topic, "Maintenance commands"));
	}

	static class NodeCreation extends EntityCreation {

		private final Set<String> labels = new LinkedHashSet<>();

		public NodeCreation(final Set<String> labels) {
			this.labels.addAll(labels);
		}

		@Override
		public void create(final DatabaseService db, final Map<String, Node> nodeMap) {

			final String type = (String)get("type");
			if (type != null) {

				final Node node = db.createNode(type, labels, this);
				nodeMap.put(getId(), node);
			}
		}

		void addLabel(final String label) {
			this.labels.add(label);
		}
	}

	static class RelationshipCreation extends EntityCreation {

		private RelationshipType relType = null;
		private String sourceId          = null;
		private String targetId          = null;

		public RelationshipCreation(final String sourceId, final String targetId, final RelationshipType relType) {

			this.sourceId = sourceId;
			this.targetId = targetId;
			this.relType  = relType;
		}

		@Override
		void create(final DatabaseService db, final Map<String, Node> nodeMap) {

			final Node sourceNode = nodeMap.get(sourceId);
			final Node targetNode = nodeMap.get(targetId);

			if (sourceNode != null && targetNode != null) {

				sourceNode.createRelationshipTo(targetNode, relType, this);
			}
		}
	}

	static abstract class EntityCreation extends LinkedHashMap<String, Object> {

		private String id = null;

		abstract void create(final DatabaseService db, final Map<String, Node> nodeMap);

		public void setId(final String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
}