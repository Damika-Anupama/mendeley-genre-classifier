#!/bin/bash
echo "Building project with shadowJar..."
./gradlew clean shadowJar

if [ $? -ne 0 ]; then
    echo "Build failed. Exiting."
    exit 1
fi

echo "Launching web server..."
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
     --add-opens java.base/java.nio=ALL-UNNAMED \
     -Dmodel.path=app/model \
     -jar app/build/libs/mendeley-web-all.jar &

# Wait for the server to start
echo "Waiting for the server to start..."
while ! nc -z localhost 8080; do
    sleep 1
done

# Open the browser once the server is ready
echo "Server is ready. Opening browser..."
open http://localhost:8080