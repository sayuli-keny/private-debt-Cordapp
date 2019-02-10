package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.LoanContract
import com.template.contracts.RequestContract
import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class LoanRecordFlow(val ID: UniqueIdentifier, val interestRate: Int, val repaymentSchedule: Int, val lender: Party) : FlowLogic<SignedTransaction>() {

    companion object {
        object INITIALISE : ProgressTracker.Step("Initialise flow.")
        object QUERY_VAULT : ProgressTracker.Step("Query vault for input states.")
        object BUILD_TX : ProgressTracker.Step("Build transaction.")
        object VERIFY_TX : ProgressTracker.Step("Verify transaction.")
        object SIGN_TX : ProgressTracker.Step("sign transaction.")
        object OTHER_PARTY_SIGN : ProgressTracker.Step("Request other party signature.")
        object FINALISE : ProgressTracker.Step("Finalise transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISE, QUERY_VAULT, BUILD_TX,
                VERIFY_TX, SIGN_TX, OTHER_PARTY_SIGN, FINALISE)
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        progressTracker.currentStep = INITIALISE

        progressTracker.currentStep = QUERY_VAULT
        val vaultQueryCriteria = QueryCriteria.LinearStateQueryCriteria(listOf(ourIdentity), listOf(ID))
        val inputState = serviceHub.vaultService.queryBy<RequestState>(vaultQueryCriteria).states.first()

        val outputState = LoanState(inputState.state.data.loanAmount, interestRate, repaymentSchedule, ourIdentity, lender,ID)
        val requestStateCommand = Command(RequestContract.Commands.Create(), listOf(ourIdentity.owningKey, lender.owningKey))
        val loanStateCommand = Command(LoanContract.Commands.Create(), listOf(ourIdentity.owningKey, lender.owningKey))

        progressTracker.currentStep = BUILD_TX
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputState)
                .addOutputState(outputState, LoanContract.LOAN_CONTRACT_ID)
                .addCommand(requestStateCommand)
                .addCommand(loanStateCommand)

        progressTracker.currentStep = VERIFY_TX
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGN_TX
        val partiallySignedTransaction = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = OTHER_PARTY_SIGN
        val otherPartySession = initiateFlow(outputState.lender)
        val completelySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))

        progressTracker.currentStep = FINALISE
        return subFlow(FinalityFlow(completelySignedTransaction))
    }
}

@InitiatedBy(LoanRecordFlow::class)
class ResponderFlow(val counterpartySession: FlowSession) :FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call() : SignedTransaction{

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a loan record transaction." using (output is LoanState)
                val outputState = output as LoanState
                "the loan amount should be positive" using (outputState.amountToBePaid>0)
            }
        }
        return subFlow(signTransactionFlow)
    }
}
