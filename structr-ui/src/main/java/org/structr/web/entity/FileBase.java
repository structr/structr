package org.structr.web.entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.type;
import org.structr.core.Services;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;
import static org.structr.web.entity.AbstractFile.parent;
import org.structr.web.entity.relation.Folders;
import org.structr.web.property.PathProperty;

/**
 *
 * @author Christian Morgner
 */
public class FileBase extends AbstractFile implements Linkable {

	private static final Logger logger = Logger.getLogger(FileBase.class.getName());

	public static final Property<String> contentType = new StringProperty("contentType").indexedWhenEmpty();
	public static final Property<String> relativeFilePath = new StringProperty("relativeFilePath").readOnly();
	public static final Property<Long> size = new LongProperty("size").indexed().readOnly();
	public static final Property<String> url = new StringProperty("url");
	public static final Property<Long> checksum = new LongProperty("checksum").unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds = new IntProperty("cacheForSeconds");
	public static final Property<Integer> version = new IntProperty("version").indexed().readOnly();
	public static final Property<String> path = new PathProperty("path").indexed().readOnly();
	public static final Property<Boolean> isFile = new BooleanProperty("isFile", true).readOnly();

	public static final View publicView = new View(FileBase.class, PropertyView.Public, type, name, contentType, size, url, owner, path, isFile);
	public static final View uiView = new View(FileBase.class, PropertyView.Ui, type, contentType, relativeFilePath, size, url, parent, checksum, version, cacheForSeconds, owner, path, isFile);

	@Override
	public void onNodeCreation() {

		final String uuid = getUuid();
		final String filePath = getDirectoryPath(uuid) + "/" + uuid;

		try {
			setProperty(relativeFilePath, filePath);
		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while trying to set relative file path {0}: {1}", new Object[]{filePath, t});

		}
	}

	@Override
	public void onNodeDeletion() {

		String filePath = null;
		try {
			final String path = getRelativeFilePath();

			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while trying to delete file {0}: {1}", new Object[]{filePath, t});

		}

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			java.io.File fileOnDisk = new java.io.File(filesPath + "/" + getRelativeFilePath());

			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.log(Level.SEVERE, "Could not create file", ex);
				return;
			}

			setProperty(checksum, FileHelper.getChecksum(FileBase.this));
			setProperty(version, 0);

			long fileSize = FileHelper.getSize(FileBase.this);
			if (fileSize > 0) {
				setProperty(size, fileSize);
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, "Could not create file", ex);

		}

	}

	@Override
	public String getPath() {
		return FileHelper.getFolderPath(this);
	}

	public String getRelativeFilePath() {

		return getProperty(FileBase.relativeFilePath);

	}

	public String getUrl() {

		return getProperty(FileBase.url);

	}

	public String getContentType() {

		return getProperty(FileBase.contentType);

	}

	public Long getSize() {

		return getProperty(size);

	}

	public Long getChecksum() {

		return getProperty(checksum);

	}

	public String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FileBase.version);
		if (_version == null) {

			setProperty(FileBase.version, 1);

		} else {

			setProperty(FileBase.version, _version + 1);
		}
	}

	public InputStream getInputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream and save checksum and size after closing
				fis = new FileInputStream(fileOnDisk);

				return fis;

			} catch (FileNotFoundException e) {
				logger.log(Level.FINE, "File not found: {0}", new Object[]{path});

				if (fis != null) {

					try {

						fis.close();

					} catch (IOException ignore) {
					}

				}
			}
		}

		return null;
	}

	public OutputStream getOutputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file output stream and save checksum and size after closing
				FileOutputStream fos = new FileOutputStream(fileOnDisk) {

					@Override
					public void close() throws IOException {

						super.close();

						try {
							setProperty(checksum, FileHelper.getChecksum(FileBase.this));
							setProperty(size, FileHelper.getSize(FileBase.this));

						} catch (FrameworkException ex) {

							logger.log(Level.SEVERE, "Could not determine or save checksum and size after closing file output stream", ex);

						}
					}
				};

				return fos;

			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "File not found: {0}", new Object[]{path});
			}

		}

		return null;

	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {

		final List<Syncable> data = new LinkedList<>();

		// nodes
		data.add(getProperty(parent));
		data.add(getIncomingRelationship(Folders.class));

		return data;
	}

	@Override
	public boolean isNode() {
		return true;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return null;
	}

	@Override
	public void updateFromPropertyMap(PropertyMap properties) throws FrameworkException {
	}
}
