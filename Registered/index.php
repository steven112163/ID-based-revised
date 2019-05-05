<?php
ini_set("display_errors", "On");
error_reporting(E_ALL);
include('db.php');
?>
<!DOCTYPE HTML>
<html>
<head>
    <meta charset="UTF-8">
    <title>Authentication</title>
    <script src="./jquery-latest.js"></script>
    <link rel="stylesheet" type=text/css href="style.css">
</head>
<body>

<script language="javascript">
$(function(){
    $('#submit').mouseover(function(){$(this).css({'background':'#00AAAA', 'font-size':'22px'})});
    $('#submit').mouseout(function(){$(this).css({'background':'#00AAAA', 'font-size':'20px'})});
});
function userlogin(){
    var user_name = $('#user_name').val();
    var user_password = $('#user_password').val();
    $.ajax({
        url: "login_chk.php",
        type: "POST",
        data: "user_name="+user_name+"&user_password="+user_password,
        beforeSend: function(){
            $('#error_msg').html('');
            $('#error_msg').hide();
            $('#loading_div').show(); 
        },
        success: function(msg){
            if(msg == 'success'){
                $('#error_msg').html('');
                $('#error_msg').hide();
                $('#login_success').text('Login Successfully!!!');
                $('#login_success').show();
                setTimeout(function() {
                    window.open("http://192.168.44.101/show.php", "_self")
                }, 1000);
				
            }
            else if(msg == 'fail')
            {
                $('#error_msg').show();
                $('#error_msg').html('沒有此用戶或密碼不正確，請重新登入！');
            }
            else if(msg == 'block')
			{
                $('#error_msg').show();
                $('#error_msg').html('此裝置已被封鎖，故您暫時無法登入！');
            }
        },
        error: function(xhr, ajaxOptions, thrownError){
            alert(xhr);
            alert(ajaxOptions);
            alert(thrownError);
        },
        complete: function(msg){
            $('#loading_div').hide();
        }
    });	
};
</script>

<div id="login_block">
	<h1 style="text-align:center;">Registered Users</h1>
    <h1>Login</h1>

    <form id="user_login" method="POST">
        <table id="login_table">
            <tr>
                <td><label>Account:</label></td>
                <td><input type='text' id="user_name" /></td>
            </tr>
            <tr>
                <td><label>Password:</label></td>
                <td><input type='password' id="user_password" /></td>
            </tr>
            <tr height="80">
                <td><input type='button' id='submit' value='Login' onclick='userlogin();'/></td>
            </tr>
        </table>
    </form>

    <div id="error_msg" style="display:none;"></div>

    <div id="loading_div" style="display:none">
        <img src="loading.gif"><br/>
    </div>

    <div id="login_success" style="display:none;"></div>
</div>
</body>
</html>
