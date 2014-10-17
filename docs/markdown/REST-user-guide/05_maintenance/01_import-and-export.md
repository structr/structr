As superuser, you can issue the following REST call to export the content to a ZIP file.

    POST /maintenance/sync '{"mode":"export", "file":"export.zip"}'

To leave out the files, use exportDb as mode.

To import, simply issue

    POST /maintenance/sync '{"mode":"import", "file":"export.zip"}'

If you only want to import the schema, extract the ZIP, open the db file with a text editor, remove all but the lines with SchemaNode and SchemaRelationship, and update the ZIP with the reduced db file.

There's a trick how you can import data into a remote instance with HTTP access only: Just upload the ZIP file using Structr's UI, then look up the relative file path, and if you know the installation path of the remote Structr instance, you can do

    POST /maintenance/sync '{"mode":"import", "file":"/usr/lib/structr/files/0/9/e/7/09e75d08c8a642e0ad3353ffc9dbc908"}'
