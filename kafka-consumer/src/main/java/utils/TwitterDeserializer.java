package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import modeles.TweetObject;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

public class TwitterDeserializer implements Deserializer<TweetObject>{

/*    public static TweetObject deserializeTwitter(String twitterJson) throws ParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        TweetObject tweetObject = null;
        try{
            tweetObject = mapper.readValue(twitterJson, TweetObject.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return tweetObject;
    }*/



     private ObjectMapper objectMapper;



    @Override
    public void configure(Map map, boolean b) {
        objectMapper = new ObjectMapper();
    }

    @Override
    public TweetObject deserialize(String s, byte[] bytes) {
        TweetObject tweetObject = null;

        try {
            tweetObject = objectMapper.readValue(bytes,TweetObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tweetObject ;
    }

    @Override
    public void close() {
        objectMapper = null;
    }

}