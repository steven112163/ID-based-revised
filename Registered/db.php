<?php
session_start();
$_SESSION['link'] = mysqli_connect('192.168.44.128', 'user', '!1Qazwsxedc', 'portal');

if(mysqli_connect_errno()) {
  echo "Failed to connect to MySQL: ".mysqli_error();
}

mysqli_query($_SESSION['link'], "SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');
?>
