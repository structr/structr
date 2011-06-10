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

    var heightOffset = 92;

    var treeArea = jQuery("#treeArea .body");
    var tabArea = jQuery(".tabArea .body");
    //  console.log(treeArea.length);
    //  console.log(tabArea.length);
    if (treeArea.length > 0) {
        treeArea.height(windowHeight - headerHeight - heightOffset);
        treeArea.width(200);
        tabArea.height(windowHeight - headerHeight - heightOffset);
        tabArea.width(windowWidth - 288);
    } else {
        //jQuery(".tabArea").css("left", 0);
        tabArea.height(windowHeight - headerHeight - heightOffset);
        tabArea.width(windowWidth - 50);
    }


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
        canvas.height = tabAreaBodyHeight + 12;
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
    jQuery.cookies.set("scrollTree", jQuery("#treeArea .body").scrollTop());
    jQuery.cookies.set("scrollCode", jQuery(".tabArea .CodeMirror-wrapping iframe").contents().scrollTop());
    jQuery.cookies.set("scrollIframe", jQuery("#rendition-tab iframe").contents().scrollTop());
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
    
    jQuery('#treeArea .header').mouseover(function() {
        var addIcon = jQuery('#addNodeIcon');
        addIcon.draggable({
            distance:    '5',
            containment: '#treeArea',
            helper:      'clone',
            scroll:      'true',
            revert:      'invalid',
            start: function() {
                jQuery('#treeArea .header').unbind('mouseout');
            }
        });       
        addIcon.show();
    //var deleteIcon = jQuery('#deleteNodeIcon');
    //deleteIcon.show();
    });

    jQuery('#treeArea .header').mouseout(function() {
        jQuery('#addNodeIcon').hide();
    //jQuery('#deleteNodeIcon').hide();
    });
    
    

});


function showDropSelect(node) {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: node.offset().top-4,
            left: node.offset().left-4
        },
        message: jQuery('#dropSelectDiv'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });

}


function showCreateSelect(node) {
    jQuery.blockUI.defaults.css = {};
    jQuery.blockUI({
        centerX: 0,
        centerY: 0,
        css: {
            width: '',
            top: node.offset().top-4,
            left: node.offset().left-4
        },
        message: jQuery('#createSelectDiv'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });

}

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
        message: jQuery('#newNodeFormDiv'),
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
        message: jQuery('#uploadFormDiv'),
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
        message: jQuery('#extractNodeFormDiv'),
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
        message: jQuery('#copyNodeFormDiv'),
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
        message: jQuery('#moveNodeFormDiv'),
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
        message: jQuery('#newRelationshipFormDiv'),
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
        message: jQuery('#editPropertiesFormDiv'),
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
        message: jQuery('#deleteNodeFormDiv'),
        baseZ: 9,
        fadeIn: 0,
        fadeOut: 0,
        overlayCSS:  {
            backgroundColor: '#000',
            opacity:         0.5
        }
    });
}



function getParam(name,url) {
    return decodeURI((RegExp(name + '=' + '(.+?)(&|$)').exec(url) || [,null])[1]);
}

function submitCreateNodeForm() {
    //  console.log(targetId);
    //  console.log(jQuery('#newNodeForm'));
    jQuery('#newNodeForm_targetNodeId').val(targetId);
    jQuery('#newNodeForm_createNewNode').val(' Create new node ');
    var name = jQuery('#nodeNameField').attr("value");   
    jQuery('#newNodeForm_name').val(name);                    
    var type = jQuery('#nodeTypeField').attr("value");
    jQuery('#newNodeForm_type').val(type);
    jQuery('#newNodeForm').submit();
}
      
function submitMoveNodeForm() {
    jQuery('#moveNodeForm_sourceNodeId').val(sourceId);
    if (parentNodeId && parentNodeId != 'null') {
        jQuery('#moveNodeForm_parentNodeId').val(parentNodeId);
    }
    jQuery('#moveNodeForm_targetNodeId').val(targetId);
    jQuery('#moveNodeForm_moveNode').val(' Move node ');
    jQuery('#moveNodeForm').submit();
}
      
function submitCopyNodeForm() {
    jQuery('#copyNodeForm_sourceNodeId').val(sourceId);
    jQuery('#copyNodeForm_targetNodeId').val(targetId);
    jQuery('#copyNodeForm_copyNode').val(' Copy node ');
    jQuery('#copyNodeForm').submit();
}
    
function submitLinkNodeForm() {
    jQuery('#newRelationshipForm_sourceNodeId').val(targetId);
    jQuery('#newRelationshipForm_targetNodeId').val(sourceId);
    var type = jQuery('#relTypeField').attr("value");
    jQuery('#newRelationshipForm_relType').val(type);
    jQuery('#newRelationshipForm_linkNode').val(' Link node ');
    jQuery('#newRelationshipForm').submit();
}

//jQuery('#deleteNodeIcon').click(function() {
//  showDeleteNodePanel()
//});
