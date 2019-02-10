package com.template.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

class LoanState (val amountToBePaid: Int, val interestRate : Int, val repaymentSchedule : Int, val borrower: Party, val lender: Party, override val linearId: UniqueIdentifier) : LinearState{
    override val participants: List<AbstractParty> = listOf(borrower,lender)
}