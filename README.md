# noodleio

Noodle.io is a ramen themed [Slither.io](https://en.wikipedia.org/wiki/Slither.io) clone.


# Docker

`docker compose build` creates a docker container running the project.
> it also creates an up to date runnable jar

`docker compose up` starts the container
> if you have configured the .env variable, by copying your relevant .env.xyz file and renaming it ".env", it should be possible to run the game through a tool like VcXsrv X server on Windows, or some equivalent tool on macOS/Linux. Just remember to set the screen to 0.

## Why Docker?

With Docker we enure that all developers can run the project in the same environment.
*Todo: complete Docker documentation*
