/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

;exports.init = function(test, name) {
    
    // TODO: make sure current active page is pages-tab
    //test.assertEval(function() { return ($('#pages').hasClass('active')); });
    sections.push('A new page has been created. The page is automatically loaded into the preview window.');
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(5000);
    //casper.waitForSelector('#previewTabs li:nth-child(2)');

    if (name) {

        casper.then(function() {
            s.moveMousePointerTo(casper, '#previewTabs li:nth-child(3)');
        });

        casper.then(function() {
            this.click('#previewTabs li:nth-child(3)');
        });

        casper.wait(2000);

        casper.then(function() {
            s.animatedType(this, '#previewTabs li:nth-child(3) .new-name', false, name, true);
            //s.animatedType(this, '#previewTabs .page .name_', false, name, true);
        });

        casper.wait(2000);
    }
};