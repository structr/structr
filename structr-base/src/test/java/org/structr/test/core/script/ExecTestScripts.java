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
package org.structr.test.core.script;

import org.structr.api.config.Settings;
import org.structr.core.function.ExecFunction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

/** Test scripts for exec() and execBinary(), registered under their keys and removed again afterwards. */
class ExecTestScripts {

	// per test class, since test classes run in parallel and remove their scripts when done
	final String printArgs;
	final String printArgsNoShebang;
	final String cat;
	final String fail;
	final String background;

	private static final String PRINT_ARGS_BODY = "for a in \"$@\"; do printf '[%s]\\n' \"$a\"; done\n";

	// the scripts folder is shared by the test classes, so the last one done removes it if one of them created it
	private static int users          = 0;
	private static Path createdFolder = null;

	private final List<Path> created = new ArrayList<>();
	private final String prefix;

	ExecTestScripts(final String prefix) {

		this.prefix             = prefix;
		this.printArgs          = "test.exec." + prefix + ".printargs";
		this.printArgsNoShebang = "test.exec." + prefix + ".printargs.noshebang";
		this.cat                = "test.exec." + prefix + ".cat";
		this.fail               = "test.exec." + prefix + ".fail";
		this.background         = "test.exec." + prefix + ".background";
	}

	void create() throws Exception {

		final Path folder = Paths.get(ExecFunction.SCRIPTS_FOLDER);

		synchronized (ExecTestScripts.class) {

			if (users++ == 0 && !Files.exists(folder)) {

				createdFolder = Files.createDirectories(folder);
			}
		}

		script(folder, printArgs,            "test-printargs.sh",           "#!/bin/sh\n" + PRINT_ARGS_BODY);
		script(folder, printArgsNoShebang, "test-printargs-noshebang.sh", PRINT_ARGS_BODY);
		script(folder, cat,                   "test-cat.sh",                 "#!/bin/sh\ncat\n");
		script(folder, fail,                  "test-fail.sh",                "#!/bin/sh\necho partial\nexit 3\n");

		// the background child inherits stdout and keeps the pipe open after the script has exited
		script(folder, background,            "test-background.sh",          "#!/bin/sh\necho before\nsleep 20 &\n");
	}

	void remove() throws Exception {

		for (final Path path : created) {

			Files.deleteIfExists(path);
		}

		synchronized (ExecTestScripts.class) {

			if (--users == 0 && createdFolder != null) {

				Files.deleteIfExists(createdFolder);
				createdFolder = null;
			}
		}
	}

	/** What the print-args script outputs for the given values: each in brackets, one per line. */
	static String printed(final List<String> values) {

		final StringBuilder buf = new StringBuilder();

		for (final String value : values) {

			buf.append('[').append(value).append("]\n");
		}

		return buf.toString();
	}

	private void script(final Path folder, final String key, final String name, final String content) throws Exception {

		final Path path = folder.resolve(prefix + "-" + name);

		Files.writeString(path, content);
		Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));

		created.add(0, path);

		Settings.createSettingForValue(Settings.miscGroup, key, path.getFileName().toString());
	}
}
