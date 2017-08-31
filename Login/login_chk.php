<?php
session_start();
/* -------------------------- */
/* Check account & password   */
/* -------------------------- */
include('db.php');
sleep(1);

if (!empty($_SERVER['HTTP_CLIENT_IP']))
    $ip = $_SERVER['HTTP_CLIENT_IP'];
else if (!empty($_SERVER['HTTP_X_FORWARDED_FOR']))
    $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
else
    $ip = $_SERVER['REMOTE_ADDR'];

$sql = 'SELECT MAC FROM IP_MAC WHERE IP = "'.$ip.'"';
$result = mysql_query($sql);
$row = mysql_fetch_assoc($result);

$mac = $row['MAC'];

$account = isset($_POST["user_name"]) ? $_POST["user_name"] : $_GET["user_name"];
$password = isset($_POST["user_password"]) ? $_POST["user_password"] : $_GET["user_password"];

$sql = 'SELECT User_ID, Group_ID, Password FROM User WHERE Account = "'.$account.'"';
$result = mysql_query($sql);
$row = mysql_fetch_assoc($result);
$record_count = mysql_num_rows($result);

//no this user
if($record_count<1){
    echo 'fail';  
}
else{
    $user_id = $row['User_ID']; 
    $group_id = $row['Group_ID'];
    $correct_passwd = $row['Password'];

    //password isn't correct
    if($password != $correct_passwd){
        echo 'fail';
    }
    else{
        $sql = 'UPDATE Registered_MAC SET Enable=true, User_ID = "'.$user_id.'", Group_ID = "'.$group_id.'" WHERE MAC= "'.$mac.'"';
        $result = mysql_query($sql);
            
        echo 'success';
    }
}
?>
