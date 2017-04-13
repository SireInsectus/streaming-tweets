# streaming-tweets

TwitStreamer is a application used to help demonstrate Apache Spark's Streaming & Structured Streaming APIs

It uses Twitter4J to connect to Twitter's Firehose of public, filtered, tweets and convert those messages into a TCP-IP stream.

## Setup Instructions
1. Got https://apps.twitter.com/ and setup a new application
2. In setting up your app, you need four peices of information.
  * **Consumer Key (API Key)**
  * **Consumer Secret (API Secret)**
  * **Access Token**
  * **Access Token Secret**
  
3. To build, use the gradle targets **clean**, **build** and **aws**. The **aws** target creates the Buildfile and Procfile necessary to run on Elastic Beanstalk.
4. The zip file located in /streaming-tweets-server/build/distributions/streaming-tweets-server-x.x.x.zip can be uploaded directly to Elastic Beanstalk or unziped and ran locally.
5. Before running you need to setup four environment variables (either on your local PC or in the ElasticBeanstalk configuration). The four variables are listed below and are specified by your Twitter Application settings (step #2 above).

* **twitter4j_oauth_consumerKey**
* **twitter4j_oauth_consumerSecret**
* **twitter4j_oauth_accessToken**
* **twitter4j_oauth_accessTokenSecret**

6. To start the application (locally only), extract the zip file and run either 

* /bin/streaming-tweets-server (mac/linux)
* /bin/streaming-tweets-server.bat (windows)
