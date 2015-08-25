package org.structr.web.entity.relation;

import org.structr.core.entity.OneToOne;
import org.structr.core.entity.Relation;
import org.structr.web.entity.Image;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */
public class VideoFileHasPosterImage extends OneToOne<VideoFile, Image> {

	@Override
	public Class<VideoFile> getSourceType() {
		return VideoFile.class;
	}

	@Override
	public Class<Image> getTargetType() {
		return Image.class;
	}

	@Override
	public String name() {
		return "HAS_POSTER_IMAGE";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
