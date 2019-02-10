package com.template.states

import net.corda.core.contracts.*
import net.corda.core.identity.Party

class RequestState(val loanAmount: Int, val borrower: Party, val intermediary: Party, override val linearId: UniqueIdentifier) : LinearState {
    override val participants get() = listOf(borrower, intermediary)
}