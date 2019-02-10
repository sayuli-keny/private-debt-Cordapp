package com.template.contracts

import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class RequestContract : Contract {
    companion object {
        const val REQUEST_CONTRACT_ID = "com.template.contracts.RequestContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is RequestContract.Commands.Issue ->
                requireThat {
                    "No inputs should be consumed when raising a loan request." using (tx.inputs.isEmpty())
                    "There should be one output state of type RequestState." using (tx.outputs.size == 1)
                    val output = tx.outputsOfType<RequestState>().single()
                    "The loan's value must be non-negative." using (output.loanAmount > 0)
                    val expectedSigners = listOf(output.borrower.owningKey)
                    "There must be one signer." using (command.signers.toSet().size == 1)
                    "The borrower is the signer" using (command.signers.containsAll(expectedSigners))
                }

            is RequestContract.Commands.Create ->
                requireThat {
                    "Exactly one input state." using(tx.inputs.size == 1 )
                    "The single input is of type RequestState" using(tx.inputsOfType<RequestState>().size == 1)
                    "Exactly one output state." using (tx.outputs.size == 1)
                    "The single output is of type LoanState" using (tx.outputsOfType<LoanState>().size == 1)
                    val output = tx.outputsOfType<LoanState>().single()
                    "The loan's value must be non-negative." using (output.amountToBePaid > 0)
                    val expectedSigners = listOf(output.borrower.owningKey, output.lender.owningKey)
                    "There must be two signers." using (command.signers.toSet().size == 2)
                    "The itermediary and lender must be signers." using (command.signers.containsAll(expectedSigners))
                }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Create : Commands
    }
}