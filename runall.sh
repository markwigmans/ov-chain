java -Dserver.port=8090 -jar backend/target/backend.jar &
java -Dserver.port=8190 -jar client/target/client.jar &
java -Dserver.port=8290 -jar feed/target/feed.jar &
