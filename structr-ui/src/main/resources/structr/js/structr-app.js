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
 * 
 * This file has to be present in a web page to make Structr widgets work.
 * 
 * v 1.0.0
 * 
 */

var structrRestUrl = '/structr/rest/'; // TODO: Auto-detect base URI
var buttonSelector = '[data-structr-action]';
var altKey = false, ctrlKey = false, shiftKey = false, eKey = false;

$(function() {

    var s = new StructrApp(structrRestUrl);

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
    this.edit = false;
    this.data = {};
    this.btnLabel = undefined;
    
    /**
     * Bind 'click' event to all Structr buttons
     */
    $(buttonSelector).on('click', function() {
        var btn = $(this);
        s.btnLabel = s.btnLabel || btn.text();
        var a = btn.attr('data-structr-action').split(':');
        var action = a[0], type = a[1];
        var reload = btn.attr('data-structr-reload') === 'true';
        var attrString = btn.attr('data-structr-attributes');
        var attrs = (attrString ? attrString.split(',') : []).map(function(s) {
            return s.trim();
        });
        var id = btn.attr('data-structr-id');
        var container = $('[data-structr-container="' + id + '"]');

        if (action === 'create') {

            $.each(attrs, function(i, key) {
                s.data[key] = container.find('[data-structr-attr="' + key + '"]').val();
            });
            s.create(type, s.data, reload);

        } else if (action === 'edit') {
            
            s.editAction(btn, id, attrs, reload);
            
        } else if (action === 'cancel-edit') {
            
            s.cancelEditAction(btn, id, attrs, reload);
            
        } else if (action === 'delete') {
            var f = s.field($('[data-structr-attr="name"]', container));
            //console.log(f);
            //container.find('[data-structr-attr="name"]').val(); console.log(id, container, name)
            s.delete(id, btn.attr('data-structr-confirm') === 'true', reload, f ? f.val : undefined);

        }
    });
    
    this.editAction = function(btn, id, attrs, reload) {
        var container = $('[data-structr-container="' + id + '"]');
        $.each(attrs, function(i, key) {
            var el = $('[data-structr-attr="' + key + '"]', container);
            if (!el.length) {
                return;
            }
            
            var f = s.field(el);
            f.id = id;
            
            s.data[key] = f.val;
            var anchor = el[0].tagName.toLowerCase() === 'a' ? el : el.parent('a');
            if (anchor.length) {
                var href = anchor.attr('href');
                anchor.attr('href', '').css({textDecoration: 'none'}).on('click', function() {
                    return false;
                });
            }
            //el.html(inputField(id, key, val));
            
            var i; //console.log(f.id, f.type, f.key, f.val);
            if (f.type === 'Boolean') {
                i = checkbox(f.id, f.type, f.key, f.val);
            }
            else {
                if (f.val.indexOf('\n') === -1) {
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
            
            
        });
        $('<button data-structr-action="save" data-structr-id="' + id + '" class="structr-button">Save</button>').insertBefore(btn);
        $('button[data-structr-action="save"][data-structr-id="' + id + '"]', container).on('click', function() {
            s.saveAction(btn, id, attrs, reload);
        });
        btn.text('Cancel').attr('data-structr-action', 'cancel-edit');
    },
    
    this.saveAction = function(btn, id, attrs, reload) {
        var container = $('[data-structr-container="' + id + '"]');
        $.each(attrs, function(i, key) {
            var inp = s.input($('[data-structr-attr="' + key + '"]', container));
            var f = s.field(inp);
            s.data[key] = f.val; console.log(f, f.val);
        });
        s.request('PUT', structrRestUrl + id, s.data, false, 'Successfully updated ' + id, 'Could not update ' + id, function() {
            s.cancelEditAction(btn, id, attrs, reload);
        });
    },
    
    this.cancelEditAction = function(btn, id, attrs, reload) {
        if (reload) {
            window.location.reload();
        } else {
            var container = $('[data-structr-container="' + id + '"]');
            $.each(attrs, function(i, key) {
                var inp = $('[data-structr-attr="' + key + '"]', container);
                var href = inp.attr('data-structr-href');
                var anchor = inp.parent('a');
                if (href && anchor.length) {
                    anchor.attr('href', href);
                }
                inp.replaceWith(s.data[key]);
                
            });
            $('button[data-structr-action="save"]').remove();
            btn.text(s.btnLabel).attr('data-structr-action', 'edit');
        }
    },
    
    this.input = function(elements) {
        var el = $(elements[0]);
        var inp;
        if (el.is('input') || el.is('textarea')) {
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
                    //console.log('no input found');
                    return null;
                }
            }
        }
    },
    
    this.field = function(el) {
        var type = el.attr('data-structr-type') || 'String', id = el.attr('data-structr-id'), key = el.attr('data-structr-attr');
        var val;
        if (type === 'Boolean') {
            if (el.is('input')) {
                val = el.is(':checked');
            } else {
                val = el.text() === 'true';
            }
        } else {
            var inp = s.input(el);
            if (inp) {
                val = inp.val().replace(/<br>/gi, '\n');
            } else {
                val = el.html().replace(/<br>/gi, '\n');
            }
        }
        //console.log(el, type, id, key, val);
        return {'id': id, 'type': type, 'key': key, 'val': val};
    };


    this.create = function(type, data, reload) {
        //console.log('Create', type, data, reload);
        s.request('POST', structrRestUrl + type.toUnderscore(), data, reload, 'Successfully created new ' + type, 'Could not create ' + type);
    };

    this.request = function(method, url, data, reload, successMsg, errorMsg, callback) {
        $.ajax({
            type: method,
            url: url,
            data: JSON.stringify(data),
            contentType: 'application/json; charset=utf-8',
            statusCode: {
                200: function(data) {
                    s.dialog('success', successMsg);
                    if (reload) {
                        window.setTimeout(function() {
                            window.location.reload();
                        }, 200);
                    }
                    if (callback) {
                        callback();
                    }
                },
                201: function(data) {
                    s.dialog('success', successMsg);
                    if (reload) {
                        window.setTimeout(function() {
                            window.location.reload();
                        }, 200);
                    }
                    if (callback) {
                        callback();
                    }
                },
                400: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                },
                401: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                },
                403: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                },
                404: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                },
                422: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
                },
                500: function(data, status, xhr) {
                    s.dialog('error', errorMsg + ': ' + data.responseText);
                    console.log(data, status, xhr);
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
        console.log('Delete', id, conf, reload);
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

    this.editable = function(enable) {

        if (enable === undefined) {
            return s.edit;
        }
        s.edit = enable;

        if (enable) {

            $('.createButton').show();
            $('.deleteButton').show();

            $('[data-structr-type="Date"],[data-structr-type="String"],[data-structr-type="Integer"],[data-structr-type="Boolean"]').each(function() {
                if (!$(this).closest('body').length)
                    return;
                var sp = $(this), f = s.field(sp), p = sp.parent('a');
                if (p) {
                    p.attr('href', '').css({textDecoration: 'none'}).on('click', function() {
                        return false;
                    });
                }
                var i; //console.log(f.id, f.type, f.key, f.val);
                if (f.type === 'Boolean') {
                    i = checkbox(f.id, f.type, f.key, f.val);
                }
                else {
                    if (f.val.indexOf('\n') === -1) {
                        i = inputField(f.id, f.type, f.key, f.val);
                    } else {
                        i = textarea(f.id, f.key, f.val);
                    }
                }

                sp.replaceWith(i);
                var input = $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]')
                input.css({fontFamily: 'sans-serif'});

                resizeInput(input);

                if (f.type === 'Boolean') {
                    input.on('change', function(e) {
                        s.save(s.field($(this)));
                    });
                } else {
                    input.on('keyup', function(e) {
                        s.checkInput(e, f, $(this));
                    });
                }

                var relatedNode = input.closest('[data-structr-data-type]');
                var parentType = relatedNode.attr('data-structr-data-type');

                var deleteButton = $('.deleteButton[data-structr-id="' + f.id + '"]');

                if (parentType && !(deleteButton.length)) {
                    relatedNode.append('<div class="clear"></div><button class="deleteButton" data-structr-id="' + f.id + '">Delete ' + parentType + '</button>');
                }


            });


            $('[data-structr-related-property]:not(.createButton)').each(function(i, v) {

                var relatedNode = $(this);
                var relatedProperty = relatedNode.attr('data-structr-related-property');
                var parentType = relatedNode.attr('data-structr-data-type');
                var sourceType = relatedNode.attr('data-structr-source-type');
                var sourceId = relatedNode.attr('data-structr-source-id');
                var id = relatedNode.attr('data-structr-data-id');
                var removeButton = $('.removeButton[data-structr-id="' + id + '"]');
                //console.log(id, sourceId, sourceType, relatedProperty);
                if (relatedProperty && sourceId && !(removeButton.length)) {
                    $('<button class="removeButton" data-structr-id="' + id + '" data-structr-source-id="' + sourceId + '">Remove ' + parentType + ' from ' + relatedProperty + '</button>').insertAfter(relatedNode);

                    $('.removeButton[data-structr-id="' + id + '"][data-structr-source-id="' + sourceId + '"]').on('click', function() {
                        s.remove(id, sourceId, sourceType, relatedProperty);
                    });

                }

            });



        } else {
            $('.createButton').hide();
            $('.deleteButton').hide();
            $('[data-structr-type="Date"],[data-structr-type="String"],[data-structr-type="Integer"],[data-structr-type="Boolean"]').each(function() {
                var inp = $(this), f = s.field(inp), p = inp.parent('a');
                //console.log(inp, f, p);
                inp.next('button').remove();
                if (p) {
                    p.attr('href', f.val).css({textDecoration: ''}).on('click', function() {
                        return true;
                    });
                }
                inp.replaceWith(field(f.id, f.type, f.key, f.val));
            });
        }
        return s.edit;
    };
    this.checkInput = function(e, f, inp) {
        var k = e.which, b = $('#save_' + f.id + '_' + f.key);
        if (isTextarea(inp[0])) {
            if (inp.val().indexOf('\n') === -1) {

                // No new line in textarea content => transform to input field
                inp.replaceWith(inputField(f.id, f.type, f.key, inp.val()));
                var parent = inp.parent();
                inp = s.input(parent.find('[data-structr-attr="' + f.key + '"]'));
                inp.on('keyup', function(e) {
                    s.checkInput(e, f, $(this));
                });

                setCaretToEnd(inp[0]);
            }
        } else {

            // Input field here
            if (k === 13) {// && shiftKey === true) {

                // Shift-return in input field => make textarea and append line break
                var parent = inp.parent();
                inp.replaceWith(textarea(f.id, f.key, inp.val()));
                inp = s.input(parent.find('[data-structr-attr="' + f.key + '"]'));

                inp.on('keyup', function(e) {
                    s.checkInput(e, f, $(this));
                });

                inp.css({fontFamily: 'sans-serif'});

                setCaretToEnd(inp[0]);

            }
        }

        resizeInput(inp);

        if (!(k === 13 && shiftKey) && ((k < 46 && k > 32) || (k > 9 && k < 32))) {
            return true;
        }

    };
    this.appendSaveButton = function(b, p, inp, id, key) {

        // Remove existing save button
        if (b.length) {
            $('#save_' + id + '_' + key).remove();
        }

        //(p.length ? p : inp).after('<button class="saveButton" id="save_' + id + '_' + key + '">Save</button>');
        inp.after('<button class="saveButton" id="save_' + id + '_' + key + '">Save</button>');
        $('#save_' + id + '_' + key).on('click', function() {
            var btn = $(this), inp = btn.prev();
            //console.log('append save button', btn, inp);
            s.save(s.field(inp), btn);
        });

    };
}
function resizeInput(inp) {

    var text = inp.val();// console.log(inp, 'value of input', text);

    if (isTextarea(inp[0])) {

        var n = (text.match(/\n/g) || []).length;
        inp.prop('rows', n+1);

        var lines = text.split('\n');
        var c = lines.sort(function(a, b) {
            return b.length - a.length;
        })[0].length;

        inp.prop('cols', c);

    } else {

        inp.attr('size', text.length + 1);

    }

    // set the width value to the max parent width value
    if ($(inp).parent().outerWidth() < $(inp).width()) {
        $(inp).width($(inp).parent().outerWidth());
    }

    // Focus on last empty field
    if (!text || !text.length)
        inp.focus();
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

String.prototype.splitAndTitleize = function(sep) {

    var res = new Array();
    var parts = this.split(sep);
    parts.forEach(function(part) {
        res.push(part.capitalize());
    })
    return res.join(" ");
};

String.prototype.toUnderscore = function() {
    return this.replace(/([A-Z])/g, function(m, a, offset) {
        return (offset > 0 ? '_' : '') + m.toLowerCase();
    });
};

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
    return '<textarea class="structr-input-text" data-structr-id="' + id + '" data-structr-attr="' + key + '">' + (val === 'null' ? '' : val) + '\n</textarea>';
}

function inputField(id, type, key, val) {
    return '<input class="structr-input-text" type="text" placeholder="' + key.capitalize() + '" data-structr-attr="' + key + '" value="' + (val === 'null' ? '' : val) + '" size="' + (val ? val.length : 5) + '">';
}

function field(id, type, key, val) {
    return '<span type="text" data-structr-id="' + id + '" data-structr-type="' + type + '" data-structr-attr="' + key + '">' + val + '</span>';
}

function checkbox(id, type, key, val) {
    return '<input type="checkbox" data-structr-id="' + id + '" data-structr-type="' + type + '" data-structr-attr="' + key + '" ' + (val ? 'checked="checked"' : '') + '">';
}

function select(id, key, val, options) {
    var s = '<select data-structr-id="' + id + '" data-structr-attr="' + key + '">';
    $.each(options, function(i, o) {
        s += '<option ' + (o === val ? 'selected' : '') + '>' + o + '</option>';
    });
    return s + '</select>';
}