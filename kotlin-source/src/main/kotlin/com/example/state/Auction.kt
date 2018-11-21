package com.example.state

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant


data class Auction(
        val itemName: String,
        val ItemDescription: String,
        val startPrice: Int,
        val ExpiryDate: Instant,
        val itemOwner: Party,
        val auctionWinner: Party ?= null,
        val highestBid: Int,
        val AuctionActive: Boolean,
        val AuctionParticipants: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState  {
    override val participants: List<AbstractParty> = if (auctionWinner!=null) listOf(itemOwner,auctionWinner) else listOf(itemOwner)
}