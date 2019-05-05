<?php
$link = mysqli_connect('127.0.0.1','root','root','portal');

if (!$link) {
  die('Could not connect:' .mysqli_error());
}
// echo 'Connected successfully';
mysql_query($link, "SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');	
?>
