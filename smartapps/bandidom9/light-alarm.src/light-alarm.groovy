/**
 *  Copyright 2018 Jose Perez
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
 *
 *  Author: Jose Perez (bandidom9)
 *  Date: 2018-09-09
 */
 
definition(
    name: "Light Alarm",
    namespace: "bandidom9",
    author: "Jose Perez (bandidom9)",
    description: "Light Alarm sets a color and flashes lights in response to motion or open/close event",
    category: "Convenience",
    iconUrl: "https://png.pngtree.com/element_origin_min_pic/17/03/02/3c5d50cadd0910cbe5c014c3b7cddb98.jpg",
    iconX2Url: "https://png.pngtree.com/element_origin_min_pic/17/03/02/3c5d50cadd0910cbe5c014c3b7cddb98.jpg"
)

preferences {
	section("Motion/Contact Sensors"){
		input "motion", "capability.motionSensor", title: "Motion Sensor?", required: false
		input "contact", "capability.contactSensor", title: "Contact Sensor?", required: false
	}
    section("Motion/Contact Sensors 2"){
		input "motion2", "capability.motionSensor", title: "Motion Sensor?", required: false
	}
	section("Color Capable Lights"){
		input "bulbs", "capability.colorControl", title: "These lights", multiple: true
		input "numFlashes", "number", title: "This number of times (default 3)", required: false
	}
	section("Time settings in milliseconds (optional)..."){
		input "onFor", "number", title: "On for (default 1000)", required: false
		input "offFor", "number", title: "Off for (default 1000)", required: false
        input "backToNormal", "number", title: "Back to Normal in (defailt 2000)", required: false
        
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {

	if (contact) {
		subscribe(contact, "contact.open", contactOpenHandler)
	}

	if (motion) {
		subscribe(motion, "motion.active", motionActiveHandler)
	}
}


def motionActiveHandler(evt) {
    def motionState = motion2.currentMotion
    log.debug "motion $evt.value"
    log.debug "$motionState"
    if (motionState == "inactive") {
        log.debug "Doing flasher"
    	Flasher()
     }
     else {
        log.debug "Not doing flasher"
    }
}

def contactOpenHandler(evt) {
	log.debug "contact $evt.value"
    Flasher()
}


private Flasher() {
	def doFlash = true
	def onFor = onFor ?: 1000
	def offFor = offFor ?: 1000
	def numFlashes = numFlashes ?: 3
	def backToNormal = backToNormal ?: 2000

	log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = ((numFlashes + 1) * (onFor + offFor)) + backToNormal  
		doFlash = elapsed > sequenceTime
  	   log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
    }
 


	if (doFlash) {
        state.lastActivated = now()
    	def state = bulbs.collect{it.currentSwitch == "on"}
        def hue = bulbs.collect{it.currentHue}
        def saturation = bulbs.collect{it.currentSaturation}
        def color = bulbs.collect{it.currentColor}
        def level = bulbs.collect{it.currentLevel}
        def qty = state.size 
           
 		log.debug "Current color to: $color "
		log.debug "Current color to: $state "

		bulbs.setColor(hex: "#FF0F00" )
		bulbs.setLevel(100)
		bulbs.setSaturation(100)
		bulbs.setHue(1)
 
 		numFlashes.times{
            bulbs.on()
            pause(onFor)  
            bulbs.off()
            pause(offFor)
		}
	 	pause(backToNormal)
    
  
        bulbs.eachWithIndex {s, i ->
        def  previousState = state[i]
        if (previousState) {
            def  previousColor = color[i]
            def  previousLevel = level[i] 
            def  previousSaturation = saturation[i] 
            def  previousHue = hue[i]  
            
            s.setColor(hex: "$previousColor")
            s.setLevel($previousLevel)
            s.setSaturation($previousSaturation)
            s.setHue($previousHue)
            s.on()   
        }
        else {
            s.off()
        }          
 		}
 	}
}


