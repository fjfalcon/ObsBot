package com.fjfalcon.youtube;

import com.fjfalcon.config.YoutubeProperties;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

//@Component
public class YoutubeClient {
    private final YouTube client;

    private final YoutubeProperties youtubeProperties;

    public YoutubeClient(YoutubeProperties youtubeProperties) throws IOException {
        this.youtubeProperties = youtubeProperties;
        this.client = new YouTube.Builder(new NetHttpTransport(),  new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("fj-stream-bot").build();

        System.out.println(getVideos());
    }

    public String getVideos() {
        try {


            YouTube.Channels.List channelRequest = client.channels().list("contentDetails");
            channelRequest.setMine(true);
            channelRequest.setKey(youtubeProperties.getApiKey());
            channelRequest.setFields("items/contentDetails,nextPageToken,pageInfo");
            ChannelListResponse channelResult = channelRequest.execute();

            channelResult.getItems().forEach(System.out::println);


        var search = client.videos().list("id,snippet");
        search.setKey(youtubeProperties.getApiKey());

//        search.setQ("fjfalcon Весны");
        search.setMaxResults(5L);
        VideoListResponse searchResponse = null;

            searchResponse = search.execute();

        List<Video> searchResultList = searchResponse.getItems();
        StringBuilder response = new StringBuilder();
        for (Video result : searchResultList) {
            response.append(result);
        }

        return response.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
