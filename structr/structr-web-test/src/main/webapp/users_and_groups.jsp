<!DOCTYPE html>
<html>
    <head>
        <%@include file="include/header.jsp" %>
        <script type="text/javascript">
            <!--
            /**************** config parameter **********************************/
            var rootUrl =     '/structr-web-test/json/api/';
            var headers = { 'X-SplinkUser' : 0 };
            /********************************************************************/
            var main;
            $(document).ready(function() {
                main = $('#main');
                //refreshEntities('group');
                //refreshEntities('user');
                $('#import_json').on('click', function() {
                    var jsonArray = $.parseJSON($('#json_input').val());
                    $(jsonArray).each(function(i, json) {
                        //console.log(json);
                        createEntity(json);
                    });
                    //var json = $.parseJSON('{ "test" : "abc" }');
          
                });
                $('#loginButton').on('click', function() {
                    var username = $('#usernameField').val();
                    var password = $('#passwordField').val();
                    doLogin(username, password);
                });
                $('#logoutLink').on('click', function() {
                    doLogout();
                });
                $('#usernameField').keypress(function(e) {
                    if(e.which == 13) {
                        jQuery(this).blur();
                        jQuery('#loginButton').focus().click();
                    }
                });
                $('#passwordField').keypress(function(e) {
                    if(e.which == 13) {
                        jQuery(this).blur();
                        jQuery('#loginButton').focus().click();
                    }
                });
            });
            //-->
        </script>
        <script src="js/users_and_groups.js" type="text/javascript"></script>
    </head>
    <body>
        <%@include file="include/menu.jsp" %>

        <div id="main">
            <!--      <textarea id="json_input" rows="10" cols="80">
                  </textarea>
                  <input type="button" id="import_json" value="Import JSON">-->
            <div style="clear: both"></div>
        </div>
        <%@include file="include/footer.jsp" %>

        <div id="login">
            <table>
                <tr><td><label for="username">Username:</label></td><td><input id="usernameField" type="text" name="username"></input></td></tr>
                <tr><td><label for="password">Password:</label></td><td><input id="passwordField" type="password" name="password"></input></td></tr>
                <tr><td colspan="2" class="btn"><span id="errorText"></span></td></tr>
                <tr><td colspan="2" class="btn"><button id="loginButton" name="login"><img src="icon/key.png"> Login</button></td></tr>
            </table>
        </div>

        <div id="confirmation">
            <div id="confirmationText"></div>
            <button id="yesButton"><img src="icon/tick.png">Yes</button>
            <button id="noButton"><img src="icon/cross.png">No</button>
        </div>

    </body>
</html>