package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.contract.AuctionContract
import com.example.state.Auction
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NodeInfo
import java.time.Instant;
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * A flow that handles the starting of a new auctions. It creates a new [auction] and stores it in in the vault of the
 * node that runs this flow (the manager of the auction) and then broadcasts it to all the other nodes on the
 * crowdFunding business network.
 *
 * The nodes receiving the broadcast use the observable states feature by recording all visible output states despite
 * the fact the only participant for the [auction] start is the [manager] of the [auction].
 */
@StartableByRPC
class StartAuction( val itemName: String,
                    val ItemDescription: String,
                    val startPrice: Int,
                    val ExpiryDate: String,
                    val AuctionParticipants: String) : FlowLogic<SignedTransaction>() {

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

        val allAuctionParticipants: MutableList<Party> = mutableListOf()
        if (AuctionParticipants != "None"){
            val participants = AuctionParticipants.split('$');
            for (participant in participants){

                val organization = participant.split(',')[0].split('=')[1]
                val locality = participant.split(',')[1].split('=')[1]
                val country = participant.split(',')[2].split('=')[1]

                var p = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name(organization,locality,country));
                if (p != null) {
                    allAuctionParticipants.add(p);
                }
            }
        }



        // Pick a notary. Don't care which one.
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        // Stage 1.
        progressTracker.currentStep = GENERATING_TRANSACTION

        // Assemble the transaction components.
        val newAuction = Auction(itemName,ItemDescription,startPrice,Instant.parse(ExpiryDate),serviceHub.myInfo.legalIdentities.first(), null, 0, true,allAuctionParticipants)
        val startCommand = Command(AuctionContract.Start(), listOf(ourIdentity.owningKey))
        val outputState = StateAndContract(newAuction, AuctionContract.CONTRACT_REF)

        // Build, sign and record the transaction.
        val utx = TransactionBuilder(notary = notary).withItems(outputState, startCommand)
        val stx = serviceHub.signInitialTransaction(utx)
        val ftx = subFlow(FinalityFlow(stx))

        // Broadcast this transaction to all parties on this business network.
        subFlow(BroadcastTransaction(ftx,allAuctionParticipants))

        return ftx
    }

}