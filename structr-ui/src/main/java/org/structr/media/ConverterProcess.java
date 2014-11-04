package org.structr.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

public class ConverterProcess extends AbstractProcess<VideoFile> {

	private VideoFile newFile     = null;
	private VideoFile inputFile   = null;
	private String outputFileName = null;
	private String outputSize     = null;
	private String fileExtension  = null;

	public ConverterProcess(final SecurityContext securityContext, final VideoFile inputFile, final String outputFileName, final String outputSize) {

		super(securityContext);

		this.inputFile      = inputFile;
		this.outputFileName = outputFileName;
		this.outputSize     = outputSize;
	}

	@Override
	public void preprocess() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			// create an empty file to store the converted video
			newFile = FileHelper.createFile(securityContext, new byte[0], null, VideoFile.class, outputFileName);

			// extract file extension
			fileExtension = StringUtils.substringAfterLast(outputFileName, ".");

			// obtain destination path of new file
			outputFileName = newFile.getFileOnDisk().getAbsolutePath();

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public StringBuilder getCommandLine() {

		final StringBuilder commandLine = new StringBuilder("avconv -y -i ");

		// build command line from builder options
		commandLine.append(inputFile.getDiskFilePath(securityContext));

		if (outputSize != null) {

			commandLine.append(" -s ");
			commandLine.append(outputSize);
			commandLine.append(" ");

		} else {

			commandLine.append(" -vcodec copy ");
		}

		commandLine.append(outputFileName);

		if (!fileExtension.isEmpty()) {
			commandLine.append(".");
			commandLine.append(fileExtension);
		}

		return commandLine;
	}

	@Override
	public VideoFile processExited(int exitCode) {

		if (exitCode == 0) {

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				// move converted file into place
				final java.io.File diskFile = new java.io.File(outputFileName + "." + fileExtension);
				final java.io.File dstFile  = new java.io.File(outputFileName);
				if (diskFile.exists()) {

					Files.move(diskFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					FileHelper.updateMetadata(newFile);
				}

				tx.success();

			} catch (FrameworkException | IOException fex) {
				fex.printStackTrace();
			}

		}

		return newFile;
	}
}
