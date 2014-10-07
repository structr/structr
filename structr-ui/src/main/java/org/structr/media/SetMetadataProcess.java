package org.structr.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */

public class SetMetadataProcess extends AbstractProcess<Void> {

	private final Map<String, String> metadata = new LinkedHashMap<>();
	private VideoFile inputVideo               = null;
	private String outputFileName              = null;
	private String fileExtension               = null;

	public SetMetadataProcess(final SecurityContext securityContext, final VideoFile inputVideo, final String key, final String value) {
		this(securityContext, inputVideo, toMap(key, value));
	}

	public SetMetadataProcess(final SecurityContext securityContext, final VideoFile inputVideo, final Map<String, String> values) {

		super(securityContext);

		this.outputFileName = inputVideo.getDiskFilePath(securityContext);
		this.inputVideo     = inputVideo;
		
		this.metadata.putAll(values);
	}

	@Override
	public void preprocess() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			// extract file extension
			fileExtension = StringUtils.substringAfterLast(inputVideo.getName(), ".");
			tx.success();

		} catch (FrameworkException fex) {}
	}

	@Override
	public StringBuilder getCommandLine() {

		if (metadata.isEmpty()) {
			return null;
		}

		final StringBuilder commandLine = new StringBuilder("avconv -y -i ");
		final String diskFilePath       = inputVideo.getDiskFilePath(securityContext);

		// build command line from builder options
		commandLine.append(diskFilePath);

		for (final Entry<String, String> meta : metadata.entrySet()) {

			commandLine.append(" -metadata ");
			commandLine.append(meta.getKey());
			commandLine.append("=\"");
			commandLine.append(meta.getValue());
			commandLine.append("\"");
		}

		commandLine.append(" -codec copy ");
		commandLine.append(diskFilePath);

		if (!fileExtension.isEmpty()) {
			commandLine.append(".");
			commandLine.append(fileExtension);
		}

		return commandLine;
	}

	@Override
	public Void processExited(int exitCode) {

		if (exitCode == 0) {

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				// move converted file into place
				final java.io.File diskFile = new java.io.File(outputFileName + "." + fileExtension);
				final java.io.File dstFile  = new java.io.File(outputFileName);
				if (diskFile.exists()) {

					Files.move(diskFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					FileHelper.updateMetadata(inputVideo);
				}

				tx.success();

			} catch (FrameworkException | IOException fex) {
				fex.printStackTrace();
			}

		}

		return null;
	}

	protected boolean accept(final String key, final String value) {
		return key != null && !key.startsWith(";");
	}

	private static final Map<String, String> toMap(final String key, final String value) {

		final Map<String, String> map = new LinkedHashMap<>();
		map.put(key, value);

		return map;
	}
}

