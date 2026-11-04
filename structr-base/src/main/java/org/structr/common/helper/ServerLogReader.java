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
package org.structr.common.helper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Reads the server log as entries: a line that starts with a timestamp begins an entry, and the lines
 * without one that follow it, such as a stack trace, belong to it. Only complete lines are read, so a
 * line that is still being written is left for the next read, and every result carries the byte offset
 * after its last line, from which the next read continues.
 */
public final class ServerLogReader {

	public enum Level { TRACE, DEBUG, INFO, WARN, ERROR }

	/**
	 * @param lines   the lines of the selected entries, oldest first
	 * @param offset  the byte offset to continue from, or -1 for a compressed file, which has none
	 * @param rotated true if the requested offset lay beyond the end of the file, so it was read from the start
	 */
	public record Result(List<String> lines, long offset, boolean rotated) {}

	/** An entry of the server log starts with a timestamp, the lines after it belong to it. */
	public static final Pattern SERVER_LOG_ENTRY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} ");

	/** Every line is an entry of its own, as in the request log. */
	public static final Pattern ENTRY_PER_LINE = Pattern.compile("^");

	private static final Pattern LEVEL  = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[.*? (TRACE|DEBUG|INFO|WARN|ERROR) ");
	private static final int CHUNK      = 8192;

	private ServerLogReader() {}

	public static Level parseLevel(final String value) {

		try {

			return value == null || value.isBlank() ? null : Level.valueOf(value.trim().toUpperCase(Locale.ROOT));

		} catch (IllegalArgumentException ex) {

			return null;
		}
	}

	/**
	 * Returns the last entries that match, until they add up to at least maxLines lines, reading backwards
	 * from the end of the file.
	 */
	public static Result tail(final Path file, final int maxLines, final Level minLevel, final String filter) throws IOException {

		return tail(file, maxLines, minLevel, filter, SERVER_LOG_ENTRY);
	}

	public static Result tail(final Path file, final int maxLines, final Level minLevel, final String filter, final Pattern entryStart) throws IOException {

		try (final FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {

			final long end                 = endOfCompleteLines(channel, channel.size());
			final List<List<String>> found = new ArrayList<>();
			final List<String> pending     = new ArrayList<>();
			final ByteArrayOutputStream line = new ByteArrayOutputStream();
			int count                      = 0;
			long position                  = end;

			while (position > 0 && count < maxLines) {

				final int size          = (int) Math.min(CHUNK, position);
				final ByteBuffer buffer = ByteBuffer.allocate(size);

				position -= size;
				channel.read(buffer, position);

				final byte[] bytes = buffer.array();

				for (int i = size - 1; i >= 0 && count < maxLines; i--) {

					// the newline that ends the region belongs to its last line
					if (bytes[i] == '\n' && position + i != end - 1) {

						count += collect(decodeReversed(line), pending, found, minLevel, filter, entryStart);
						line.reset();

					} else if (bytes[i] != '\n') {

						line.write(bytes[i]);
					}
				}
			}

			// the first line of the file has no newline before it
			if (position == 0 && count < maxLines && line.size() > 0) {

				collect(decodeReversed(line), pending, found, minLevel, filter, entryStart);
			}

			// lines before the first timestamp of the file form an entry without a level
			if (position == 0 && count < maxLines && !pending.isEmpty()) {

				addIfMatches(new ArrayList<>(pending), found, minLevel, filter);
			}

			Collections.reverse(found);

			return new Result(flatten(found), end, false);
		}
	}

	public static boolean isCompressed(final Path file) {

		return file.getFileName().toString().endsWith(".gz");
	}

	/**
	 * Like tail(), for a rotated log that was compressed with gzip: it can only be read from the start,
	 * so it is read through once, keeping no more than the last entries that match.
	 */
	public static Result tailCompressed(final Path file, final int maxLines, final Level minLevel, final String filter) throws IOException {

		return tailCompressed(file, maxLines, minLevel, filter, SERVER_LOG_ENTRY);
	}

	public static Result tailCompressed(final Path file, final int maxLines, final Level minLevel, final String filter, final Pattern entryStart) throws IOException {

		final Deque<List<String>> kept = new ArrayDeque<>();
		List<String> entry             = new ArrayList<>();
		int count                      = 0;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {

			String line;

			while ((line = reader.readLine()) != null) {

				if (entryStart.matcher(line).find() && !entry.isEmpty()) {

					count = keep(entry, kept, count, maxLines, minLevel, filter);
					entry = new ArrayList<>();
				}

				entry.add(line);
			}
		}

		if (!entry.isEmpty()) {

			keep(entry, kept, count, maxLines, minLevel, filter);
		}

		return new Result(flatten(new ArrayList<>(kept)), -1L, false);
	}

	/**
	 * Returns the entries that match after the given offset, oldest first, until they add up to at least
	 * maxLines lines. The offset of the result points to the first entry not yet examined, so nothing is
	 * skipped when there are more.
	 */
	public static Result since(final Path file, final long offset, final int maxLines, final Level minLevel, final String filter) throws IOException {

		return since(file, offset, maxLines, minLevel, filter, SERVER_LOG_ENTRY);
	}

	public static Result since(final Path file, final long offset, final int maxLines, final Level minLevel, final String filter, final Pattern entryStart) throws IOException {

		try (final FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {

			final long end         = endOfCompleteLines(channel, channel.size());
			final boolean rotated  = offset > channel.size();
			final long start       = rotated ? 0 : Math.min(offset, end);
			final List<String> out = new ArrayList<>();
			final List<String> entry = new ArrayList<>();
			final ByteArrayOutputStream line = new ByteArrayOutputStream();
			long entryOffset       = start;
			long position          = start;

			while (position < end) {

				final int size          = (int) Math.min(CHUNK, end - position);
				final ByteBuffer buffer = ByteBuffer.allocate(size);

				channel.read(buffer, position);

				final byte[] bytes = buffer.array();

				for (int i = 0; i < size; i++) {

					if (bytes[i] != '\n') {

						line.write(bytes[i]);
						continue;
					}

					final String text     = line.toString(StandardCharsets.UTF_8);
					final long lineStart  = position + i - line.size();

					line.reset();

					if (entryStart.matcher(text).find() && !entry.isEmpty()) {

						if (matches(entry, minLevel, filter)) {

							out.addAll(entry);
						}

						entry.clear();
						entryOffset = lineStart;

						if (out.size() >= maxLines) {

							return new Result(out, entryOffset, rotated);
						}
					}

					entry.add(text);
				}

				position += size;
			}

			if (!entry.isEmpty() && matches(entry, minLevel, filter)) {

				out.addAll(entry);
			}

			return new Result(out, end, rotated);
		}
	}

	// ----- private methods -----
	private static int keep(final List<String> entry, final Deque<List<String>> kept, final int count, final int maxLines, final Level minLevel, final String filter) {

		if (!matches(entry, minLevel, filter)) {

			return count;
		}

		int total = count + entry.size();

		kept.addLast(entry);

		// the oldest entry goes once the others reach maxLines without it
		while (kept.size() > 1 && total - kept.peekFirst().size() >= maxLines) {

			total -= kept.removeFirst().size();
		}

		return total;
	}

	private static int collect(final String text, final List<String> pending, final List<List<String>> found, final Level minLevel, final String filter, final Pattern entryStart) {

		pending.add(text);

		if (!entryStart.matcher(text).find()) {

			return 0;
		}

		// read backwards, so the header comes last
		final List<String> entry = new ArrayList<>(pending);

		Collections.reverse(entry);
		pending.clear();

		return addIfMatches(entry, found, minLevel, filter);
	}

	private static int addIfMatches(final List<String> entry, final List<List<String>> found, final Level minLevel, final String filter) {

		if (matches(entry, minLevel, filter)) {

			found.add(entry);

			return entry.size();
		}

		return 0;
	}

	private static boolean matches(final List<String> entry, final Level minLevel, final String filter) {

		if (minLevel != null) {

			final Matcher matcher = LEVEL.matcher(entry.getFirst());
			if (!matcher.find() || Level.valueOf(matcher.group(1)).ordinal() < minLevel.ordinal()) {

				return false;
			}
		}

		if (filter != null && !filter.isEmpty()) {

			for (final String line : entry) {

				if (line.contains(filter)) {

					return true;
				}
			}

			return false;
		}

		return true;
	}

	private static List<String> flatten(final List<List<String>> entries) {

		final List<String> lines = new ArrayList<>();

		for (final List<String> entry : entries) {

			lines.addAll(entry);
		}

		return lines;
	}

	private static String decodeReversed(final ByteArrayOutputStream reversed) {

		final byte[] bytes = reversed.toByteArray();

		for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {

			final byte b = bytes[i];

			bytes[i] = bytes[j];
			bytes[j] = b;
		}

		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static long endOfCompleteLines(final FileChannel channel, final long size) throws IOException {

		long position = size;

		while (position > 0) {

			final int length        = (int) Math.min(CHUNK, position);
			final ByteBuffer buffer = ByteBuffer.allocate(length);

			position -= length;
			channel.read(buffer, position);

			final byte[] bytes = buffer.array();

			for (int i = length - 1; i >= 0; i--) {

				if (bytes[i] == '\n') {

					return position + i + 1;
				}
			}
		}

		return 0;
	}
}
