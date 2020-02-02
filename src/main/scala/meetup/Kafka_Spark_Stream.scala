package meetup

import org.apache.spark.sql.SparkSession

object Kafka_Spark_Stream {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().
      appName("sample").master("local").
      getOrCreate()


    val prod = new Kafka_Producer_Demo

    prod.kafakProducer

    val sparkStreaming = new Stream_Processing_App

    val meetup_rsvp_consume = sparkStreaming.meetupStream(spark)

    val meetup_rsvp_transform = sparkStreaming.meetupStream(spark, meetup_rsvp_consume )

    // writing to console
   sparkStreaming.meetupToConsole(spark, meetup_rsvp_transform)


    // writing to mysql
    sparkStreaming.writingToMysql(spark, meetup_rsvp_transform)





  }
}