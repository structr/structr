package org.structr.function;

/**
 *
 */
public class NameAndContent {

	private String name = null;
	private String content = null;

	public NameAndContent(final String name, final String content) {
		this.name = name;
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public String getContent() {
		return content;
	}
}
