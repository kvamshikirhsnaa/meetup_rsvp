package meetup

import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class Kafka_Producer_Demo {

  val kafka_topic_name = "meetuprsvptopic"
  val kafka_bootstrap_servers = "localhost:9092"

  println("Kafka Producer Application Started...")

  //meetup.com RSVP stream REST API endpoint
  val meetup_rsvp_stream_api_endpoint = "http://stream.meetup.com/2/rsvps"
  val url_object = new URL(meetup_rsvp_stream_api_endpoint)
  val connection_object = url_object.openConnection()

  val jsonfactoy_object = new JsonFactory(new ObjectMapper)
  val parser_object = jsonfactoy_object.createParser(connection_object.getInputStream)


  //set the kafka producer details in the properties object
  val properties_object = new Properties()
  properties_object.put("bootstrap_servers", kafka_bootstrap_servers)
  properties_object.put("acks", "all")
  properties_object.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties_object.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties_object.put("enable_auto.commit", "true")
  properties_object.put("auto.commit.interval.ms", "1000")
  properties_object.put("session.timeout.ms", "3000")

  val kafka_producer_object = new KafkaProducer[String, String](properties_object)



  def kafakProducer: Unit = {
    while (parser_object.nextToken() != null) {
      val message_record = parser_object.readValueAsTree().toString
      println( message_record )

      val producer_record_object = new ProducerRecord[String, String]( kafka_topic_name, message_record )
      kafka_producer_object.send( producer_record_object )
    }

    kafka_producer_object.close()
  }


  println("Kafka Producer Application Completed.")

}
