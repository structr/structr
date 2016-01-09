/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.apache.sshd.common.file.SshFile;

/**
 *
 *
 */
public class NullFile implements SshFile {

	@Override
	public String getAbsolutePath() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
		return null;
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
		return null;
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
	}

	@Override
	public String readSymbolicLink() throws IOException {
		return null;
	}

	@Override
	public void createSymbolicLink(SshFile destination) throws IOException {
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean doesExist() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public boolean isExecutable() {
		return false;
	}

	@Override
	public boolean isRemovable() {
		return false;
	}

	@Override
	public SshFile getParentFile() {
		return this;
	}

	@Override
	public long getLastModified() {
		return -1;
	}

	@Override
	public boolean setLastModified(long time) {
		return false;
	}

	@Override
	public long getSize() {
		return -1;
	}

	@Override
	public boolean mkdir() {
		return false;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public boolean create() throws IOException {
		return false;
	}

	@Override
	public void truncate() throws IOException {
	}

	@Override
	public boolean move(SshFile destination) {
		return false;
	}

	@Override
	public List<SshFile> listSshFiles() {
		return null;
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		return null;
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		return null;
	}

	@Override
	public void handleClose() throws IOException {
	}
}
