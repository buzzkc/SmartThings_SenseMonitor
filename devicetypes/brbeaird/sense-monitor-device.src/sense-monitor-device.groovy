/**
 *	Sense Device
 *
 *	Author: Brian Beaird
 *  Last Updated: 2018-08-11
 *
 ***************************
 *
 *  Copyright 2018 Brian Beaird
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import java.text.SimpleDateFormat
String devVersion() { return "0.2.0"}

metadata {
    definition (name: "Sense Monitor Device", namespace: "brbeaird", author: "Brian Beaird") {
        capability "Power Meter"
        capability "Switch"
        capability "Actuator"
        capability "Sensor"
        
        attribute "lastUpdated", "string"
        attribute "deviceLocation", "string"
        attribute "dtCreated", "string"
        attribute "deviceMake", "string"
        attribute "deviceModel", "string"
        attribute "detectionMature", "string"
        attribute "deviceRevoked", "string"
    }

    preferences {
       
    }

    tiles (scale: 2) {
        multiAttributeTile(name:"genericMulti", type:"generic", width:6, height:4) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${currentValue}', icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
                attributeState "off", label: '${currentValue}', icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            }
            tileAttribute("device.power", key: "SECONDARY_CONTROL") {
                attributeState "power", label: '${currentValue}W', unit: "W",
                        foregroundColor: "#000000",
                        backgroundColors:[
                            [value: 1, color: "#00cc00"], //Light Green
                            [value: 2000, color: "#79b821"], //Darker Green
                            [value: 3000, color: "#ffa81e"], //Orange
                            [value: 4000, color: "#FFF600"], //Yellow
                            [value: 5000, color: "#fb1b42"] //Bright Red
                        ]
            }
        }
        valueTile("power", "device.power", decoration: "flat", width: 1, height: 1) {
            state "power", label:'${currentValue} W', unit: "W"
        }
        valueTile("blank1", "device.blank", height: 1, width: 1, inactiveLabel: false, decoration: "flat") {
            state("default", label:'')
        }
        valueTile("blank2", "device.blank", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("default", label:'')
        }
        valueTile("lastUpdated", "device.lastUpdated", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
            state("lastUpdated", label:'Last Updated:\n${currentValue}')
        }
        valueTile("deviceLocation", "device.deviceLocation", height: 1, width: 3, inactiveLabel: false, decoration: "flat") {
            state("deviceLocation", label:'Device Location:\n${currentValue}')
        }
        valueTile("dtCreated", "device.dtCreated", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("dtCreated", label:'Device Created:\n${currentValue}')
        }
        valueTile("deviceMake", "device.deviceMake", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("deviceMake", label:'Device Make:\n${currentValue}')
        }
        valueTile("deviceModel", "device.deviceModel", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("deviceModel", label:'Device Model:\n${currentValue}')
        }
        valueTile("detectionMature", "device.detectionMature", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("detectionMature", label:'Mature Detection:\n${currentValue}')
        }
        valueTile("deviceRevoked", "device.deviceRevoked", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
            state("deviceRevoked", label:'Device Revoked:\n${currentValue}')
        }
        main(["power"])
        details(["genericMulti", "lastUpdated", "deviceLocation", "dtCreated", "deviceMake", "deviceModel", "detectionMature", "deviceRevoked"])
    }
}

def installed() {
	log.trace "${device?.displayName} Executing Installed..."
    def dt = formatDt(new Date(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    state?.dateCreated = dt
	initialize()
}

def updated() {
	log.trace "${device?.displayName} Executing Updated..."
	initialize()
}

def initialize() {
	log.trace "${device?.displayName} Executing initialize"
 	sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
}

def updateDeviceLastRefresh(lastRefresh){
    log.debug "Last refresh: " + lastRefresh
    def refreshDate = new Date()
    def hour = refreshDate.format("h", location.timeZone)
    def minute =refreshDate.format("m", location.timeZone)
    def ampm = refreshDate.format("a", location.timeZone)
    //def finalString = refreshDate.getDateString() + ' ' + hour + ':' + minute + ampm

    def finalString = new Date().format('MM/dd/yyyy hh:mm a',location.timeZone)
    if(isStateChange(device, "lastRefresh", finalString as String)) {
        sendEvent(name: "lastRefresh", value: finalString, display: false, displayed: false)
    }
}

def getShortDevName(){
    return device?.displayName?.replace("Sense-", "")
}

def updateDeviceStatus(Map senseDevice){
    String devName = getShortDevName()

    senseDevice?.each { k,v ->
        log.debug "$k: $v"
    }
    Float currentPower = senseDevice?.usage?.isNumber() ? senseDevice?.usage as Float : 0.0
    Float oldPower = device.currentState("power")?.floatValue ?: -1

    // log.debug "usage: ${senseDevice?.usage} | currentPower: $currentPower | oldPower: ${oldPower}"

    if(senseDevice?.containsKey("dateCreated")) {
        def dtCreated = senseDevice?.dateCreated ? formatDt(parseDt(senseDevice?.dateCreated, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")) : (state?.dateCreated ?: "")
        if(isStateChange(device, "dtCreated", dtCreated as String)) {
            sendEvent(name: "dtCreated", value: dtCreated as String, display: true, displayed: true)
        }
    }

    String loc = senseDevice?.location ?: "Not Set"
    if(isStateChange(device, "deviceLocation", loc?.toString())) {
        sendEvent(name: "deviceLocation", value: loc?.toString(), display: true, displayed: true)
    }

    String make = senseDevice?.make ?: "Not Set"
    if(isStateChange(device, "deviceMake", make?.toString())) {
        sendEvent(name: "deviceMake", value: make?.toString(), display: true, displayed: true)
    }

    String model = senseDevice?.model ?: "Not Set"
    if(isStateChange(device, "deviceModel", model?.toString())) {
        sendEvent(name: "deviceModel", value: model?.toString(), display: true, displayed: true)
    }
    
    if (oldPower != currentPower) {
        def usageChange = (currentPower - oldPower).abs()
        if (isStateChange(device, "power", currentPower?.toString())) {
            log.debug "Updating usage from $oldPower to $currentPower"
            sendEvent(name: "power", value: currentPower, units: "W", display: true, displayed: true, isStateChange: true)
        }
    }
    setOnlineStatus(true)
    updateDeviceLastRefresh()
}

public setOnlineStatus(Boolean isOnline) {
    if(isStateChange(device, "DeviceWatch-DeviceStatus", (isOnline ? "online" : "offline"))) {
        sendEvent(name: "DeviceWatch-DeviceStatus", value: (isOnline ? "online" : "offline"), displayed: true, isStateChange: true)
    }
}

def formatDt(dt, String tzFmt=("MM/d/yyyy hh:mm a")) {
	def tf = new SimpleDateFormat(tzFmt); tf.setTimeZone(location.timeZone);
    return tf.format(dt)
}

def parseDt(dt, dtFmt) {
    return Date.parse(dtFmt, dt)
}

Boolean ok2Notify() {
    return (parent?.getOk2Notify())
}

def updateDeviceLastRefresh(){
    def finalString = new Date().format('MM/d/yyyy hh:mm a',location.timeZone)
    sendEvent(name: "lastUpdated", value: finalString, display: false , displayed: false)
}

def log(msg){
    log.debug msg
}

def showVersion(){
    return "0.0.1"
}
