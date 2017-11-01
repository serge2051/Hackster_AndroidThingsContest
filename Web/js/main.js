
var flag_sprinklers_visible;

function publishState(messageIn) {
	$.ajax({
		url:'./php/emergency_action.php',
		data: { action:'publish_message', projectId: "android-things-contest", topicName : "emergency", message : messageIn},
		complete: function (response) {
			console.log(response.responseText);
			checkInfrastructureState();
		},
		error: function () {
			console.log("There was some error(publish)...");
		}
	});
	return false;
}

function checkInfrastructureState() {
	
	requestedMessages = new Array();
	$.ajax({
		url:'./php/emergency_action.php',
		data: { action:'pull_data', projectId: "android-things-contest", subscriptionName : "mchs_sub"},
		dataType:"json",
		complete: function (response) {
			console.log("pull OK!");
			console.log(response);
			
			requestedMessages = JSON.parse(response.responseText);
			for (var i = 0; i < requestedMessages.length; i++) {
				console.log(requestedMessages[i].device + " | " + requestedMessages[i].value);
				if (requestedMessages[i].type == "get")
				{
					var tmpEl = document.getElementById(requestedMessages[i].device);
					var newVal = parseInt(requestedMessages[i].value, 10);					
					if ((tmpEl!=null) && (newVal!=null))
					{
						if (newVal > 25)
						{
							tmpEl.classList.remove("bg-green");
							tmpEl.classList.add("bg-red");
						}
						else
						{
							tmpEl.classList.remove("bg-red");
							tmpEl.classList.add("bg-green");
						}
					}
				}
			}
		},
		error: function () {
			console.log("There was some error(check)...");
		}
	});
	
	return false;
}

function sprinklers_scheduling()
{
	if (flag_sprinklers_visible)
	{
		var tmpEl = document.getElementById('Water_x5F_3');
		tmpEl.style.display = 'none';
		
		tmpEl = document.getElementById('Water_x5F_5');
		tmpEl.style.display = 'none';
		
		tmpEl = document.getElementById('Water_x5F_4');
		tmpEl.style.display = 'none';
		
		tmpEl = document.getElementById('Water_x5F_2');
		tmpEl.style.display = 'none';		
	}
	else
	{
		var tmpEl = document.getElementById('Water_x5F_3');
		tmpEl.style.display = 'block';
		
		tmpEl = document.getElementById('Water_x5F_5');
		tmpEl.style.display = 'block';
		
		tmpEl = document.getElementById('Water_x5F_4');
		tmpEl.style.display = 'block';
		
		tmpEl = document.getElementById('Water_x5F_2');
		tmpEl.style.display = 'block';
	}
	flag_sprinklers_visible = !flag_sprinklers_visible;
	deviceGetState('sensor_zenit_121');
	
	setTimeout(sprinklers_scheduling, 5000);
}

window.onload = function()
{	
	setTimeout(sprinklers_scheduling, 700);
	flag_local_server_connected = false;
	flag_sprinklers_visible = false;
}

function deviceGetState(id)
{	
	publishState("mchs|get|" + id + "|ok");
}

function deviceChangeState(id)
{
	var tmpEl = document.getElementById(id);
	var value = tmpEl.checked;
	
	if (value)
		publishState("mchs|set|" + id + "|on");
	else
		publishState("mchs|set|" + id + "|off");
		
	if (id=="toggle-zenit-1")
	{
		if (value)
		{	
			var newEl = '<li>';
			newEl+='<i class="fa fa-bell-o bg-blue"></i>';
			newEl+='<div class="timeline-item">';
			newEl+='<span class="time"><i class="fa fa-clock-o"></i> 12:05</span>';
			newEl+='<h3 class="timeline-header"><a href="#">Sprinkler</a> on</h3>';
			newEl+='<div class="timeline-body">';
			newEl+=' Sprinkler №1 in sector 04 switched on manually';
			newEl+='</div>';
			newEl+='</div>';
			newEl+='</li>';
			$('#timeline_header').after(newEl);
		}
		else
		{
			var newEl = '<li>';
			newEl+='<i class="fa fa-bell-slash-o bg-blue"></i>';
			newEl+='<div class="timeline-item">';
			newEl+='<span class="time"><i class="fa fa-clock-o"></i> 12:05</span>';
			newEl+='<h3 class="timeline-header"><a href="#">Sprinkler</a> off</h3>';
			newEl+='<div class="timeline-body">';
			newEl+=' Sprinkler №1 in sector 04 switched off manually';
			newEl+='</div>';
			newEl+='</div>';
			newEl+='</li>';
			$('#timeline_header').after(newEl);
		}	
	}
	if (id=="toggle-zenit-6")
	{
		if (value)
		{	
			var newEl = '<li>';
			newEl+='<i class="fa fa-bell-o bg-blue"></i>';
			newEl+='<div class="timeline-item">';
			newEl+='<span class="time"><i class="fa fa-clock-o"></i> 12:05</span>';
			newEl+='<h3 class="timeline-header"><a href="#">Sprinkler</a> on</h3>';
			newEl+='<div class="timeline-body">';
			newEl+=' Sprinkler №6 in sector 04 switched on manually';
			newEl+='</div>';
			newEl+='</div>';
			newEl+='</li>';
			$('#timeline_header').after(newEl);
		}
		else
		{
			var newEl = '<li>';
			newEl+='<i class="fa fa-bell-slash-o bg-blue"></i>';
			newEl+='<div class="timeline-item">';
			newEl+='<span class="time"><i class="fa fa-clock-o"></i> 12:05</span>';
			newEl+='<h3 class="timeline-header"><a href="#">Sprinkler</a> off</h3>';
			newEl+='<div class="timeline-body">';
			newEl+=' Sprinkler №6 in sector 04 switched off manually';
			newEl+='</div>';
			newEl+='</div>';
			newEl+='</li>';
			$('#timeline_header').after(newEl);
		}	
	}
}