Small service that polls the blizzard auction house api and deposits data into a kafka topic. Created as part of a blog post for whylogs.

[Docker hub link](https://hub.docker.com/r/naddeoa/wow-ah-data-poller)

# Running

## Docker

```bash
make build-docker
make run-docker
```

You'll need a `conf.env` file with some Blizzard api credentials that you can get
for free at https://develop.battle.net/access/clients.

```bash
API_CLIENT_ID=xxxx
API_CLIENT_SECRET=xxxx
```

## Locally

```
make jar run
```

This will depend on the content of `conf.env` being set in your terminal
environment.


## Running everything locally

This lets you replicate the blog post that links to this repo. It will run a
local Kafka cluster, local whylogs container to consume from the cluster and
upload profiles to whylabs, and a local server that sends Blizzard auction house
data into your local Kafka cluster.

First, create a `compose.env` file with the following

```bash
# Can access via http://localhost:2023/swagger-ui#/whylogs/writeProfiles to trigger profile writes
WHYLOGS_CONTAINER_PORT=2023

ZOOKEEPER_PORT=2181

# Can exec docker commands using docker compose exec. For example,
# $ docker compose --env-file compose.env exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh \
#          --create \
#          --bootstrap-server localhost:9092 \
#          --replication-factor 1 \
#          --partitions 10 \
#          --topic wow-ah
INTERNAL_KAFKA_PORT=9092


# Can use local kafka tools on localhost:9093
EXTERNAL_KAFKA_PORT=9093


## Blizzard data poller env
# Get the api/secret from Blizzard following the instrucctions in the Docker section
API_CLIENT_ID=TODO
API_CLIENT_SECRET=TODO
CONTAINER_KEY=password # can leave this

## whylabs/whylogs config
# You can get these from your WhyLabs account for free after you create a model
# and visit your settings
MODEL_ID=TODO
ORG_ID=TODO
WHYLABS_API_KEY=TODO
```

Fill in the missing items marked with `TODO` and then run `make stack`, which
will run [docker compose](https://docs.docker.com/compose/install/) using
`compose.env` to fill in variables.

### API Keys

You can get valid Blizzard API credentials at
https://docs.docker.com/compose/install/ for free. You can get a WhyLabs account
and access token for free at https://hub.whylabsapp.com/.
