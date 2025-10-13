package com.danielebonaldo.neuralyzer.client

import java.util.UUID


object NeuralyzerLedUUID {

    interface IBleElement {
        val uuid: UUID
        val description: String
    }

    object NeuralyzerLightService : IBleElement {
        override val uuid: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
        override val description: String = "Neuralyzer BLE Service"

        object LEDColor : IBleElement {
            override val uuid: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
            override val description: String = "Led Color"
        }

        object LEDIntensity : IBleElement {
            override val uuid: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
            override val description: String = "Led Intensity"
        }

        object LEDActiveStatus : IBleElement {
            override val uuid: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef4")
            override val description: String = "Led Active Status"
        }
    }
}

