/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicitly call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');
    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);
    }, 
	saveMessagesX: function() {
		var el = document.getElementById('status');
		el.innerHTML = "<p>Ok, starting!</p>";
		alert("Done!");
	},
	saveMessages: function() {
		var el = document.getElementById('status');
		el.innerHTML = "<p>Working...</p>";
		el.className = "blink";
		alert("Ok, starting!  This may take a while...");
		
		var success = function(message) { 
			//alert("Done! Plug in your USB cable and copy the SavedTexts directory to your computer."); 
			el.innerHTML = "<p><b>Done!</b> Plug in your USB cable and copy the SavedTexts directory to your computer.</p>";
			el.className = "status";
			};
			
		var error = function(message) { alert("Failed: " + message); };
		
		 cordova.exec(
            success, // success callback function
            error, // error callback function
            'MessageList', // mapped to our native Java class called 
            'save', // with this action name
            ['From Javascript to Java and back']
        ); 
	}
};

app.initialize();

