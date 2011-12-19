var callbacks = new Array();

function connect() {

    try {

        ws = new WebSocket("ws://localhost:8080/structr-web-test/ws/", "structr");

        log('State: ' + ws.readyState);

        ws.onopen = function() {
            log('Open: ' + ws.readyState);
        }

        ws.onmessage = function(message) {
            log('Message: ' + message.data);
            var msg = eval("(" + message.data + ")");
            if(msg.token) {
                document.getElementById("token").value = msg.token;
            }

            if(msg.command == "LIST") {
                var callbackId = msg.callback;
                var callback = callbacks[callbackId];
                var parent = document.getElementById(callbackId);

                if(callback && parent) {
                    var i = 0;
                    for(i=0;i<msg.result.length; i++) {
                        callback(parent, msg.result[i]);
                    }
                } else {
                    alert("Callback or parent not found.");
                }
            }

        }

        ws.onclose = function() {
            log('Close: ' + ws.readyState);
        }

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function send(text) {

    if (!text) {
        log('No text to send!');
        return;
    }

    try {

        ws.send(text);
        log('Sent: ' + text);

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function log(msg) {
    var logElement = document.getElementById("log");
    if(logElement) {
        logElement.innerHTML = msg + "<br />" + logElement.innerHTML;
    }
}

function login() {
    send('{ "command":"LOGIN", "data" : { "username" : "chrisi", "password" : "wurst432" } }');
}

function sendData(cmd, id, callback, page, pageSize, data) {

    var token = document.getElementById("token").value;

    var message = '{"command":"' + cmd + '","id":"' + id + '","token":"' + token + '"';

    if(id)                {
        message += ', "id":' + id;
    }
    if(callback)            {
        message += ', "callback":' + callback;
    }
    if(page && page > 0)        {
        message += ', "page":' + page;
    }
    if(pageSize && pageSize > 0)    {
        message += ', "pageSize":' + pageSize;
    }
    if(data)             {
        message += ', "data":' + data;
    }

    message += '}';

    send(message);
}

function start() {
    connect();
    window.setTimeout("login()", 100);
}

function loadList(id, type, callback) {
    callbacks[id] = callback;
    sendData("LIST", "", id, 0, 0, '{"type":"' + type + '"}');
}
