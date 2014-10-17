To rebuild the Lucene index (keyword/fulltext), you can use the following command:

    POST /maintenance/rebuildIndex '{}'

You can restrict the command to be applied to nodes or relationships only:

    POST /maintenance/rebuildIndex '{"mode":"nodesOnly"}'


    POST /maintenance/rebuildIndex '{"mode":"relsOnly"}'

