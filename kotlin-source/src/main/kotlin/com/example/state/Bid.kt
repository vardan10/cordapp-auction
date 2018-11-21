package com.example.state

import com.example.schema.BidSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class Bid(
        val amount: Int,
        val bidder: Party,
        val itemOwner: Party,
        val auctionReference: UniqueIdentifier,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(bidder,itemOwner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BidSchemaV1 -> BidSchemaV1.PersistentBid(
                    this.itemOwner.name.toString(),
                    this.bidder.name.toString(),
                    this.amount,
                    this.auctionReference.id.toString(),
                    this.linearId.id.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BidSchemaV1)


}