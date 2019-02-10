package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.LoanContract
import com.template.states.LoanState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class PayAmountFlow(val ID: UniqueIdentifier, val amount: Int) : FlowLogic<SignedTransaction>() {

    companion object {
        object INITIALISE : ProgressTracker.Step("Initialise flow.")
        object QUERY_VAULT : ProgressTracker.Step("Query vault for input states.")
        object BUILD_TX : ProgressTracker.Step("Build transaction.")
        object VERIFY_TX : ProgressTracker.Step("Verify transaction.")
        object SIGN_TX : ProgressTracker.Step("Sign transaction.")
        object OTHER_PARTY_SIGN : ProgressTracker.Step("Request other party for signature.")
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
        val inputState = serviceHub.vaultService.queryBy<LoanState>(vaultQueryCriteria).states.first()
        val lender = inputState.state.data.lender
        val chk = inputState.state.data.amountToBePaid - amount
        val txBuilder: TransactionBuilder
        val outputState: LoanState
        val loanStateCommand : Command<LoanContract.Commands>

        progressTracker.currentStep = BUILD_TX

        if (chk > 0) {
            outputState = LoanState(chk, inputState.state.data.interestRate, inputState.state.data.repaymentSchedule, ourIdentity, lender, ID)
            loanStateCommand = Command(LoanContract.Commands.Update(), listOf(ourIdentity.owningKey, lender.owningKey))
            txBuilder = TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(outputState, LoanContract.LOAN_CONTRACT_ID)
                    .addCommand(loanStateCommand)
        } else {
            loanStateCommand = Command(LoanContract.Commands.Close(), listOf(ourIdentity.owningKey, lender.owningKey))
            txBuilder = TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addCommand(loanStateCommand)
        }

        progressTracker.currentStep = VERIFY_TX
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGN_TX
        val partiallySignedTransaction = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = OTHER_PARTY_SIGN
        val otherPartySession = initiateFlow(lender)
        val completelySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))

        progressTracker.currentStep = FINALISE
        return subFlow(FinalityFlow(completelySignedTransaction))
    }
}

@InitiatedBy(PayAmountFlow::class)
class CheckAmountPaidFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                /*val output = stx.tx.outputs.single().data
                "This must be a loan record transaction." using (output is LoanState)
                val outputState = output as LoanState
                val input = stx.tx.inputs[0]
                val inputState = serviceHub.loadState(input).data as LoanState
                "The amount should be positive." using (outputState.amountToBePaid < inputState.amountToBePaid)*/
            }
        }
        return subFlow(signTransactionFlow)
    }
}
