/*
 *  Copyright (C) 2010-2013 Axel Morgner
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

/**
 * JS library for interactive web applications built with Structr.
 * This file has to be present in a web page to make Structr widgets work.
 */

var structrRestUrl = '/structr/rest/'; // TODO: Auto-detect base URI
var buttonSelector = '[data-structr-action]';
var altKey = false, ctrlKey = false, shiftKey = false, eKey = false;

$(function() {

    var s = new StructrApp(structrRestUrl);
    s.activateButtons(buttonSelector);
    $(document).trigger('structr-ready');
    s.hideNonEdit();
    

    $(window).on('keydown', function(e) {
        var k = e.which;
        //console.log('before down', k, altKey, ctrlKey, shiftKey, eKey)
        if (k === 16)
            shiftKey = true;
        if (k === 17)
            altKey = true;
        if (k === 18)
            ctrlKey = true;
        if (k === 69)
            eKey = true;
        //console.log('after down', k, altKey, ctrlKey, shiftKey, eKey)
    });

    $(window).on('keyup', function(e) {
        var k = e.which;
        //console.log('before up', k, altKey, ctrlKey, shiftKey, eKey)
        if (k === 16)
            shiftKey = false;
        if (k === 17)
            altKey = false;
        if (k === 18)
            ctrlKey = false;
        if (k === 69)
            eKey = false;
        //console.log('after up', k, altKey, ctrlKey, shiftKey, eKey)
    });

});

/**
 * Base class for Structr apps
 * 
 * @param baseUrl
 * @returns {StructrApp}
 */
