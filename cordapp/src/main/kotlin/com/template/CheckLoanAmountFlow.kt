package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.states.RequestState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(RequestFlow::class)
class CheckLoanAmountFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val verifiedTransaction = subFlow(ReceiveTransactionFlow(counterPartySession))
        val output = verifiedTransaction.tx.outputs.single().data
        val loan = output as RequestState
        if(loan.loanAmount > 1000000){
        val regulator = serviceHub.identityService.partiesFromName("Regulator", true).single()
        subFlow(ReportToRegulatorFlow(regulator, verifiedTransaction))
        }
    }
}

@InitiatingFlow
class ReportToRegulatorFlow(private val regulator: Party, private val finalTx: SignedTransaction) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val session = initiateFlow(regulator)
        subFlow(SendTransactionFlow(session, finalTx))
    }
}

@InitiatedBy(ReportToRegulatorFlow::class)
class ReceiveRegulatoryReportFlow(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(otherSideSession, true, StatesToRecord.ALL_VISIBLE))
    }
}
