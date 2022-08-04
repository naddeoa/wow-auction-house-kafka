package ai.whylabs.kafka

import WowAHKafkaBlog.enqueueAuctionData
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import kotlinx.coroutines.runBlocking


class Handler : RequestHandler<Map<String, String>, String> {
    override fun handleRequest(input: Map<String, String>, context: Context): String = runBlocking {
        enqueueAuctionData()
        "done"
    }
}