import com.fasterxml.jackson.databind.ObjectMapper;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import modeles.TweetObject;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.*;
import org.json4s.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import utils.TwitterDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PopToMongo {
    private static final Logger logger = LoggerFactory.getLogger(PopToMongo.class);

    public static void main(String args[]){

        ObjectMapper objectMapper = new ObjectMapper();
        Mongo mongo = new Mongo("hadoop000",27017);
        DB db = mongo.getDB("DB_Twitter");
        DBCollection collection = db.getCollection("TwitterTable");


        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("PopToMongo");
        conf.set("spark.serializer", KryoSerializer.class.getName());
        JavaStreamingContext streamingContext = new JavaStreamingContext(conf, Durations.seconds(2));

        Map<String, Object> kafkaParams = new HashMap<String, Object>();
        kafkaParams.put("bootstrap.servers", "hadoop000:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "POPTOMONGO-Direct-Stream-PRO-v1");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);

        Collection<String> topics = Arrays.asList("twitterMessage");

        logger.info("Start Stream");
        JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(topics, kafkaParams)
                );

        JavaDStream<String> lines = stream.map(stringStringConsumerRecord -> stringStringConsumerRecord.value());

        lines.foreachRDD(rdd-> {
            System.out.println("--- New RDD with " + rdd.partitions().size()
                    + " partitions and " + rdd.count() + " records" );
            rdd.collect().forEach(ele -> {
                try {
                    TweetObject tweetObject = objectMapper.readValue(ele.getBytes(),TweetObject.class);
                    System.out.println("Name test-----------------"+tweetObject.displayName);
                    DBObject dbTwitter = (DBObject) JSON.parse(objectMapper.writeValueAsString(tweetObject));
                    dbTwitter.put("statusId",tweetObject.statusId);
                    dbTwitter.put("displayName",tweetObject.displayName);
                    dbTwitter.put("date",tweetObject.date);
                    dbTwitter.put("retweetCount",tweetObject.retweetCount);
                    dbTwitter.put("favoriteCount",tweetObject.favoriteCount);
                    dbTwitter.put("country",tweetObject.country);
                    dbTwitter.put("countryCode",tweetObject.countryCode);
                    dbTwitter.put("source",tweetObject.source);
                    dbTwitter.put("tweetText",tweetObject.tweetText);
                    collection.insert(dbTwitter);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("test------------------"+ele);
            });
        } );

        // Start the computation
        streamingContext.start();
        try {
            streamingContext.awaitTermination();
            mongo.close();
            logger.info("Stop the job");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
