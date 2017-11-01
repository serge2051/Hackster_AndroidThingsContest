<?php
//Using XAMPP as local server for PHP
require 'C:/xampp/vendor/autoload.php';
use Google\Cloud\PubSub\PubSubClient;

ini_set('max_execution_time', 0);
if ($_REQUEST['action'] == 'pull_data')
    return pull_data($_REQUEST['projectId'], $_REQUEST['subscriptionName']);
else if ($_REQUEST['action'] == 'publish_message')
	return publish_message($_REQUEST['projectId'], $_REQUEST['topicName'], $_REQUEST['message']);
else
    echo json_encode( array( 'status' => 'error') );
	
function pull_data($projectId, $subscriptionName)
{
	$retVals = array();
    $pubsub = new PubSubClient([
        'projectId' => $projectId,
		'keyFilePath' => './keypub.json'
    ]);
    $subscription = $pubsub->subscription($subscriptionName);
    foreach ($subscription->pull() as $message) {
        $subscription->acknowledge($message);
		$json_decoded = json_decode($message->data());
		if (!($json_decoded == NULL))
		{
			if ($json_decoded->{'src'} != 'mchs')
				$retVals[] = $json_decoded;
		}
    }
	
	echo json_encode($retVals);
	
	return $retVals;
}


function publish_message($projectId, $topicName, $message)
{
    $pubsub = new PubSubClient([
        'projectId' => $projectId,
		'keyFilePath' => './keypub.json'
    ]);
    $topic = $pubsub->topic($topicName);
	
	$pieces = explode("|", $message);
	$json_out = array('src' => $pieces[0], 'type' => $pieces[1], 'device' => $pieces[2], 'value' => $pieces[3]);
    $topic->publish(['data' => json_encode($json_out)]);
    print('Message published' . PHP_EOL);
}

?>