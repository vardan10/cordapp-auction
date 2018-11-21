package com.example.plugin

import com.example.api.AuctionApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class AuctionPlugin : WebServerPluginRegistry {
   /**
    * A list of classes that expose web APIs.
    */
   override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::AuctionApi))

   /**
    * A list of directories in the resources directory that will be served by Jetty under /web.
    */
   override val staticServeDirs: Map<String, String> = mapOf(
           // This will serve the auctionWeb directory in resources to /web/auction
           "auction" to javaClass.classLoader.getResource("auctionWeb").toExternalForm()
   )
}
