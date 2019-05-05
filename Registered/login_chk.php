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

$sql = 'SELECT MAC FROM IP_MAC WHERE IP = "'.$ip.'";';
$result = mysqli_query($_SESSION['link'], $sql);
$row = mysqli_fetch_assoc($result);

$_SESSION['MAC'] = $row['MAC'];

$account = isset($_POST["user_name"]) ? $_POST["user_name"] : $_GET["user_name"];
$password = isset($_POST["user_password"]) ? $_POST["user_password"] : $_GET["user_password"];

$sql = 'SELECT User_ID, Group_ID, Password FROM User WHERE Account = "'.$account.'";';
$result = mysqli_query($_SESSION['link'], $sql);
$row = mysqli_fetch_assoc($result);
$record_count = mysqli_num_rows($result);

//no this user
if($record_count < 1) {
    echo 'fail';  
}
else {
    $correct_passwd = $row['Password'];

    //password isn't correct
    if($password != $correct_passwd){
        echo 'fail';
    }
    else{
        $_SESSION['User_ID'] = $row['User_ID'];
        echo 'success';
    }
}
?>
