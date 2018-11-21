package com.example.contract

import com.example.state.Auction
import com.example.state.Bid
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class BidContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "com.example.contract.BidContract"
    }

    interface Commands : CommandData
    class Create : TypeOnlyCommandData(), Commands

    override fun verify(tx: LedgerTransaction) {
        // We only need the bid commands at this point to determine which part of the contract code to run.
        val bidCommand = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = bidCommand.signers.toSet()

        when (bidCommand.value) {
            is Create -> verifyCreate(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "The can only one input state in an create bid transaction." using (tx.inputStates.size == 1)
        "There must be two output states in an create bid transaction." using (tx.outputStates.size == 2)

        val auctionInput = tx.inputsOfType<Auction>().single()
        val auctionOutput = tx.outputsOfType<Auction>().single()
        val bidOutput = tx.outputsOfType<Bid>().single()

        // Assert stuff about the bid in relation to the auction state.
        "The bid must be for this auction." using (bidOutput.auctionReference == auctionOutput.linearId)
        "The auction must be updated by the amount bid." using (bidOutput.amount == auctionOutput.highestBid)
        "The bid must be higher than start price" using (bidOutput.amount > auctionOutput.startPrice)
        "The bid must be higher than highest bid" using (bidOutput.amount > auctionInput.highestBid)

        // Assert correct signer.
        "The auction must be signed by the manager and bidder." using (signers.containsAll(listOf(auctionInput.itemOwner.owningKey,bidOutput.bidder.owningKey)))
    }

}