function StructrApp(baseUrl) {
    if (baseUrl) {
        structrRestUrl = baseUrl;
    }
    var s = this;
    var hideEditElements = {}; // store elements in edit mode
    var hideNonEditElements = {}; // store elements in not edit mode
    this.edit = false;
    this.data = {};
    this.btnLabel = undefined;
    
    /**
     * Bind 'click' event to all Structr buttons
     */
    this.activateButtons = function(sel) {
        $(sel).on('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var btn = $(this);
            disableButton(btn);
            s.btnLabel = s.btnLabel || btn.text();
            var a = btn.attr('data-structr-action').split(':');
            var action = a[0], type = a[1];
            var reload = btn.attr('data-structr-reload') === 'true';
            var attrString = btn.attr('data-structr-attributes');
            var attrs = (attrString ? attrString.split(',') : []).map(function(s) {
                return s.trim();
            });
            var id = btn.attr('data-structr-id');
            var container = $('[data-structr-id="' + id + '"]');

            if (action === 'create') {

                $.each(attrs, function(i, key) {
                    if (!s.data[id]) s.data[id] = {};
                    var val = $('[data-structr-name="' + key + '"]').val();
                    s.data[id][key] = val ? val.parseIfJSON() : val;
                });
                s.create(type, s.data[id], reload, function() {enableButton(btn)}, function() {enableButton(btn)});

            } else if (action === 'edit') {
                s.editAction(btn, id, attrs, reload, function() {enableButton(btn)}, function() {enableButton(btn)});

            } else if (action === 'cancel-edit') {
                s.cancelEditAction(btn, id, attrs, reload);

            } else if (action === 'delete') {
                var f = s.field($('[data-structr-attr="name"]', container));
                //console.log(f);
                //container.find('[data-structr-attr="name"]').val(); console.log(id, container, name)
                s.delete(id, btn.attr('data-structr-confirm') === 'true', reload, f ? f.val : undefined);

            } else {
                s.customAction(id, type, action, s.data[id], reload, function() {enableButton(btn)}, function() {enableButton(btn)});
            }
        });
    },

    this.editAction = function(btn, id, attrs, reload) {
        var container = $('[data-structr-id="' + id + '"]');
        
        //show edit elements and hide non-edit elements 
        s.hideEdit(container);
                
        $.each(attrs, function(i, key) {
            var el = $('[data-structr-attr="' + key + '"]', container);
            if (!el.length) {
                return;
            }
            
            var f = s.field(el);
            f.id = id;
            if (!s.data[id]) s.data[id] = {};
            s.data[id][key] = f.val;
            var anchor = el[0].tagName.toLowerCase() === 'a' ? el : el.parent('a');
            if (anchor.length) {
                var href = anchor.attr('href');
                anchor.attr('href', '').css({textDecoration: 'none'}).on('click', function() {
                    return false;
                });
            }
            //el.html(inputField(id, key, val));
                    
            // don't replace select elements
            var inp = s.input(el);
            if (inp && inp.is('select')) {
                return;
            }
            
            var i; //console.log(f.id, f.type, f.key, f.val);
            if (f.type === 'Boolean') {
                i = checkbox(f.id, f.type, f.key, f.val);
            } else {
                if (!f.val || f.val.indexOf('\n') === -1) {
                    i = inputField(f.id, f.type, f.key, f.val);
                } else {
                    i = textarea(f.id, f.key, f.val);
                }
            }

            el.html(i);
            var el = container.find('[data-structr-attr="' + f.key + '"]');
            var inp = s.input(el);
            inp.css({fontFamily: 'sans-serif'});
            
            //console.log('editAction: input element', inp);
            resizeInput(inp);
            
            if (anchor.length) {
                inp.attr('data-structr-href', href);
            }
            
            if (f.type === 'Boolean') {
//                inp.on('change', function(e) {
//                    s.save(s.field($(this)));
//                });
            } else {
                inp.on('keyup', function(e) {
                    s.checkInput(e, s.field(inp), $(this));
                });
            }

            if (f.type === 'Date') {
                inp.on('mouseup', function(event) {
                    event.preventDefault();
                    var self = $(this);
                    self.datetimepicker({
                        // ISO8601 Format: 'yyyy-MM-dd"T"HH:mm:ssZ'
                        separator: 'T',
                        dateFormat: 'yy-mm-dd',
                        timeFormat: 'HH:mm:ssz',
                    });
                    self.datetimepicker('show');
                    self.off('mouseup');
                });
            }
            
            
            
        });
        $('<button data-structr-action="save" data-structr-id="' + id + '">Save</button>').insertBefore(btn);
        $('button[data-structr-action="save"][data-structr-id="' + id + '"]', container).on('click', function() {
            s.saveAction(btn, id, attrs, reload);
        });
        btn.text('Cancel').attr('data-structr-action', 'cancel-edit');
        enableButton(btn); // 
    },
    
    this.saveAction = function(btn, id, attrs, reload) {
        var container = $('[data-structr-id="' + id + '"]');
        $.each(attrs, function(i, key) {
            
            var inp = s.input($('[data-structr-attr="' + key + '"]', container));
            var f = s.field(inp);

            if (key.contains('.')) {
                delete s.data[id][key];
                var prop = key.split('.');
                var local = prop[0];
                var related = prop[1];
                //console.log('related property (key, local, value)', key, local, related, f);
                
                key = local;
                s.data[id][local] = {};
                s.data[id][local][related] = f.val;
                
            } else if (f && f.type === 'Boolean') {
                
                s.data[id][key] = (f.val === true ? true : false);
                
            } else {
                
                if (f && f.val && f.val.length) {
                    s.data[id][key] = f.val;
                } else {
                    s.data[id][key] = null;
                }
                
            }
        });
        //console.log('PUT', structrRestUrl + id, s.data[id]);
        s.request('PUT', structrRestUrl + id, s.data[id], false, 'Successfully updated ' + id, 'Could not update ' + id, function() {
            s.cancelEditAction(btn, id, attrs, reload);
        }, function(data) {
            if (data && data.status === 404) {
                // TODO: handle related properties more flexible
//                var response = JSON.parse(data.responseText);
//                Object.keys(response.errors).forEach(function(type) {
//                });
            }
            
        });
    },
    
    this.cancelEditAction = function(btn, id, attrs, reload) {
        if (reload) {
            window.location.reload();
        } else {
            var container = $('[data-structr-id="' + id + '"]');
            $.each(attrs, function(i, key) {
                var inp = s.input($('[data-structr-attr="' + key + '"]', container));
                if (inp && inp.is('select')) {
                    return;
                }
                var href = inp.attr('data-structr-href');
                var anchor = inp.parent('a');
                if (href && anchor.length) {
                    anchor.attr('href', href);
                }
                inp.replaceWith(s.data[id][key]);
            });
            // clear data
            $('button[data-structr-id="' + id + '"][data-structr-action="save"]').remove();
            btn.text(s.btnLabel).attr('data-structr-action', 'edit');
            enableButton(btn);
            
            //hide non edit elements and show edit elements
            s.hideNonEdit(container);
        }
    },
    
    this.input = function(elements) {
        var el = $(elements[0]);
        var inp;
        if (el.is('input') || el.is('textarea') || el.is('select')) {
            //console.log('el is input or textarea', el);
            return el;
        } else {
            inp = el.children('textarea');
            if (inp.length) {
                //console.log('inp is textarea', inp);
                return inp;
            } else {
                inp = el.children('input');
                if (inp.length) {
                    //console.log('inp is input field', inp);
                    return inp;
                } else {
                    inp = el.children('select');
                    if (inp.length) {
                        //console.log('inp is select element', inp);
                        return inp;
                    } else {
                        //console.log('no input found');
                        return null;
                    }
                }
            }
        }
    },
    
    this.field = function(el) {
        if (!el || !el.length) return;
        var type = el.attr('data-structr-type') || 'String', id = el.attr('data-structr-id'), key = el.attr('data-structr-attr'), rawVal = el.attr('data-structr-raw-value');
        var val;
        if (type === 'Boolean') {
            if (el.is('input')) {
                val = el.is(':checked');
            } else {
                val = (el.text() === 'true');
            }
        } else {
            var inp = s.input(el);
            if (inp) {
                if (inp.is('select')) {
                    var selection = $(':selected', inp); 
                    val = selection.attr('value');
                } else {
                    val = rawVal || (inp.val() && inp.val().replace(/<br>/gi, '\n'));
                }
            } else {
                val = rawVal || el.html().replace(/<br>/gi, '\n');
                //val = rawVal || el.text();
            }
        }
        //console.log(el, type, id, key, val);
        return {'id': id, 'type': type, 'key': key, 'val': val, 'rawVal': rawVal};
    };

    this.getRelatedType = function(type, key, callback) {
        s.request('GET', structrRestUrl + '_schema', null, false, null, null, function(data) {
            //console.log(data);
        });
    },

    this.create = function(type, data, reload, successCallback, errorCallback) {
        //console.log('Create', type, data, reload);
        s.request('POST', structrRestUrl + type.toUnderscore(), data, reload, 'Successfully created new ' + type, 'Could not create ' + type, successCallback, errorCallback);
    };

    this.customAction = function(id, type, action, data, reload, successCallback, errorCallback) {
        //console.log('Create', type, data, reload);
        s.request('POST', structrRestUrl + type.toUnderscore() + '/' + id + '/' + action, data, reload, 'Successfully execute custom action ' + action, 'Could not execute custom action ' + type, successCallback, errorCallback);
    };

    this.request = function(method, url, data, reload, successMsg, errorMsg, onSuccess, onError) {
        var dataString = JSON.stringify(data);
        //console.log(dataString);
        $.ajax({
            type: method,
            url: url,
            data: dataString,
            contentType: 'application/json; charset=utf-8',
            statusCode: {
                200: function(data) {
                    s.dialog('success', successMsg);
                    if (reload) {
                        window.setTimeout(function() {
                            window.location.reload();
                        }, 200);
                    } else {
                        if (onSuccess) {
                            onSuccess(data);
                        }
                    }
                },
                201: function(data) {
                    s.dialog('success', successMsg);
                    if (reload) {
                        window.setTimeout(function() {
                            window.location.reload();
                        }, 200);
                    } else {
                        if (onSuccess) {
                            onSuccess();
                        }
                    }
                },
                400: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError();
                    }
                },
                401: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError();
                    }
                },
                403: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError();
                    }
                },
                404: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError(data);
                    }
                },
                422: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError();
                    }
                },
                500: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                    if (onError) {
                        onError();
                    }
                }
            }
        });
    },

    this.dialog = function(type, msg) {
        var el = $('[data-structr-dialog="' + type + '"]');
        el.addClass(type).html(msg).show().delay(2000).fadeOut(200);
    };

    this.add = function(id, sourceId, sourceType, relatedProperty) {

        var d = JSON.parse('{"' + relatedProperty + '":[{"id":"' + id + '"}]}');

        $.ajax({
            url: structrRestUrl + sourceType.toUnderscore() + '/' + sourceId + '/ui', method: 'GET', contentType: 'application/json',
            statusCode: {
                200: function(data) {
                    //console.log(data.result, data.result[relatedProperty], d);

                    if (data.result[relatedProperty] === undefined) {
                        alert('Could not read related property\n\n    ' + sourceType + '.' + relatedProperty + '\n\nMake sure it is contained in the\nentity\'s ui view and readable via REST.');
                        return;
                    }

                    if (data.result[relatedProperty].length) {
                        $.each(data.result[relatedProperty], function(i, obj) {
                            d[relatedProperty].push({'id': obj.id});
                        });
                    }

                    $.ajax({
                        url: structrRestUrl + sourceType.toUnderscore() + '/' + sourceId, method: 'PUT', contentType: 'application/json',
                        data: JSON.stringify(d),
                        statusCode: {
                            200: function() {
                                window.location.reload();
                            },
                            400: function(xhr) {
                                console.log(xhr);
                            },
                            422: function(xhr) {
                                console.log(xhr);
                            }
                        }
                    });

                }
            }
        });

    };

    this.remove = function(id, sourceId, sourceType, relatedProperty) {

        var d = JSON.parse('{"' + relatedProperty + '":[]}');

        $.ajax({
            url: structrRestUrl + sourceId + '/ui', method: 'GET', contentType: 'application/json',
            statusCode: {
                200: function(data) {

                    if (data.result[relatedProperty] === undefined) {
                        alert('Could not read related property\n\n    ' + sourceType + '.' + relatedProperty + '\n\nMake sure it is contained in the\nentity\'s ui view and readable via REST.');
                        return;
                    }

                    if (data.result[relatedProperty].length) {
                        $.each(data.result[relatedProperty], function(i, obj) {
                            if (obj.id !== id) {
                                d[relatedProperty].push({'id': obj.id});
                            }
                        });
                    }

                    //console.log(data.result[relatedProperty], d, JSON.stringify(d));

                    $.ajax({
                        //url: '/structr/rest/' + sourceType.toUnderscore() + '/' + sourceId, method: 'PUT', contentType: 'application/json',
                        url: structrRestUrl + sourceId, method: 'PUT', contentType: 'application/json',
                        data: JSON.stringify(d),
                        statusCode: {
                            200: function() {
                                window.location.reload();
                            },
                            400: function(xhr) {
                                console.log(xhr);
                            },
                            422: function(xhr) {
                                console.log(xhr);
                            }
                        }
                    });

                }
            }
        });

    };

    this.delete = function(id, conf, reload, name) {
        //console.log('Delete', id, conf, reload);
        var sure = true;
        if (conf) {
            sure = confirm('Are you sure to delete ' + (name ? name : id) + '?');
        }
        if (!conf || sure) {
            $.ajax({
                url: structrRestUrl + id, method: 'DELETE', contentType: 'application/json',
                statusCode: {
                    200: function() {
                        if (reload) {
                            window.location.reload();
                        }
                    }
                }
            });
        }
    };

    this.save = function(f, b) {
        //console.log(f, b);
        var obj = {};

        obj[f.key] = f.val;
        if (b) {
            b.html('<img src="/structr/img/al.gif"> Saving');
        }

        $.ajax({url: baseUrl + f.id, method: 'PUT', contentType: 'application/json', data: JSON.stringify(obj),
            statusCode: {
                200: function() {
                    if (b) {
                        b.text('Success!').remove();
                    }
                }
            }
        });
    };

    this.checkInput = function(e, f, inp) {
        var k = e.which;
        
        if (isTextarea(inp[0])) {
            
            if (inp.val().indexOf('\n') === -1) {

                var parent = inp.parent();
                
                // No new line in textarea content => transform to input field
                inp.replaceWith(inputField(f.id, f.type, f.key, inp.val()));
                inp = s.input(parent);

                inp.on('keyup', function(e) {
                    s.checkInput(e, f, $(this));
                });

                setCaretToEnd(inp[0]);
                
            }
            
        } else if (k === 13) {// && shiftKey === true) {

            // Return key in input field => replace by textarea
            var parent = inp.parent();

            inp.replaceWith(textarea(f.id, f.key, inp.val() + '\n'));
            inp = s.input(parent);

            inp.on('keyup', function(e) {
                s.checkInput(e, f, $(this));
            });

            inp.css({fontFamily: 'sans-serif'});

            setCaretToEnd(inp[0]);

        }

        resizeInput(inp);

    };
    this.appendSaveButton = function(b, p, inp, id, key) {

        // Remove existing save button
        if (b.length) {
            $('#save_' + id + '_' + key).remove();
        }

        //(p.length ? p : inp).after('<button class="saveButton" id="save_' + id + '_' + key + '">Save</button>');
        inp.after('<button class="save-button" id="save_' + id + '_' + key + '">Save</button>');
        $('#save_' + id + '_' + key).on('click', function() {
            var btn = $(this), inp = btn.prev();
            //console.log('append save button', btn, inp);
            s.save(s.field(inp), btn);
        });

    };
    this.hideEdit = function(container) {
        
        // show elements [data-structr-hide="non-edit"]
        $.each($('[data-structr-hide-id]', container), function() {
           var id = $(this).attr('data-structr-hide-id');
           $(this).replaceWith(hideNonEditElements[id]);
           delete hideNonEditElements[id];
           
        });
        
        // hide edit elements
        $.each($('[data-structr-hide="edit"]', container), function(i, obj) {
            
            var random = Math.floor(Math.random()*1000000+1);
            
            hideEditElements[random] = $(obj).clone(true,true);
            $(obj).replaceWith('<div style="display:none;" data-structr-hide-id="'+random+'"></div>');
        });
        
        $(document).trigger("structr-edit");
    };
    this.hideNonEdit = function(container) {
        
        //first call to hide all non-edit elements
        if (container === undefined){
            
            // hide all non-edit elements
            $.each($('[data-structr-hide="non-edit"]'), function(i, obj) { 
                
                var random = Math.floor(Math.random()*1000000+1);
                
                hideNonEditElements[random] = $(obj).clone(true,true);
                $(obj).replaceWith('<div style="display:none;" data-structr-hide-id="'+random+'"></div>');
            });
            
        } else {
            
            // show elements [data-structr-hide="edit"]
            $.each($('[data-structr-hide-id]', container), function() {
            
                var id = $(this).attr("data-structr-hide-id");
                $(this).replaceWith(hideEditElements[id]);
                delete hideNonEditElements[id];

             });
            
            // hide non-edit elements
            $.each($('[data-structr-hide="non-edit"]', container), function(i, obj) {
                
                var random = Math.floor(Math.random()*1000000+1);
                
                hideNonEditElements[random] = $(obj).clone(true,true);
                $(obj).replaceWith('<div style="display:none;" data-structr-hide-id="'+random+'"></div>');
                
            });
            
        }
    };
}
function resizeInput(inp) {

    var text = inp.val();// console.log(inp, 'value of input', text);
    // don't resize empty input elements with preset size
    if (!text.length && inp.attr('size')) return;

    if (isTextarea(inp[0])) {

        var n = (text.match(/\n/g) || []).length;
        inp.attr('rows', n+2);

        var lines = text.split('\n');
        var c = lines.sort(function(a, b) {
            return b.length - a.length;
        })[0].length;

        inp.attr('cols', c+1);

    } else {

        inp.attr('size', text.length + 1);

    }
    // Focus on last empty field
//    if (!text || !text.length)
//        inp.focus();
}

