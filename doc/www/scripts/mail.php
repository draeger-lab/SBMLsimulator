<?php
  $admin= 'andreas.draeger@uni-tuebingen.de';
  $subject= 'SBMLsimulator-Registrierung';
  $email=$_POST['email'];
  $name=$_POST['name'];
  $organization=$_POST['org'];
  $message= "Nachricht von\t".$name."\n";
  $message .= "Organisation:\t".$organization."\n";
  $message .= "E-Mail-Adresse:\t".$email."\n";
  if (0 < strlen($email) && (0 < strlen($name)) && (0 < strlen($organization))) {
    mail($admin, $subject, $message, "From: $email");
    header('Location: http://'.getenv('HTTP_HOST').'/software/SBMLsimulator/downloads/index.html#download');
  } else {
    header('Location: http://'.getenv('HTTP_HOST').'/software/SBMLsimulator/downloads/index.html');
  }
?>

