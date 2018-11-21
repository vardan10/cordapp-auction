package com.example.contract

import com.example.state.Auction
import com.example.state.Bid
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

// TODO We need to improve this contract code so it works with confidential identities.
class AuctionContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "com.example.contract.AuctionContract"
    }

    interface Commands : CommandData
    class Start : TypeOnlyCommandData(), Commands
    class End : TypeOnlyCommandData(), Commands
    class AcceptBid : TypeOnlyCommandData(), Commands

    override fun verify(tx: LedgerTransaction) {
        val auctionCommand = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = auctionCommand.signers.toSet()

        when (auctionCommand.value) {
            is Start -> verifyStart(tx, setOfSigners)
            is End -> verifyEnd(tx, setOfSigners)
            is AcceptBid -> verifyBid(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyStart(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "No inputs should be consumed when starting a auction." using (tx.inputStates.isEmpty())
        "Only one Auction state should be created when starting a Auction." using (tx.outputStates.size == 1)

        // There can only be one output state and it must be a Auction state.
        val auction = tx.outputStates.single() as Auction

        // Assert stuff over the state.
        "A newly issued auction must have a positive start price." using (auction.startPrice > 0)
        "There must be a auction item name." using (auction.itemName != "")

        // Assert correct signers.
        "The auction must be signed by the manager only." using (signers.contains(auction.itemOwner.owningKey))
    }

    private fun verifyBid(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "The can only one input state in an accept bid transaction." using (tx.inputStates.size == 1)
        "There must be two output states in an accept bid transaction." using (tx.outputStates.size == 2)

        val auctionInput = tx.inputsOfType<Auction>().single()
        val auctionOutput = tx.outputsOfType<Auction>().single()
        val bidOutput = tx.outputsOfType<Bid>().single()

        // Assert stuff about the bid in relation to the auction state.
        "The bid must be for this acution." using (bidOutput.auctionReference == auctionOutput.linearId)
        "The auction must be updated by the amount bided." using (bidOutput.amount == auctionOutput.highestBid)
        "The bid must be higher than start price" using (bidOutput.amount > auctionOutput.startPrice)
        "The bid must be higher than highest bid" using (bidOutput.amount > auctionInput.highestBid)

        // Assert correct signer.
        "The auction must be signed by the manager only." using (signers.contains(auctionInput.itemOwner.owningKey))
    }

    private fun verifyEnd(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "Only one auction can end per transaction." using (tx.inputsOfType<Auction>().size == 1)
        "There must be no bid output states when ending a auction." using (tx.outputsOfType<Bid>().isEmpty())

        // Get references to auction state.
        val auction = tx.inputsOfType<Auction>().single()

        // Check the auction state is signed by the auction manager.
        "Ending auction transactions must be signed by the auction manager." using (signers.contains(auction.itemOwner.owningKey))
    }

}