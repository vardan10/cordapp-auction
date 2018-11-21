package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for TradeState.
 */
object BidSchema

/**
 * An TradeState schema.
 */
object BidSchemaV1 : MappedSchema(
        schemaFamily = BidSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBid::class.java)) {
    @Entity
    @Table(name = "trade_states")
    class PersistentBid(
            @Column(name = "itemOwner")
            var itemOwner: String,

            @Column(name = "bidder")
            var bidder: String,

            @Column(name = "amount")
            var amount: Int,

            @Column(name = "auctionReference")
            var auctionReference: String,

            @Column(name = "linear_id")
            var linearId: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0,"",UUID.randomUUID().toString())
    }
}