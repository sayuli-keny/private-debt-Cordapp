package com.template.contracts

import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class LoanContract : Contract {
    companion object {
        //Used to identify our contract when building a transaction.
        const val LOAN_CONTRACT_ID = "com.template.contracts.LoanContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<LoanContract.Commands>()
        when (command.value) {
            is LoanContract.Commands.Create ->
                requireThat {
                    "Exactly one input state." using (tx.inputs.size == 1)
                    "The single input is of type RequestState" using (tx.inputsOfType<RequestState>().size == 1)
                    "Exactly one output state." using (tx.outputs.size == 1)
                    "The single output is of type LoanState" using (tx.outputsOfType<LoanState>().size == 1)
                    val output = tx.outputsOfType<LoanState>().single()
                    "The loan's value must be non-negative." using (output.amountToBePaid > 0)
                    "The interest rate must be non-negative." using (output.interestRate > 0)
                    "The repayment schedule must be non-negative." using (output.repaymentSchedule > 0)

                    // Constraints on the signers.
                    val expectedSigners = listOf(output.borrower.owningKey, output.lender.owningKey)
                    "There must be two signers." using (command.signers.toSet().size == 2)
                    "The borrower and lender must be signers." using (command.signers.containsAll(expectedSigners))
                }
            is LoanContract.Commands.Update ->
                requireThat {
                    "Exactly one input state." using (tx.inputs.size == 1)
                    "The single input is of type LoanState" using (tx.inputsOfType<LoanState>().size == 1)
                    "Exactly one output state." using (tx.outputs.size == 1)
                    "The single output is of type LoanState" using (tx.outputsOfType<LoanState>().size == 1)
                    val output = tx.outputsOfType<LoanState>().single()
                    "The loan's value must be non-negative." using (output.amountToBePaid > 0)
                    "The interest rate must be non-negative." using (output.interestRate > 0)
                    "The repayment schedule must be non-negative." using (output.repaymentSchedule > 0)
                    val input = tx.inputsOfType<LoanState>().single()
                    "The input state amount should be greater than output state amount." using (input.amountToBePaid >= output.amountToBePaid)
                    // Constraints on the signers.
                    val expectedSigners = listOf(output.borrower.owningKey, output.lender.owningKey)
                    "There must be two signers." using (command.signers.toSet().size == 2)
                    "The borrower and lender must be signers." using (command.signers.containsAll(expectedSigners))
                }
            is LoanContract.Commands.Close ->
                requireThat {
                    "Exactly one input state." using (tx.inputs.size == 1)
                    "The single input is of type LoanState" using (tx.inputsOfType<LoanState>().size == 1)
                    "No output states." using (tx.outputs.isEmpty())
                    val input = tx.inputsOfType<LoanState>().single()
                    // Constraints on the signers.
                    val expectedSigners = listOf(input.borrower.owningKey, input.lender.owningKey)
                    "There must be two signers." using (command.signers.toSet().size == 2)
                    "The borrower and lender must be signers." using (command.signers.containsAll(expectedSigners))
                }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
        class Close : Commands
    }
}