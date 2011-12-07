<!--
 Copyright (C) 2011 Axel Morgner

 This file is part of structr <http://structr.org>.

 structr is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 structr is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with structr.  If not, see <http://www.gnu.org/licenses/>.
-->
<!DOCTYPE html>
<html>
    <head>
        <title>WebSocket Test</title>
        <%@include file="include/header.jsp" %>
        <script src="js/websocket.js" type="text/javascript"></script>
    </head>
    <body>
        <button onclick='connect()'>Connect</button><br />
        <button onclick='send("{\"uuid\":\"123\",\"command\":\"UPDATE\"}")'>Send</button>
        <div id="log"></div>
    </body>
</html>