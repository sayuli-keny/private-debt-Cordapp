package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RequestContract
import com.template.states.RequestState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RequestFlow(val loanAmount: Int,
                      val intermediary: Party):FlowLogic<SignedTransaction>() {

    companion object {
        object INITIALISE : ProgressTracker.Step("Initialising flow.")
        object BUILD_TX : ProgressTracker.Step("Building transaction.")
        object VERIFY_TX : ProgressTracker.Step("Verifying transaction.")
        object SIGN_TX : ProgressTracker.Step("signing transaction.")
        object FINALISE : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISE, BUILD_TX,
                VERIFY_TX, SIGN_TX, FINALISE)
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALISE
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val outputState = RequestState(loanAmount, ourIdentity, intermediary, UniqueIdentifier())

        progressTracker.currentStep = BUILD_TX
        val transactionBuilder = TransactionBuilder(notary).
                addOutputState(outputState, RequestContract.REQUEST_CONTRACT_ID).
                addCommand(RequestContract.Commands.Issue(), ourIdentity.owningKey)

        progressTracker.currentStep = VERIFY_TX
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGN_TX
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val session = initiateFlow(outputState.intermediary)
        subFlow(SendTransactionFlow(session,signedTransaction))

        progressTracker.currentStep = FINALISE
        return subFlow(FinalityFlow(signedTransaction))
    }
}
