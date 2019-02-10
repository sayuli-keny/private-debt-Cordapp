package com.template.contracts

import com.template.contracts.RequestContract
import com.template.contracts.RequestContract.Companion.REQUEST_CONTRACT_ID
import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class RequestContractTest {
    private val ledgerServices = MockServices()
    private val testBorrower = TestIdentity(CordaX500Name("PartyA", "London", "GB"))
    private val testIntermediary = TestIdentity(CordaX500Name("PartyB", "New York", "US"))
    private val testLender = TestIdentity(CordaX500Name("PartyC", "New York", "US"))


    @Test
    fun `Transaction must include Issue command`() {
        ledgerServices.ledger {
            transaction {
                output(REQUEST_CONTRACT_ID, RequestState(1000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                fails()
                command(listOf(testBorrower.publicKey), RequestContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `Issue transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(REQUEST_CONTRACT_ID, RequestState(1000,  testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                output(REQUEST_CONTRACT_ID, RequestState(1000,  testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey), RequestContract.Commands.Issue())
                `fails with`("No inputs should be consumed when raising a loan request.")
            }
        }
    }

    @Test
    fun `Transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(REQUEST_CONTRACT_ID, RequestState(1000, testBorrower.party, testIntermediary.party, UniqueIdentifier("123a")))
                output(REQUEST_CONTRACT_ID, RequestState(1000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey), RequestContract.Commands.Issue())
                `fails with`("There should be one output state of type RequestState.")
            }
        }
    }

    @Test
    fun `Borrower  must be signer`() {
        ledgerServices.ledger {
            transaction {
                output(REQUEST_CONTRACT_ID, RequestState(1000, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey,testIntermediary.publicKey), RequestContract.Commands.Issue())
                `fails with`("There must be one signer.")
            }
        }
    }

    @Test
    fun `The loan's value must be non-negative`() {
        ledgerServices.ledger {
            transaction {
                output(REQUEST_CONTRACT_ID, RequestState(-10, testBorrower.party, testIntermediary.party,UniqueIdentifier("123a")))
                command(listOf(testBorrower.publicKey, testIntermediary.publicKey), RequestContract.Commands.Issue())
                `fails with`("The loan's value must be non-negative.")
            }
        }
    }

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
    fun `The loan's value must be non-negative for create transaction`() {
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
    fun `The interest rate must be non-negative for create transaction`() {
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
    fun `The repayment schedule must be non-negative for create transaction`() {
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
    fun `Borrower and the lender must be signers for create transaction`() {
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

}