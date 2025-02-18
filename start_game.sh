#!/bin/bash
# Start the game
./gradlew lwjgl3:run

# Keep the container alive after the game process exits (if it does)
tail -f /dev/null
