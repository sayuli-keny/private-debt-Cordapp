package com.template.FlowTest

import com.google.common.collect.ImmutableList
import com.template.LoanRecordFlow
import com.template.RequestFlow
import com.template.contracts.LoanContract
import com.template.contracts.RequestContract
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

class LoanRecordFlowTest {
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
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowUsesNotary() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputs[0]
        assertEquals(network.notaryNodes[0].info.legalIdentities[0], output.notary)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneInputRequestState() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.inputs.size)
        val input = signedTransaction.tx.inputs[0]
        val inputStateAndContract = a.services.toStateAndRef<RequestState>(input)
        assertEquals(a.info.legalIdentities[0], inputStateAndContract.state.data.borrower)
        assertEquals(b.info.legalIdentities[0], inputStateAndContract.state.data.intermediary)
        assertEquals(10000, inputStateAndContract.state.data.loanAmount)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneInputUsingRequestContract() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.inputs.size)
        val input = signedTransaction.tx.inputs[0]
        val inputStateAndContract = a.services.toStateAndRef<RequestState>(input)
        assertEquals(RequestContract.REQUEST_CONTRACT_ID, inputStateAndContract.state.contract)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneOutputStateLoanState() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputsOfType(LoanState::class.java)[0]
        assertEquals(a.info.legalIdentities[0], output.borrower)
        assertEquals(c.info.legalIdentities[0], output.lender)
        assertEquals(10000, output.amountToBePaid)
    }


    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasOneOutputUsingLoanContract() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val (_, contract) = signedTransaction.tx.outputs[0]
        assertEquals(LoanContract.LOAN_CONTRACT_ID, contract)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowHasTwoCreateCommands() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val (value, _) = signedTransaction.tx.commands[0]
        assert(value is RequestContract.Commands.Create)
        val (value1, _) = signedTransaction.tx.commands[1]
        assert(value1 is LoanContract.Commands.Create)
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByFlowSignedByBorrowerAndLender() {
        val flow = LoanRecordFlow(requestID, 10,1, c.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val (_, signers) = signedTransaction.tx.commands[0]
        assertEquals(2, signers.size.toLong())
        assert(signers.containsAll(listOf(a.info.legalIdentities[0].owningKey, c.info.legalIdentities[0].owningKey)))
    }
}
