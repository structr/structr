/*
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
;exports.init = function(test, name, password){

    sections.push('Login with username and password.');

    casper.waitForSelector('#usernameField').then(function() {
        s.animatedType(this, '#usernameField', false, name);
    });

    casper.waitForSelector('#passwordField').then(function() {
        s.animatedType(this, '#passwordField', false, password);
    });

    casper.then(function() {
        s.mousePointer(casper, { left: 600, top: 400 });
        s.moveMousePointerTo(casper, '#loginButton');
    });

    casper.then(function() {
        this.click('#loginButton');
    });

    casper.waitForSelector('#dashboard', function() {

        test.assertEval(function() { return !($('#errorText').text() === 'Wrong username or password!'); });
        test.assertEval(function() { return $('#dashboard').is(':visible'); });

    });

    
};