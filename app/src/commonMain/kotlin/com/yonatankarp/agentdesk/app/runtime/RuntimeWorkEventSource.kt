package com.yonatankarp.agentdesk.app.runtime

interface RuntimeWorkEventSource {
    fun loadObservations(): List<RuntimeWorkObservation>
}
