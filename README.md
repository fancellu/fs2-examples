# FS2 examples

A few fs2 examples

## Converter1

Converts from Fahrenheit file to stdout in Celsius

- Parses multiple input files
- Handles empty lines, comments, non doubles, missing files
- Formats to 2 decimal places
- Prepends with a header line

## WindowedAverage

- Emits a windowed running average of the last 5 values

## QueueExample

- Creates stream that emits incrementing int every 100ms to a queue
- Creates stream that reads int from queue and adds int to ref
- Runs both at the same time, terminating after 1 second, printing out the ref sum

## EchoServer

- Listens for telnet to port 5555, 2 concurrent connections
- Echos commands back to user
- "EXIT" will say "Byebye!" and terminate their connection
- "KILLSERVER" will terminate the server

## REPL

- A simple REPL example
- Echos in the input from stdin
- Exits loop upon "KILLSERVER" or EOF signal 