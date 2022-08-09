JAR_PATH=./app/build/libs/app-standalone.jar
SRC=$(shell find ./ -name "*.kt") ./app/build.gradle.kts
TAG=naddeoa/wow-ah-data-poller:latest

.PHONY:jar run build-docker run-docker push-docker stack

jar:$(JAR_PATH)

$(JAR_PATH):$(SRC)
	./gradlew build
	@echo "jar build $(JAR_PATH)"

run:
	API_CLIENT_ID=$(API_CLIENT_ID) API_CLIENT_SECRET=$(API_CLIENT_SECRET) java -jar $(JAR_PATH) 

build-docker:$(JAR_PATH)
	docker build . -t $(TAG)

run-docker:
	docker run -p --net=host --env-file conf.env $(TAG)

push-docker:
	docker push $(TAG)

stack:
	docker compose --env-file compose.env down
	docker compose --env-file compose.env up --remove-orphans
