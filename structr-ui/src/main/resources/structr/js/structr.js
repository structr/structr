/* 
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


var Structr = function(restBaseUrl, username, password) {
    this.restBaseUrl = restBaseUrl;
    this.headers = {
        'X-User': username,
        'X-Password': password
    };
};

Structr.prototype.get = function(req, callback) {
    $.ajax({
        url: this.restBaseUrl + '/' + req,
        headers: this.headers,
        type: 'GET',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        statusCode: {
            200: function(data) {
                if (callback) {
                    callback(data.result);
                }
            },
            204: function() {
            },
            400: function(data, status, xhr) {
                console.log('Bad request: ' + data.responseText);
            },
            401: function(data, status, xhr) {
                console.log('Authentication required: ' + data.responseText);
            },
            403: function(data, status, xhr) {
                console.log('Forbidden: ' + data.responseText);
            },
            404: function(data, status, xhr) {
                _Crud.error('Not found: ' + data.responseText);
            },
            422: function(data, status, xhr) {
                console.log('Syntax error: ' + data.responseText);
            },
            500: function(data, status, xhr) {
                console.log('Internal error: ' + data.responseText);
            }
        }
    });
};