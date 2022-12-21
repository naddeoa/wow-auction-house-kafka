package WowAHKafkaBlog

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
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

val kafkaCluster = System.getenv("KAFKA_BOOTSTRAP_SERVER") ?: "localhost:9092"
val kafkaTopic = System.getenv("KAFKA_TOPIC") ?: "wow-ah"

val s3Bucket: String? = System.getenv("S3_BUCKET")
val s3CsvPrefix: String? = System.getenv("S3_CSV_PREFIX")

val apiClientId =
    System.getenv("API_CLIENT_ID")?.ifEmpty { throw IllegalArgumentException("Missing API_CLIENT_ID") }
        ?: throw IllegalArgumentException("Missing API_CLIENT_ID")
val apiSecret = System.getenv("API_CLIENT_SECRET")?.ifEmpty { throw IllegalArgumentException("Missing API_CLIENT_SECRET") }
    ?: throw IllegalArgumentException("Missing API_CLIENT_SECRET")


val s3Client: AmazonS3 by lazy {
    val credentials = DefaultAWSCredentialsProviderChain()
    AmazonS3ClientBuilder.standard().withRegion("us-west-2").withCredentials(credentials)
        .build()
}


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
        logger.error(ex) { "Error refreshing access token." }
        throw ex
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

typealias FlatDict = Collection<Map<String, Any>>

fun toFlatDict(array: JsonNode): FlatDict {
    val allItems = mutableMapOf<Int, MutableMap<String, Any>>()
    forPath(array) { id, key, value ->
        val item = allItems.computeIfAbsent(id) { mutableMapOf() }
        item[key] = value
    }

    return allItems.values
}

enum class AuctionTimeLeft(val jsonValue: String) {
    SHORT("\"SHORT\""),
    MEDIUM("\"MEDIUM\""),
    LONG("\"LONG\""),
    VERY_LONG("\"VERY_LONG\"")
}
fun addOutputs(dict: FlatDict): FlatDict {

    var predictedSalesTotal: Long = 0
    dict.forEach { row ->
        val buyout = row["buyout"] as JsonNode
        val bid = row["bid"]
        val timeLeft = (row["time_left"] as JsonNode).toString()

        if(timeLeft == AuctionTimeLeft.SHORT.jsonValue){
            predictedSalesTotal +=  buyout.longValue()
        } else if(timeLeft == AuctionTimeLeft.MEDIUM.name && Math.random() > .6){
            predictedSalesTotal +=  buyout.longValue()
        } else if(timeLeft == AuctionTimeLeft.LONG.name && Math.random() > .3){
            predictedSalesTotal +=  buyout.longValue()
        } else if(timeLeft == AuctionTimeLeft.VERY_LONG.name && Math.random() > .1){
            predictedSalesTotal +=  buyout.longValue()
        }
    }

    val bids = dict.filter { row ->
        val bid = row["bid"]
        bid == null
    }.size

    val outputs = mapOf<String, Any>(
        "output_predicted_sales_total" to predictedSalesTotal,
        "output_bid_ratio" to bids.toFloat() / dict.size.toFloat(),
    )

    val newList = dict.toMutableList()
    newList.add(outputs)
    return newList
}

val producer: KafkaProducer<String, String> by lazy {
    logger.info("Connecting to kafka cluster at $kafkaCluster")
    val consumerConfig = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaCluster,
        ProducerConfig.CLIENT_ID_CONFIG to kafkaTopic,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
    )
    KafkaProducer<String, String>(consumerConfig)
}

var lastRunModifiedIso: String? = null

