package org.structr.files.cmis.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.web.entity.FileBase;

/**
 * An implementation of the CMIS ContentStream interface using a MappedByteBuffer.
 *
 * @author Christian Morgner
 */
public class CMISContentStream extends CMISExtensionsData implements ContentStream {

	private String contentType = null;
	private String name        = null;
	private long length        = 0L;
	private File file          = null;
	private long offset        = 0L;


	public CMISContentStream(final FileBase file, final BigInteger offset, final BigInteger length) {

		this.contentType = file.getProperty(FileBase.contentType);
		this.name        = file.getName();
		this.file        = file.getFileOnDisk();
		this.offset      = offset != null ? offset.longValue() : 0L;
		this.length      = length != null ? length.longValue() : file.getSize();
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public BigInteger getBigLength() {
		return BigInteger.valueOf(length);
	}

	@Override
	public String getMimeType() {
		return contentType;
	}

	@Override
	public String getFileName() {
		return name;
	}

	@Override
	public InputStream getStream() {

		try {
			final FileChannel channel     = FileChannel.open(file.toPath(), StandardOpenOption.READ);
			final long mappedLength       = Math.max(0, Math.min(channel.size() - offset, length));
			final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, mappedLength);

			return new MappedInputStream(buffer);

		} catch (IOException ioex) {

			throw new CmisConstraintException(ioex.getMessage());
		}
	}

	// ----- nested classes -----
	private class MappedInputStream extends InputStream {

		private MappedByteBuffer buffer = null;

		public MappedInputStream(final MappedByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int read() throws IOException {

			if (buffer.remaining() > 0) {
				return buffer.get();
			}

			return -1;
		}

		@Override
		public int read(final byte b[]) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(final byte b[], final int off, final int len) throws IOException {

			final int length = Math.min(buffer.remaining(), len);

			buffer.get(b, off, length);

			if (length == 0) {
				return -1;
			}

			return length;
		}

		@Override
		public long skip(final long n) throws IOException {

			final int currentPosition = Long.valueOf(buffer.position()).intValue();
			final int skip            = Math.min(buffer.remaining(), Long.valueOf(n).intValue());

			buffer.position(currentPosition + skip);

			return skip;
		}

		@Override
		public int available() throws IOException {
			return buffer.remaining();
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public synchronized void mark(int readlimit) {
			buffer.mark();
		}

		@Override
		public synchronized void reset() throws IOException {
			buffer.reset();
		}

		@Override
		public boolean markSupported() {
			return true;
		}
	}
}
