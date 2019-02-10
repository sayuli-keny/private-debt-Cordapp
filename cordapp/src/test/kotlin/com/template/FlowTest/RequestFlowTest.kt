package com.template.FlowTest

import com.template.CheckLoanAmountFlow
import com.template.RequestFlow
import com.template.contracts.RequestContract
import com.template.states.RequestState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.template.contracts"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        listOf(a, b).forEach { it.registerInitiatedFlow(CheckLoanAmountFlow::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun flowRejectsInvalidRequest() {
        val flow = RequestFlow(-10000, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    @Throws(Exception::class)
    fun flowUsesNotary() {
        val flow = RequestFlow(10000, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        val output = signedTransaction.tx.outputs[0]
        assertEquals(network.notaryNodes[0].info.legalIdentities[0], output.notary)
    }

    @Test
    @Throws(Exception::class)
    fun transactionHasOneRequestStateOutput() {
        val flow = RequestFlow(10000, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val output = signedTransaction.tx.outputsOfType(RequestState::class.java)[0]
        assertEquals(a.info.legalIdentities[0], output.borrower)
        assertEquals(b.info.legalIdentities[0], output.intermediary)
        assertEquals(10000, output.loanAmount)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneOutputStateWithRequestContract() {
        val flow = RequestFlow(10000, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val (_, contract) = signedTransaction.tx.outputs[0]
        assertEquals("com.template.contracts.RequestContract", contract)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneIssueCommand() {
        val flow = RequestFlow(10000, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.commands.size.toLong())
        val (value) = signedTransaction.tx.commands[0]

        assert(value is RequestContract.Commands.Issue)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneCommandWithBorrowerAsSigner() {
        val flow = RequestFlow(10000, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.commands.size.toLong())
        val (_, signers) = signedTransaction.tx.commands[0]
        assertEquals(1, signers.size.toLong())
        assert(signers.contains(a.info.legalIdentities[0].owningKey))
    }
}