function urlParam(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var res = regex.exec(window.location.href);
    return (res && res.length ? res[1] : '');
}

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

String.prototype.parseIfJSON = function() {
    if (this.substring(0,1) === '{') {
        var cleaned = this.replace(/'/g, "\"");
        return JSON.parse(cleaned);
    }
    return this;
}

String.prototype.splitAndTitleize = function(sep) {

    var res = new Array();
    var parts = this.split(sep);
    parts.forEach(function(part) {
        res.push(part.capitalize());
    })
    return res.join(' ');
};

String.prototype.toUnderscore = function() {
    return this.replace(/([A-Z])/g, function(m, a, offset) {
        return (offset > 0 ? '_' : '') + m.toLowerCase();
    });
};

if (typeof String.prototype.contains !== 'function') {
    String.prototype.contains = function(pattern) {
        return this.indexOf(pattern) > 0;
    };
}

function setCaretToEnd(el) {
    var l = el.value.length;
    if (document.selection) {
        el.focus();
        var o = document.selection.createRange();
        o.moveStart('character', -l);
        o.moveStart('character', l);
        o.moveEnd('character', 0);
        o.select();
    } else if (el.selectionStart || el.selectionStart === 0 || el.selectionStart === '0') {
        el.selectionStart = l;
        el.selectionEnd = l;
        el.focus();
    }
}

function escapeTags(str) {
    if (!str)
        return str;
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function isTextarea(el) {
    return el.nodeName.toLowerCase() === 'textarea';
}

function textarea(id, key, val) {
    return '<textarea class="structr-input-text">' + val + '</textarea>';
}

function inputField(id, type, key, val) {
    var size = (val ? val.length : (type && type === 'Date' ? 25 : key.length));
    return '<input class="structr-input-text" type="text" placeholder="' + (key ? key.capitalize() : '')
            + '" value="' + (val === 'null' ? '' : val)
            + '" size="' + size + '">';
}

function field(id, type, key, val) {
    return '<span type="text" data-structr-type="' + type + '" data-structr-attr="' + key + '">' + val + '</span>';
}

function checkbox(id, type, key, val) {
    return '<input type="checkbox" data-structr-id="' + id + '" data-structr-type="' + type + '" ' + (val ? 'checked="checked"' : '') + '">';
}

function select(id, key, val, options) {
    var s = '<select data-structr-id="' + id + '">';
    $.each(options, function(i, o) {
        s += '<option ' + (o === val ? 'selected' : '') + '>' + o + '</option>';
    });
    return s + '</select>';
}

function enableButton(btn) {
  btn.removeClass('disabled');
  btn.removeAttr('disabled');
}

function disableButton(btn) {
  btn.addClass('disabled');
  btn.attr('disabled', 'disabled');
}
