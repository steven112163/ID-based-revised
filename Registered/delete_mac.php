<?php
session_start();
include('db.php');
sleep(1);

if(isset($_POST['mac']) && !empty($_POST['mac'])) {
	$mac = $_POST['mac'];
	
	$sql = 'DELETE FROM Registered_MAC WHERE MAC = "'.$mac.'";';
	$result = mysqli_query($_SESSION['link'], $sql);
	echo 'success';
} else {
	echo 'fail';
}
?>
