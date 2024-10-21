# Aggregation Server

This project contains the implementation of an AggregationServer in Java, along with unit tests using JUnit and Mockito.

## Project Structure
```
Assignment2
├── lib  
│   ├── objenesis-3.3.jar      
│   ├── mockito-core-5.10.0.jar      
│   ├── byte-buddy-1.14.11.jar      
│   └── byte-buddy-agent-1.14.11.jar
├── data
│   ├── emptyFile.txt
│   ├── simpleData.txt
│   ├── weather_data.txt
│   ├── invalidInputDataNoID.txt
│   ├── invalidInputBadJSON.txt
│   ├── inputData.txt     
│   └── inputData2.txt
├── AggregationServer.java  
├── AggregationServerTest.java
├── ContentServer.java  
├── ContentServerTest.java
├── GETClient.java  
├── GETClientTest.java
├── DataFileHandler.java  
├── DataFileHandlerTest.java
├── LamportClock.java  
├── LamportClockTest.java
├── JSONParser.java  
└── JSONParserTest.java
```
## To compile and test the server components:
   ```bash
     javac AggregationServer.java ContentServer.java GETClient.java DataFileHandler.java LamportClock.java JSONParser.java
   ```
### To start the AggregationServer:
   ```bash
     java AggregationServer <port number>
   ```
#### Example:
   ```bash
     java AggregationServer 4567
   ```
### To run the ContentServer:
   ```bash
     java ContentServer <servername:portnumber> <inputFilePath> 
   ```
#### Example:
   ```bash
     java ContentServer localhost:4567 data/inputData.txt
   ```
### To run the GETClient:
   ```bash
     java GETClient <servername:portnumber>
   ```
#### Example:
   ```bash
     java GETClient localhost:4567
   ```

### Notes regarding the data file:

The `weather_data.txt` will be the main storage file.

Files that used as input for ContentServer are: `inputData.txt` and `inputData2.txt`

## To run the test files

> #### ⚠️ NOTE:
> **Before running `AggregationServerTest`, the Aggregation server must be started.**

Test the server with Junit5 using IntelliJ IDEA:

- `Mockito` is used to simulate the connections between the components so it is required as well.
- Specific version of mockito: `mockito-core-5.10.10.jar`.
- Some modules that mockito is dependent on: `objenesis-3.3.jar` `byte-buddy-1.14.11.jar` `byte-buddy-agent-1.14.11.jar`.
- Using Intellij to download `mockito-core-5.10.10.jar` from mvn libraries should be sufficient.
- Download jar files necessary for Junit5.
- Add the jar files to the project’s classpath.
- Run the test file with the run option in sidebar on the left of IntelliJ.

- This Aggregation server has undergone extensive testing both manually and automatically.
- Manual testing involves running the server on the command line and open another terminal tabs to start multiple clients.
- Automated testing involves running tests written with Junit5.




