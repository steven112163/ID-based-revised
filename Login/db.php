<?php
mysql_connect('192.168.44.128','root','root') or die("Couldn't connect to SQL Server");
mysql_select_db('portal');
mysql_query("SET NAMES 'utf8'");
date_default_timezone_set('Asia/Taipei');	
?>