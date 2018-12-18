/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Lock It When I Leave
 *
 *  Author: SmartThings
 *  Date: 2013-02-11
 */

definition(
    name: "Did I locked the doors?",
    namespace: "bandidom9",
    author: "Jose Perez (bandidom9)",
    description: "Notify if a door was left open during departure",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("When any of this precense departs") {
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
    	section("Notify me of Status of these Presence") {
		input "presence2", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Check these doors"){
		input "contact", "capability.contactSensor", title: "Contact Sensor?", multiple: true
	}
        }
    section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
            paragraph "If outside the US please make sure to enter the proper country code"
            input "phone2", "phone", title: "Enter a second phone number to get SMS", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
       } 
     section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	 }
	

def installed()
{
	subscribe(presence1, "presence", presence)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presence)
}

def presence(evt)
{
def contactState = contact.currentContact

	if (evt.value == "not present") {	                
            def state = "close"
            contact.eachWithIndex {s, i ->
            
            if (contactState[i] == "open"){
            state = "open"
            }      
           }
           
           if (state == "open"){
            sendMessage(evt)
            }                    
		}		
}

private sendMessage(evt) {
	String msg = messageText
	Map options = [:]
    Map options2 = [:]

	if (!messageText) {
		//msg = "You left a door open"
        
      
        def contactState = contact.currentContact
		def contactLabel = contact.label               
        def openContacts = "None"
        	contact.eachWithIndex {s, i ->
                if (contactState[i] == "open"){
                    if (openContacts == "None"){
                     openContacts = contactLabel[i]
                    }else{
                     openContacts += " and " + contactLabel[i]
                    } 
                 }
        }                     
	
      
        //def presenceEventEntity = presence1.label 
        def presenceState = presence2.currentPresence
		def presenceLabel = presence2.displayName               
        def presentEntities = "No one"
        	presence2.eachWithIndex {s, i ->
                if (presenceState[i] == "present"){
                    if (presentEntities == "No one") {
                     presentEntities = presenceLabel[i]
                    }else{
                     presentEntities += " and " + presenceLabel[i]
                    }
                }      
        }                     
        
        def defaultMessage = "$evt.displayName just left leaving the $openContacts open. $presentEntities is in the house."
        msg = defaultMessage
       // log.debug presenceEventEntity
       // log.debug presentEntities 
	}


	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
           options2.method = 'phone'
			options.phone = phone
             if (phone2) {
              options2.phone = phone2
             }
			if (pushAndPhone != 'No') {
				log.debug "Sending push and SMS"
               // log.debug contact.label
               // log.debug contact.currentContact
               // log.debug presence2.label
               // log.debug presence2.currentPresence
               //log.debug msg
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
                if (phone2) {
                 options2.phone = phone2
                }
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
        if (phone2) {
          sendNotification(msg, options2)   
         }
        
	}

}

