package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.sun.org.apache.xalan.internal.lib.NodeInfo
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Filters out any notary identities and removes our identity, then broadcasts the [SignedTransaction] to all the
 * remaining identities.
 */
@InitiatingFlow
class BroadcastTransaction(val stx: SignedTransaction,val nodes: List<Party>) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        // Get a list of all identities from the network map cache.
        var everyone = serviceHub.networkMapCache.allNodes.flatMap { it.legalIdentities }

        // If nodes list is not empty, only broadcast to this nodes
        if (!nodes.isEmpty()){
            for (party in everyone){
                if (!(nodes.contains(party))){
                    everyone = everyone.filter { it.equals(party).not() }
                }
            }
        }

        // Filter out the notary identities and remove our identity.
        val everyoneButMeAndNotary = everyone.filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity

        // Create a session for each remaining party.
        val sessions = everyoneButMeAndNotary.map { initiateFlow(it) }

        // Send the transaction to all the remaining parties.
        sessions.forEach { subFlow(SendTransactionFlow(it, stx)) }
    }

}