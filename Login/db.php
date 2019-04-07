<?php
$link = mysqli_connect('192.168.44.128','root','root','portal');

if (!$link) {
  die('Could not connect:' .mysquli_error());
}
// echo 'Connected successfully';
mysql_query($link, "SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');	
?>
