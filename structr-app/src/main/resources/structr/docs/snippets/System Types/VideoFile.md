# VideoFile

Extends File with specialized video handling. VideoFile inherits all standard file functionality – storage backends, metadata, permissions, indexing – and adds video-specific features like transcoding between formats, playback from specific timestamps, and streaming delivery.

## Details

Like all files, videos can live on different storage backends while keeping consistent metadata and permissions. Reference them in pages using standard HTML video tags. You can extend VideoFile with custom properties or create subtypes for specialized video categories.