fun parseBlizzardDateString(date: String): Long? {
    return try {
        val parsedDate = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("E, d MMM yyyy H:mm:ss z"))
        parsedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (t: Throwable) {
        logger.error("Couldn't parse blizzard's date to get epoch time $date", t)
        null
    }
}

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

    if (!auctions.isArray) {
        throw IllegalArgumentException("Unexpected data format from blizzard api.")
    }

    lastRunModifiedIso = lastModifiedIso
    logger.info("Got AH data updated at $lastRunModifiedIso")
    logger.info("Got ${auctions.size()} auctions")

    try {
        val parsedDate = LocalDateTime.parse(lastModifiedIso, DateTimeFormatter.ofPattern("E, d MMM yyyy H:mm:ss z"))
        val millis = parsedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        logger.warn("date $parsedDate is $millis")
    } catch (t: Throwable) {
        logger.error("huh", t)
        throw t
    }

    val batchId = UUID.randomUUID().toString()
    auctions.forEach { auction ->
        val auctionObject = auction as ObjectNode
        auctionObject.put("timestamp_iso", lastModifiedIso)
        auctionObject.put("timestamp", parseBlizzardDateString(lastModifiedIso))
        auctionObject.put("batch_id", batchId)
        auctionObject.put("server", "kiljaeden")
        auctionObject.put("server_id", 9)
    }

    return auctions
}

val defaultRetryPolicy: RetryPolicy<Throwable> = limitAttempts(5) + fullJitterBackoff(base = 10, max = 1000)
fun enqueueAuctionData() = runBlocking {
    val auctions = try {
        val auctions = retry(defaultRetryPolicy) {
            getAuctionHouseData()
        }

        if (auctions == null) {
            val record = ProducerRecord<String, String>("heartbeat", "1")
            producer.send(record)
            return@runBlocking
        }

        auctions
    } catch (ex: Throwable) {
        // Swallow errors here to avoid executorService cancelling the schedule
        logger.error(ex) { "Error pulling auction data" }
        return@runBlocking
    }

    val flatDict = try {
        toFlatDict(auctions)
    } catch (ex: Throwable) {
        // Swallow errors here to avoid executorService cancelling the schedule
        logger.error(ex) { "Error parsing data" }
        return@runBlocking
    }


    if (s3Bucket != null && s3CsvPrefix != null) {
        try {
            val csv = createCsv(flatDict)
            writeToS3(s3Bucket, s3CsvPrefix, csv)
        } catch (ex: Throwable) {
            // Swallow errors here to avoid executorService cancelling the schedule
            logger.error(ex) { "Error writing data to s3" }
        }
    }

    val dataAndOutputs = try {
        addOutputs(flatDict)
    } catch(t: Throwable){
        logger.error(t){ "Error computing the outputs" }
        flatDict
    }

    try {
        logger.info("Sending ${auctions.size()} auctions to kafka")
        dataAndOutputs.forEach { data ->
            val record = ProducerRecord<String, String>(kafkaTopic, mapper.writeValueAsString(data))
            producer.send(record)
        }
    } catch (ex: Throwable) {
        // Swallow errors here to avoid executorService cancelling the schedule
        logger.error(ex) { "Error sending data to kafka" }
    }
}

fun createCsv(data: FlatDict): String {
    val columns: MutableMap<String, Int> = LinkedHashMap()
    val rows: MutableList<Map<Int, Any>> = mutableListOf()

    data.forEach {
        val row = LinkedHashMap<Int, Any>()
        it.entries.forEach { (key, value) ->
            val valueIndex: Int = if (columns.containsKey(key)) {
                columns[key] ?: throw IllegalStateException("impossible")
            } else {
                val index = columns.size
                columns[key] = columns.size
                index
            }

            row[valueIndex] = value
        }

        rows.add(row)
    }


    val sb = StringBuilder()
    sb.appendLine(columns.keys.joinToString(","))
    rows.forEach { row ->
        val rowData = arrayOfNulls<Any?>(columns.keys.size)
        for (i in columns.keys.indices) {
            rowData[i] = row[i]
        }
        sb.appendLine(rowData.map { it ?: "" }.joinToString(","))
    }

    return sb.toString()
}

fun writeToS3(bucketName: String, prefix: String, content: String) {
    val fileName = Instant.now().toString()
    s3Client.putObject(bucketName, "$prefix/$fileName.csv", content)
}

private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
fun main() {
    executorService.scheduleWithFixedDelay(
        ::enqueueAuctionData,
        0,
        Duration.of(5, ChronoUnit.MINUTES).seconds,
        TimeUnit.SECONDS
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Closing producer")
        producer.close()
    })


    Javalin.create().start(8089)
}
