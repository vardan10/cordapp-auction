package com.example.api

import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED
import com.example.state.Auction
import com.example.flow.StartAuction
import com.example.flow.EndAuction
import com.example.flow.MakeBid.Initiator
import net.corda.core.identity.Party
import net.corda.core.contracts.UniqueIdentifier

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/auction. All paths specified below are relative to it.
@Path("auction")
class AuctionApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<AuctionApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered withf the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all auction states that exist in the node's vault.
     */
    @GET
    @Path("auctions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAuctions() = rpcOps.vaultQueryBy<Auction>().states

    /**
     * Get full Auction details.
     */
    @GET
    @Path("get-auction")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAuction(@QueryParam("linearID") linearID: String): Response {
        if (linearID == "") {
            return Response.status(BAD_REQUEST).entity("Query parameter 'linearID' missing or has wrong format.\n").build()
        }
        val idParts = linearID.split('_')
        val uuid = idParts[idParts.size - 1]
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(uuid)),status = Vault.StateStatus.ALL)
        return try {
            Response.ok(rpcOps.vaultQueryBy<Auction>(criteria=criteria).states).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }


    /**
     * Initiates Start Auction Flow.
     */
    @PUT
    @Path("create-auction")
    fun createAuction(@QueryParam("itemName") itemName: String,@QueryParam("ItemDescription") ItemDescription: String,@QueryParam("startPrice") startPrice: Int,@QueryParam("ExpiryDate") ExpiryDate: String,@QueryParam("AuctionParticipants") AuctionParticipants: String): Response {
        if (startPrice <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'start Price' must be non-negative.\n").build()
        }

        return try {
            val signedTx = rpcOps.startTrackedFlow(::StartAuction, itemName, ItemDescription, startPrice, ExpiryDate, AuctionParticipants).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    /**
     * Initiates Make Bid Flow.
     */
    @PUT
    @Path("make-bid")
    fun makeBid(@QueryParam("amount") amount: Int,@QueryParam("AuctionReference") AuctionReference: String): Response {
        if (AuctionReference == "" ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'Auction Reference' must not be null.\n").build()
        }

        return try {
            val signedTx = rpcOps.startTrackedFlow(::Initiator, amount, AuctionReference).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    /**
     * Initiates End Auction Flow.
     */
    @PUT
    @Path("end-auction")
    fun endAuction(@QueryParam("AuctionReference") AuctionReference: String): Response {
        return try {
            val signedTx = rpcOps.startTrackedFlow(::EndAuction, AuctionReference).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

}