package com.aemerse.muserse.model


class InvitationItem {
    var invitationId = ""
    var invitationAccepted = false

    constructor()
    constructor(invitationId: String, invitationAccepted: Boolean) {
        this.invitationId = invitationId
        this.invitationAccepted = invitationAccepted
    }
}