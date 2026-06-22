package com.yonatankarp.agentdesk.app.runtime

enum class RuntimeHostOperation(val wireName: String) {
    ReachabilityDiagnostic("reachability-diagnostic"),
    ReadObservation("read-observation"),
    InspectActionProposal("inspect-action-proposal"),
    MutatingLiveAction("mutating-live-action"),
}
