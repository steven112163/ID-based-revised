<?php
session_start();
$_SESSION['link'] = mysqli_connect('127.0.0.1', 'root', '_En6794235sT_', 'portal');

if(mysqli_connect_errno()) {
  echo "Failed to connect to MySQL: ".mysquli_error();
}

mysqli_query($_SESSION['link'], "SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');
?>
