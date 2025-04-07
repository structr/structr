/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.script;

/**
 * A small piece of JavaScript code that can either be
 * compiled or run directly.
 */
public class Snippet {

	private String codeSource        = null;
	private String name              = null;
	private String source            = null;
	private String transcribedSource = null;
	private String mimeType          = null;
	private String engineName        = null;
	private boolean embed            = true;
	private int startRow             = 0;

	public Snippet(final String name, final String source) {

		this.source = source;
		this.name   = name;
		this.embed  = false;
	}

	public Snippet(final String name, final String source, final boolean embed) {

		this.source = source;
		this.name   = name;
		this.embed  = embed;
	}

	public String getName() {
		return name;
	}

	public String getSource() {
		return source;
	}

	public boolean embed() {
		return this.embed;
	}

	public void setStartRow(final int startRow) {
		this.startRow = startRow;
	}

	public int getStartRow() {
		return this.startRow;
	}

	public void setCodeSource(final String codeSource) {
		this.codeSource = codeSource;
	}

	public String getCodeSource() {
		return codeSource;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public String getTranscribedSource() {
		return this.transcribedSource;
	}

	public void setTranscribedSource(final String transcribedSource) {
		this.transcribedSource = transcribedSource;
	}

	public String getEngineName() {
		return this.engineName;
	}

	public void setEngineName(final String engineName) {
		this.engineName = engineName;
	}
}
