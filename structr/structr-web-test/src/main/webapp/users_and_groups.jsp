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
            });
            //-->
        </script>
        <style>
            .props tr td.value input {
                width: 440px;
            }
        </style>
        <script src="js/users_and_groups.js" type="text/javascript"></script>
    </head>
    <body>
        <%@include file="include/menu.jsp" %>

        <div id="main">
            <!--      <textarea id="json_input" rows="10" cols="80">
                  </textarea>
                  <input type="button" id="import_json" value="Import JSON">-->
            <div id="groups"></div><div id="users"></div>
        </div>
        <%@include file="include/footer.jsp" %>
    </body>
</html>