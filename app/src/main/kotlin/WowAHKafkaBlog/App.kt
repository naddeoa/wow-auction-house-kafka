package WowAHKafkaBlog

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpUpload
import com.github.kittinunf.result.Result
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.fullJitterBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import io.javalin.Javalin
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

val logger = KotlinLogging.logger("WowAHKafkaBlog")
val mapper = jacksonObjectMapper()

val kafkaCluster = System.getenv("KAFKA_BOOTSTRAP_SERVER") ?: "localhost:9093"
val kafkaTopic = System.getenv("KAFKA_TOPIC") ?: "wow-ah"

val apiClientId =
    System.getenv("API_CLIENT_ID")?.ifEmpty { throw IllegalArgumentException("Missing API_CLIENT_ID") }
        ?: throw IllegalArgumentException("Missing API_CLIENT_ID")
val apiSecret = System.getenv("API_CLIENT_SECRET")?.ifEmpty { throw IllegalArgumentException("Missing API_CLIENT_SECRET") }
    ?: throw IllegalArgumentException("Missing API_CLIENT_SECRET")


data class BlizzardOAuthToken(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val sub: String
)

fun refreshBlizzardAccessToken(): String {
    val grant = listOf("grant_type" to "client_credentials")
    val (request, response, result) = "https://us.battle.net/oauth/token"
        .httpUpload(grant, Method.POST)
        .authentication().basic(apiClientId, apiSecret)
        .responseString()

    if (result is Result.Failure) {
        val ex = result.getException()
        logger.error(ex) { "Error pulling server info" }
    }
    val dataString = result.get()
    val blizzardResponse = mapper.readValue<BlizzardOAuthToken>(dataString)
    return blizzardResponse.access_token
}


fun forPath(array: JsonNode, block: (Int, String, Any) -> Unit) {
    if (!array.isArray) {
        throw IllegalArgumentException("Must be an array")
    }

    fun helper(node: JsonNode, objId: Int = 0, path: String = "") {
        if (node.isArray) {
            node.forEachIndexed { index, item ->
                val newPath = if (path.isEmpty()) path else "$path.$index"
                val id = if (path.isEmpty()) index else objId
                helper(item, id, newPath)
            }
        } else {
            node.fields().forEach { (name, value) ->
                val prefix = if (path.isEmpty()) "" else "$path."
                val newPath = "$prefix$name"

                if (value.isArray || value.isObject) {
                    helper(value, objId, newPath)
                } else {
                    block(objId, newPath, value)
                }
            }
        }
    }

    helper(array)
}

fun toFlatDict(array: JsonNode): Collection<Map<String, Any>> {
    val allItems = mutableMapOf<Int, MutableMap<String, Any>>()
    forPath(array) { id, key, value ->
        val item = allItems.computeIfAbsent(id) { mutableMapOf() }
        item[key] = value
    }

    return allItems.values
}

fun getKafkaConsumer(): KafkaProducer<String, String> {
    logger.info("Connecting to kafka cluster at $kafkaCluster")
    val consumerConfig = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaCluster,
        ProducerConfig.CLIENT_ID_CONFIG to kafkaTopic,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
    )
    return KafkaProducer<String, String>(consumerConfig)
}

var lastRunModifiedIso: String? = null
fun getAuctionHouseData(): JsonNode? {
    logger.info("Making auction house api call")
    val token = refreshBlizzardAccessToken()
    val (request, response, result) = "https://us.api.blizzard.com/data/wow/connected-realm/9/auctions?namespace=dynamic-us&locale=en_US&access_token=$token"
        .httpGet()
        .header("If-Modified-Since", lastRunModifiedIso ?: "") // Avoid us parsing the giant payload if there is nothing new
        .responseString()

    if (result is Result.Failure) {
        val ex = result.getException()
        logger.error(ex) { "Error pulling server info" }
        return null
    }

    val dataString = result.get()

    if (dataString.isNullOrEmpty()) {
        logger.warn("Nothing new since $lastRunModifiedIso, bailing out.")
        return null
    }

    val data = mapper.readTree(dataString)
    val auctions = data.get("auctions")
    val lastModifiedIso = response.headers["Last-Modified"].first()

    if (lastModifiedIso == lastRunModifiedIso) {
        // This probably can't happen after adding the If-Modified-Since header, but just in case.
        logger.warn("Ran too soon since last run. Skipping")
        return null
    }

    lastRunModifiedIso = lastModifiedIso
    logger.info("Got AH data updated at $lastRunModifiedIso")
    logger.info("Got ${auctions.size()} auctions")
    return auctions
}

val defaultRetryPolicy: RetryPolicy<Throwable> = limitAttempts(5) + fullJitterBackoff(base = 10, max = 1000)
fun enqueueAuctionData() = runBlocking {
    try {
        val auctions = retry(defaultRetryPolicy) {
            getAuctionHouseData()
        } ?: return@runBlocking

        getKafkaConsumer().use { producer ->
            // For each auction, send to kafka
            logger.info("Sending auctions to kafka")
            toFlatDict(auctions).forEach { data ->
                val record = ProducerRecord<String, String>(kafkaTopic, mapper.writeValueAsString(data))
                producer.send(record)
            }
        }
    } catch (ex: Throwable) {
        // Swallow errors here to avoid executorService cancelling the schedule
        logger.error(ex) { "Error pulling auction data" }
    }
}

private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
fun main() {
    executorService.scheduleWithFixedDelay(
        ::enqueueAuctionData,
        0,
        Duration.of(10, ChronoUnit.MINUTES).seconds,
        TimeUnit.SECONDS
    )

    Javalin.create().start(8089)
}
