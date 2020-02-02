package meetup

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

class Stream_Processing_App {
  import Kafka_Spark_Stream._


  println("Stream Processing Application Started...")

  val kafka_topic_name = "meetuprsvptopic"
  val kafka_bootstrap_servers = "localhost:9092"

  val mysql_host_name = "localhost"
  val mysql_port_no = "3306"
  val mysql_user_name = "root"
  val mysql_user_password = "Kenche@21"
  val mysql_database_name = "meetup_rsvp_db"
  val mysql_driver_class = "com.mysql.jdbc.Driver"
  val mysql_table_name = "meetup_rsvp_message_agg_details_tbl"
  val mysql_jdbc_url = s"jdbc:mysql://${mysql_host_name}:${mysql_port_no}/${mysql_database_name}"



  def meetupStream(spark: SparkSession):DataFrame = {
    spark.readStream.
      format( "kafka" ).
      option( "kafka.bootstrap.servers", kafka_bootstrap_servers ).
      option( "subscribe", kafka_topic_name ).
      option( "startingOffsets", "latest" ).
      load()
  }

/*
  println("Printing Schema of meeup_rsvp_df")
  println(meetup_rsvp_df.printSchema())
*/

  // Define custom schema for the message details
  val meetup_rsvp_message_schema = StructType(
    Array(StructField("venue", StructType(Array(
      StructField("venue_name", StringType),
      StructField("lon", StringType),
      StructField("lat", StringType),
      StructField("venue_id", StringType)
    ))),
      StructField("visibility", StringType),
      StructField("response", StringType),
      StructField("guests", StringType),
      StructField("member", StructType(Array(
        StructField("member_id", StringType),
        StructField("photo", StringType),
        StructField("member_name", StringType)
      ))),
      StructField("rsvp_id", StringType),
      StructField("mtime", StringType),
      StructField("event", StructType(Array(
        StructField("event_name", StringType),
        StructField("event_id", StringType),
        StructField("time", StringType),
        StructField("event_url", StringType)
      ))),
      StructField("group", StructType(Array(
        StructField("group_topics", ArrayType(StructType(Array(
          StructField("urlkey", StringType),
          StructField("topic_name", StringType)
        )), true)),
        StructField("group_city", StringType),
        StructField("group_country", StringType),
        StructField("group_id", StringType),
        StructField("group_name", StringType),
        StructField("group_lon", StringType),
        StructField("group_urlname", StringType),
        StructField("group_state", StringType),
        StructField("group_lat", StringType)
      )))
    ))



  def meetupStream(spark: SparkSession, meetup_rsvp_df: DataFrame): DataFrame = {
    import spark.implicits._

    val df = meetup_rsvp_df.select( 'value.cast( StringType ), 'timestamp.cast( TimestampType ) )

    val df2 = df.select(from_json('value, meetup_rsvp_message_schema) as "message_detail", 'timestamp)

    val df3 = df2.select($"message_detail.*", $"timestamp")

    val df4 = df3.select($"group.group_name", $"group.group_country",
      $"group.group_state", $"group.group_city", $"group.group_lat", $"group.group_lon", $"group.group_id",
      $"group.group_topics", $"member.member_name", $"member.member_id", $"member.photo", $"venue.venue_name",
      $"venue.lon", $"venue.lat", $"venue.venue_id", $"visibility", $"response", $"guests", $"event.event_id",
      $"event.event_name", $"event.time", $"event.event_url")

    val df5 = df4.groupBy('group_name, 'group_country, 'group_state,
      'group_city, 'group_lat, 'group_lon, 'response).agg(count('response) as "response_count")

    df5
  }


  // writing final result into console for debugging purpose

  def meetupToConsole(spark: SparkSession, meetupDF: DataFrame) = {
    meetupDF.writeStream.
      trigger( Trigger.ProcessingTime( "30 seconds" ) ).
      outputMode( "update" ).
      option( "truncate", "false" ).
      format( "console" ).
      start()
  }.awaitTermination()

  val mysql_properties = new java.util.Properties
  mysql_properties.setProperty("driver", mysql_driver_class)
  mysql_properties.setProperty("user", mysql_user_name)
  mysql_properties.setProperty("password", mysql_user_password)

  // Writing Aggregated meetup rsvp DataFrame into mysql Database table start here


  def writingToMysql(spark: SparkSession, meetupDF: DataFrame) = {

    meetupDF.writeStream.
      trigger(Trigger.ProcessingTime("30 seconds")).
      outputMode("update").
      foreachBatch{(batchDF: DataFrame, batchId: Long) =>
        val batchDF_1 = batchDF.withColumn("batch_id", lit(batchId))

        // Transform batchDF and write it to sink/target/persistent storage
        // write data from spark dataframe to mysql database
        batchDF_1.write.mode("append").
          jdbc(mysql_jdbc_url, mysql_table_name, mysql_properties)
      }.start()


  }



}


