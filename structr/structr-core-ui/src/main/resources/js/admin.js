/* 
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

function setWindowHeightAndWidth(resize) {
    var windowHeight = jQuery(window).height();
    var windowWidth = jQuery(window).width();
    var headerHeight = jQuery("#header").height();

    var heightOffset = 70;

    jQuery("#treeArea .body").height(windowHeight - headerHeight - heightOffset);
    jQuery("#treeArea .body").width(200);

    jQuery(".tabArea .body").height(windowHeight - headerHeight - heightOffset - 32);
    jQuery(".tabArea .body").width(windowWidth - 288);

    var tabAreaBodyWidth = jQuery(".tabArea .body").width();
    var tabAreaBodyHeight = jQuery(".tabArea .body").height();

    jQuery(".tabArea .CodeMirror-wrapping").width(tabAreaBodyWidth - 34);
    jQuery(".tabArea .CodeMirror-wrapping").height(tabAreaBodyHeight - 70);

    jQuery("#rendition-tab iframe").width(tabAreaBodyWidth - 4);
    jQuery("#rendition-tab iframe").height(tabAreaBodyHeight - 42);

    jQuery("#source-tab textarea").width(tabAreaBodyWidth - 4);
    jQuery("#source-tab textarea").height(tabAreaBodyHeight - 42);

    var canvas = jQuery("#viewport").get(0);
    if (canvas) {
        canvas.width = tabAreaBodyWidth - 4;
        canvas.height = tabAreaBodyHeight - 42;
        if (graph) {
            graph.redraw();
        }
    }

    if (!resize) {
        jQuery("#treeArea .body").scrollTop(jQuery.cookies.get("scrollTree") || 0);
        //jQuery(".tabArea .body").scrollTop(jQuery.cookies.get("scrollMain") || 0);
        jQuery(".tabArea .CodeMirror-wrapping iframe").contents().scrollTop(jQuery.cookies.get("scrollCode") || 0);
        jQuery(".tabArea .CodeMirror-wrapping iframe").load(function() {
            jQuery(".tabArea .CodeMirror-wrapping iframe").contents().scrollTop(jQuery.cookies.get("scrollCode") || 0);
        });
        jQuery("#rendition-tab iframe").contents().scrollTop(jQuery.cookies.get("scrollIframe") || 0);
        jQuery("#rendition-tab iframe").load(function() {
            jQuery("#rendition-tab iframe").contents().scrollTop(jQuery.cookies.get("scrollIframe") || 0);
        });
        jQuery("#source-tab textarea").scrollTop(jQuery.cookies.get("scrollTextarea") || 0);
    }

}


jQuery.noConflict();

jQuery(window).resize(function() {
    setWindowHeightAndWidth(true);
});

window.onbeforeunload = function () {
    //alert(jQuery("#treeArea .body").scrollTop());
    jQuery.cookies.set("scrollTree", jQuery("#treeArea .body").scrollTop());
    //jQuery.cookies.set("scrollMain", jQuery(".tabArea .body").scrollTop());
    jQuery.cookies.set("scrollCode", jQuery(".tabArea .CodeMirror-wrapping iframe").contents().scrollTop());
    //alert(jQuery(".tabArea .CodeMirror-wrapping iframe").contents().scrollTop());
    jQuery.cookies.set("scrollIframe", jQuery("#rendition-tab iframe").contents().scrollTop());
    //alert(jQuery("#rendition-tab iframe").contents().scrollTop());
    jQuery.cookies.set("scrollTextarea", jQuery("#source-tab textarea").scrollTop());
}

jQuery(document).ready(function() {

    jQuery("#tabs").tabs();

    setWindowHeightAndWidth(false);

    jQuery('.unblock').click(function() {
        jQuery.unblockUI({
            fadeOut: 0
        });
    });

    jQuery('#toggleNewNodeForm').click(function() {
        showNewNodePanel()
    });

});

function showNewNodePanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleNewNodeForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleNewNodeForm').offset().left-4
        },
        message: jQuery('#newNodeForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });

}

function showUploadPanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleUploadForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleUploadForm').offset().left-4
        },
        message: jQuery('#uploadForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}

function showExtractNodePanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleExtractNodeForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleExtractNodeForm').offset().left-4
        },
        message: jQuery('#extractNodeForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}

function showCopyNodePanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleCopyNodeForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleCopyNodeForm').offset().left-4
        },
        message: jQuery('#copyNodeForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}

function showMoveNodePanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleMoveNodeForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleMoveNodeForm').offset().left-4
        },
        message: jQuery('#moveNodeForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}

function showNewRelationshipPanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleNewRelationshipForm').offset().top-4,
            //    + jQuery('#toggleNewRelationshipForm').height(),
            left: jQuery('#toggleNewRelationshipForm').offset().left-4
        },
        message: jQuery('#newRelationshipForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}

function showEditPropertiesPanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleEditPropertiesForm').offset().top-4,
            //    + jQuery('#toggleNewRelationshipForm').height(),
            left: jQuery('#toggleEditPropertiesForm').offset().left-4
        },
        message: jQuery('#editPropertiesForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });

}

function showDeleteNodePanel() {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: jQuery('#toggleDeleteNodeForm').offset().top-4,
            //    + jQuery('#toggleNewNodeForm').height(),
            left: jQuery('#toggleDeleteNodeForm').offset().left-4
        },
        message: jQuery('#deleteNodeForm'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}
