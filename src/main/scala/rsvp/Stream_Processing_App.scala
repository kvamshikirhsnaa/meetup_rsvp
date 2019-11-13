package rsvp

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

object Stream_Processing_App {
  def main(args: Array[String]): Unit = {

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

    val mongodb_host_name = "localhost"
    val mognodb_port_no = "27017"
    val mongodb_user_name = "admin"
    val mongodb_password = "admin"
    val mongodb_database_name = "meetup_rsvp_db"
    val mongodb_collection_name = "meetup_rsvp_message_detail_tbl"


    val spark = SparkSession.builder().
      master("local[*]").appName("sample").
      getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    import spark.implicits._

    val meetup_rsvp_df = spark.readStream.
      format("kafka").
      option("kafka.bootstrap.servers", kafka_bootstrap_servers ).
      option("subscribe", kafka_topic_name).
      option("startingOffsets", "latest").
      load()

    println("Printing Schema of meeup_rsvp_df")
    println(meetup_rsvp_df.printSchema())

    // Define custom schema for the message details
    val meetup_rsvp_message_schema = StructType(
      Array(StructField("venue", StructType(Array(
        StructField("venue_name", StringType),
        StructField("lon", DoubleType),
        StructField("lat", DoubleType),
        StructField("venue_id", LongType)
      ))),
        StructField("visibility", StringType),
        StructField("response", StringType),
        StructField("guests", LongType),
        StructField("member", StructType(Array(
          StructField("member_id", LongType),
          StructField("photo", StringType),
          StructField("member_name", StringType)
        ))),
        StructField("rsvp_id", LongType),
        StructField("mtime", LongType),
        StructField("event", StructType(Array(
          StructField("event_name", StringType),
          StructField("event_id", LongType),
          StructField("time", LongType),
          StructField("event_url", StringType)
        ))),
        StructField("group", StructType(Array(
          StructField("group_topics", ArrayType(StructType(Array(
            StructField("urlkey", StringType),
            StructField("topic_name", StringType)
          )), true)),
          StructField("group_city", StringType),
          StructField("group_country", StringType),
          StructField("group_id", LongType),
          StructField("group_name", StringType),
          StructField("group_lon", DoubleType),
          StructField("group_urlname", StringType),
          StructField("group_state", StringType),
          StructField("group_lat", DoubleType)
        )))
    ))

    val meetup_rsvp_df_1 = meetup_rsvp_df.select('value.cast(StringType), 'timestamp.cast(TimestampType))

    val meetup_rsvp_df_2 = meetup_rsvp_df_1.
      select(from_json('value, meetup_rsvp_message_schema) as "message_detail", 'timestamp)

    val meetup_rsvp_df_3 = meetup_rsvp_df_2.select($"message_detail.*", $"timestamp")

    val meetup_rsvp_df_4 = meetup_rsvp_df_3.select($"group.group_name", $"group.group_country",
    $"group.group_state", $"group.group_city", $"group.group_lat", $"group.group_lon", $"group.group_id",
    $"group.group_topics", $"member.meber_name", $"member.member_id", $"member.member_photo", $"venue.venue_name",
    $"venue.lon", $"venue.lat", $"venu.venue_id", $"visibility", $"response", $"guests", $"event.event_id",
    $"event.event_name", $"event.time", $"event.event_url")

    println("Printing Schema of meetup_rsvp_df_4")
    meetup_rsvp_df_4.printSchema()

    val spark_mongodb_output_uri = s"mongodb://${mongodb_user_name}:${mongodb_password}@${mongodb_host_name}:" +
        s"${mognodb_port_no}/${mongodb_database_name}.${mongodb_collection_name}"

    //Writing meetup_rsvp_df_4 into mongoDb collection

    meetup_rsvp_df_4.writeStream.
      trigger(Trigger.ProcessingTime("30 seconds")).
      outputMode("update").
      foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        val batchDF_1 = batchDF.withColumn( "batch_id", lit( batchId ) )

        //Transform batchDF and write it to sink/target/persistent storage
        //write data from spark dataframe to database

        batchDF_1.write.
          format( "mongo" ).
          option( "uri", spark_mongodb_output_uri ).
          option( "database", mongodb_database_name ).
          option( "collection", mongodb_collection_name ).
          save()
      }.start()


    // writing aggregated meetup rsvp dataframe into MySql Database table

    // simple aggregate - find response count by grouping group_name
    // group_country, group_state, group_city, group_lat, group_lon, response

    val meetup_rsvp_df_5 = meetup_rsvp_df_4.groupBy('group_name, 'group_country, 'group_state,
    'group_city, 'group_lat, 'group_lon, 'response).agg(count('response) as "response_count")

    println("Printing Schema of meetup_rsvp_df_5: ")
    meetup_rsvp_df_5.printSchema()

    // writing final result into console for debugging purpose
    val trans_details_write_stream = meetup_rsvp_df_5.writeStream.
      trigger(Trigger.ProcessingTime("30 seconds")).
      outputMode("update").
      option("truncate", "false").
      format("console").
      start()


    val mysql_properties = new java.util.Properties
    mysql_properties.setProperty("driver", mysql_driver_class)
    mysql_properties.setProperty("user", mysql_user_name)
    mysql_properties.setProperty("password", mysql_user_password)

    // Writing Aggregated meetup rsvp DataFrame into mysql Database table start here

    meetup_rsvp_df_5.writeStream.
      trigger(Trigger.ProcessingTime("30 seconds")).
      outputMode("update").
      foreachBatch{(batchDF: DataFrame, batchId: Long) =>
        val batchDF_1 = batchDF.withColumn("batch_id", lit(batchId))

        // Transform batchDF and write it to sink/target/persistent storage
        // write data from spark dataframe to mysql database
        batchDF_1.write.mode("append").
          jdbc(mysql_jdbc_url, mysql_table_name, mysql_properties)
      }.start()

    // Writing Aggregated meetup rsvp DataFrame into mysql Database table ends here


    trans_details_write_stream.awaitTermination()

    println("Stream Process Application Completed.")



  }

}
