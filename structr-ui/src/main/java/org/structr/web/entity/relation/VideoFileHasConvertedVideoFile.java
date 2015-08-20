package org.structr.web.entity.relation;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */
public class VideoFileHasConvertedVideoFile extends OneToMany<VideoFile, VideoFile> {

	@Override
	public Class<VideoFile> getSourceType() {
		return VideoFile.class;
	}

	@Override
	public Class<VideoFile> getTargetType() {
		return VideoFile.class;
	}

	@Override
	public String name() {
		return "HAS_CONVERTED_VIDEO";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
