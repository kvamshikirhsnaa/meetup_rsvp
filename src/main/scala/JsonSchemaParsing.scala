import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

object JsonSchemaParsing {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().
      master("local[*]").appName("samplenew").
      getOrCreate()

    import spark.implicits._
    spark.sparkContext.setLogLevel("ERROR")

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


    val cust_schema = new StructType().add("venue", new StructType().
      add("venue_name", StringType).add("lon", StringType).
      add("lat", StringType).add("venue_id", StringType)).
      add("visibility", StringType).
      add("response", StringType).
      add("guests", StringType).
      add("member", new StructType().add("member_id", StringType).
        add("photo", StringType).add("member_name", StringType)).
      add("rsvp_id", StringType).
      add("mtime", StringType).
      add("event", new StructType().add("event_name", StringType).
        add("event_id", StringType).add("time", StringType).
        add("evnt_url", StringType)).
      add("group", new StructType().add("group_topics", ArrayType(new StructType().
        add("urlkey", StringType).add("topic_name", StringType), true))).
      add("group_city", StringType).
      add("group_country", StringType).
      add("group_id", StringType).
      add("group_name", StringType).
      add("group_lon", StringType).
      add("group_urlname", StringType).
      add("group_state", StringType).
      add("group_lat", StringType)


    val df = spark.read.format("json").schema(meetup_rsvp_message_schema).
      load("C:\\Users\\Kenche.vamshikrishna\\Downloads\\inputfiles\\meetup.json")

    df.show
    df.printSchema()


    val df2 = spark.read.format("json").schema(cust_schema).
      load("C:\\Users\\Kenche.vamshikrishna\\Downloads\\inputfiles\\meetup.json")

    df2.show
    df2.printSchema()



    val df3 = spark.read.json("C:\\Users\\Kenche.vamshikrishna\\Downloads\\inputfiles\\meetup.json")

    df3.show
    df3.printSchema()

    val df4 = spark.read.json("C:\\Users\\Kenche.vamshikrishna\\Downloads\\inputfiles\\meetupnew.json")

    df4.show
    df4.printSchema()

  }

}
