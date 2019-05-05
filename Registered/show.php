<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Users</title>
<script src="./jquery-latest.js"></script>
<style>
table {
	fond-family: arial, sans-serif;
	border-collapse: collapse;
	width: 100%;
}

td, th {
	border: 1px solid #dddddd;
	text-align: left;
	padding: 8px;
}

tr:nth-child(even) {
	background-color: #dddddd;
}

td.red {color: #FF0000;}
td.blue {color: #0000FF;}
</style>
</head>

<script language="javascript">
function clickDelete(passedMAC) {
	$.ajax({
			url: "delete_mac.php",
			type: "POST",
			data: "mac="+passedMAC,
			beforeSend: function() {
				console.log('ready');
			},
			success: function(msg) {
				if(msg == 'success') {
					console.log('success');
				} else {
					console.log('something wrong');
				}
			},
			error: function(xhr, ajaxOptions, thrownError) {
				alert(xhr);
				alert(ajaxOptions);
				alert(thrownError);
			}
	}).done(function(){
			window.location.reload();
	});
}
</script>
</html>

<?php
session_start();
include("db.php");

$sql = 'SELECT * FROM Registered_MAC WHERE User_ID = "'.$_SESSION['User_ID'].'";';
$result = mysqli_query($_SESSION['link'], $sql);

echo "<div>";
echo "<h1>Current Users</h1>";

echo "<table border = '1'><tr><th>MAC</th><th>User_ID</th><th>Group_ID</th><th>Host</th><th>Delete</th></tr>";

while($row = mysqli_fetch_row($result)) {
	echo "<tr>";
	echo "<td>".$row[0]."</td>";
	echo "<td>".$row[1]."</td>";
	echo "<td>".$row[2]."</td>";
	if($_SESSION['MAC'] == $row[0]) {
		echo "<td class='red'>Current host</td>";
		echo "<td></td>";
	} else {
		echo "<td class='blue'>Other hosts</td>";
		echo "<td><button type='button' onclick=\"clickDelete('$row[0]');\">Delete</button></td>";
	}
	echo "</tr>";
}

echo "</table>";
echo "</div>";
?>
