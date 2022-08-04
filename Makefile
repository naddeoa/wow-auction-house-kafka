JAR_PATH=./app/build/libs/app-standalone.jar
SRC=$(shell find ./ -name "*.kt") ./app/build.gradle.kts

.PHONY:jar run

jar:$(JAR_PATH)

$(JAR_PATH):$(SRC)
	./gradlew build
	@echo "jar build $(JAR_PATH)"

run:
	API_CLIENT_ID=$(API_CLIENT_ID) API_CLIENT_SECRET=$(API_CLIENT_SECRET) java -jar $(JAR_PATH) 
