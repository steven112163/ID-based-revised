<?php
$link = mysqli_connect('192.168.44.128','user','!1Qazwsxedc','portal');

if (!$link) {
  die('Could not connect:' .mysqli_error());
}
//echo 'Connected successfully';
mysqli_query($link, "SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');	
?>
