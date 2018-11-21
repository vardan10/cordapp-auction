package com.example.flow


import co.paralleluniverse.fibers.Suspendable
import com.example.contract.AuctionContract
import com.example.state.Auction
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * This flow deals with ending the auction
 */
@SchedulableFlow
@StartableByRPC
class EndAuction(val AuctionReference: String) : FlowLogic<SignedTransaction>() {

    /**
        * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
        */
    companion object {
        object GENERATING_TRANSACTION : Step("Creating a new Auction.")
        object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
        object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Pick a notary. Don't care which one.
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        // Stage 1.
        progressTracker.currentStep = GENERATING_TRANSACTION

        // Get the Auction state corresponding to the provided ID from our vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(AuctionReference)))
        val auctionInputStateAndRef = serviceHub.vaultService.queryBy<Auction>(queryCriteria).states.single()
        val auctionState = auctionInputStateAndRef.state.data
        val auctionOutputState = auctionState.copy(AuctionActive=false)
        val auctionOutputStateAndContract = StateAndContract(auctionOutputState, AuctionContract.CONTRACT_REF)

        val endAuctionCommand = Command(AuctionContract.End(), auctionState.itemOwner.owningKey)

        // Build, sign and record the transaction.
        val utx = TransactionBuilder(notary = notary).withItems(
                auctionOutputStateAndContract, // Output
                auctionInputStateAndRef, // Input
                endAuctionCommand  // Command
        )
        val stx = serviceHub.signInitialTransaction(utx)
        val ftx = subFlow(FinalityFlow(stx))

        // Broadcast this transaction to all parties on this business network.
        subFlow(BroadcastTransaction(ftx, auctionState.AuctionParticipants))

        return ftx
    }

}