package com.template.FlowTest

import com.google.common.collect.ImmutableList
import com.template.LoanRecordFlow
import com.template.PayAmountFlow
import com.template.RequestFlow
import com.template.contracts.LoanContract
import com.template.states.LoanState
import com.template.states.RequestState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PayAmountFlowTest {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode
    lateinit var requestID: UniqueIdentifier

    @Before
    fun setup() {
        network = MockNetwork(ImmutableList.of("com.template"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()

        val flow = RequestFlow(10000, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val results = future.getOrThrow()
        val newRequestState = results.tx.outputs.single().data as RequestState
        requestID = newRequestState.linearId
        network.runNetwork()
        val flow1 = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        val results1 = future1.getOrThrow()
        val newLoanState = results1.tx.outputs.single().data as LoanState
        requestID = newLoanState.linearId
        network.runNetwork()

    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedByFlowUsesNotary() {
        val flow = PayAmountFlow(requestID, 1000)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val output = signedTransaction.tx.outputs[0]

        assertEquals(network.notaryNodes[0].info.legalIdentities[0], output.notary)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneInputLoanStateWithCorrectParameters() {
        val flow = PayAmountFlow(requestID, 1000)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.inputs.size.toLong())
        val input = signedTransaction.tx.inputs[0]
        val inputState = a.services.loadState(input).data as LoanState
        assertEquals(a.info.legalIdentities[0], inputState.borrower)
        assertEquals(c.info.legalIdentities[0], inputState.lender)
        assertEquals(10000, inputState.amountToBePaid)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneOutputStateLoanStateWithCorrectParameters() {
        val flow = PayAmountFlow(requestID, 1000)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val output = signedTransaction.tx.outputsOfType(LoanState::class.java)[0]
        assertEquals(a.info.legalIdentities[0], output.borrower)
        assertEquals(c.info.legalIdentities[0], output.lender)
        assertEquals(9000, output.amountToBePaid)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneOutputStateWithLoanContract() {
        val flow = PayAmountFlow(requestID, 1000)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val (_, contract) = signedTransaction.tx.outputs[0]
        assertEquals(LoanContract.LOAN_CONTRACT_ID, contract)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasCreateCommand() {
        val flow = PayAmountFlow(requestID, 1000)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size.toLong())
        val (value1, _) = signedTransaction.tx.commands[0]
        assert(value1 is LoanContract.Commands.Update)
    }
}
