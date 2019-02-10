package com.template.contracts

import com.template.contracts.LoanContract
import com.template.contracts.RequestContract
import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class LoanContractTest {
    private val ledgerServices = MockServices()
    private val testBorrower = TestIdentity(CordaX500Name("PartyA", "London", "GB"))
    private val testIntermediary = TestIdentity(CordaX500Name("PartyB", "London", "GB"))
    private val testLender = TestIdentity(CordaX500Name("PartyC", "New York", "US"))

    @Test
    fun `Transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party, UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                fails()
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
     fun `Create transaction must have one input state`() {
         ledgerServices.ledger {
             transaction {
                 output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                 command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                 command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                 `fails with`("Exactly one input state.")
             }
         }
     }

    @Test
    fun `Create transaction must have one output state`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                `fails with`("Exactly one output state.")
            }
        }
    }

    @Test
    fun `The loan's value must be non-negative`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(-10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                `fails with`("The loan's value must be non-negative")
            }
        }
    }

    @Test
    fun `The interest rate must be non-negative`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, -10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                `fails with`("The interest rate must be non-negative.")
            }
        }
    }

    @Test
    fun `The repayment schedule must be non-negative`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, -12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Create())
                `fails with`("The repayment schedule must be non-negative.")
            }
        }
    }

    @Test
    fun `Borrower and the lender must be signers`() {
        ledgerServices.ledger {
            transaction {
                input(RequestContract.REQUEST_CONTRACT_ID, RequestState(10000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),RequestContract.Commands.Create())
                command(listOf(testBorrower.publicKey),LoanContract.Commands.Create())
                `fails with`("There must be two signers.")
            }
        }
    }

    @Test
    fun `Transaction must include Update command`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                fails()
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Update transaction must have one input state`() {
        ledgerServices.ledger {
            transaction {
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Update())
                `fails with`("Exactly one input state.")
            }
        }
    }

    @Test
    fun `Update transaction must have one output state`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Update())
                `fails with`("Exactly one output state.")
            }
        }
    }

    @Test
    fun `Borrower and the lender must be signers for update`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey),LoanContract.Commands.Update())
                `fails with`("There must be two signers.")
            }
        }
    }

    @Test
    fun `Input state's amount should be greater than output state's amount`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(1000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Update())
                `fails with`("The input state amount should be greater than output state amount.")
            }
        }
    }

    @Test
    fun `Transaction must include Close command`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                fails()
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Close())
                verifies()
            }
        }
    }

    @Test
    fun `Close transaction must have one input state`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Close())
                `fails with`("Exactly one input state.")
            }
        }
    }

    @Test
    fun `Close transaction must have no output state`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                output(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testLender.publicKey, testBorrower.publicKey),LoanContract.Commands.Close())
                `fails with`("No output states.")
            }
        }
    }

    @Test
    fun `Borrower and the lender must be signers for Close transaction`() {
        ledgerServices.ledger {
            transaction {
                input(LoanContract.LOAN_CONTRACT_ID, LoanState(10000, 10, 12, testBorrower.party, testLender.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey),LoanContract.Commands.Close())
                `fails with`("There must be two signers.")
            }
        }
    }

}
