//package com.fjfalcon.youtube;
//
//import com.fjfalcon.config.YoutubeProperties;
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.auth.oauth2.StoredCredential;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
//import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
//import com.google.api.client.googleapis.json.GoogleJsonResponseException;
//import com.google.api.client.http.HttpRequest;
//import com.google.api.client.http.HttpRequestInitializer;
//import com.google.api.client.http.HttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.client.util.store.DataStore;
//import com.google.api.client.util.store.FileDataStoreFactory;
//import com.google.api.services.youtube.YouTube;
//import com.google.api.services.youtube.model.ChannelListResponse;
//import com.google.api.services.youtube.model.LiveBroadcast;
//import com.google.api.services.youtube.model.LiveBroadcastSnippet;
//import com.google.api.services.youtube.model.LiveBroadcastStatus;
//import com.google.api.services.youtube.model.LiveStream;
//import com.google.api.services.youtube.model.LiveStreamSnippet;
//import com.google.api.services.youtube.model.SearchListResponse;
//import com.google.api.services.youtube.model.SearchResult;
//import com.google.api.services.youtube.model.Video;
//import com.google.api.services.youtube.model.VideoListResponse;
//import com.google.common.collect.Lists;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.util.List;
//
//@Component
//public class YoutubeClient {
//    private final YouTube client;
//
//    private final YoutubeProperties youtubeProperties;
//
//    private boolean isStreamStarted;
//
//    private final HttpTransport httpTransport = new NetHttpTransport();
//    private final JsonFactory jsonFactory = new JacksonFactory();
//
//
//    public YoutubeClient(YoutubeProperties youtubeProperties) throws IOException {
//        this.youtubeProperties = youtubeProperties;
//        Credential credential = authorize();
//        this.client = new YouTube.Builder(httpTransport, jsonFactory, new HttpRequestInitializer() {
//            public void initialize(HttpRequest request) throws IOException {
//            }
//        }).setApplicationName("fj-stream-bot")
//                .build();
//
////        System.out.println(getVideos());
//
//        var z = client.liveStreams().list("fjfalcon");
//        createStream();
//    }
//
//    public static Credential authorize() throws IOException {
//        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
//
//        // Load client secrets.
//        Reader clientSecretReader = new InputStreamReader(Auth.class.getResourceAsStream("/client_secrets.json"));
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);
//
//        // Checks that the defaults have been replaced (Default = "Enter X here").
//        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
//                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
//            System.out.println(
//                    "Enter Client ID and Secret from https://console.developers.google.com/project/_/apiui/credential "
//                            + "into src/main/resources/client_secrets.json");
//            System.exit(1);
//        }
//
//        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
//        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
//        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);
//
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
//                .build();
//
//        // Build the local server and bind it to port 8080
//        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();
//
//        // Authorize.
//        return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
//    }
//
//    public void createStream() {
//        // Создайте описание трансляции.
//        LiveBroadcast broadcast = new LiveBroadcast();
//        LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
//        broadcastSnippet.setTitle("New Live Broadcast");
//        broadcast.setSnippet(broadcastSnippet);
//
//        LiveBroadcastStatus status = new LiveBroadcastStatus();
//        status.setPrivacyStatus("private");
//        broadcast.setStatus(status);
//
//// Создайте поток.
//        LiveStream stream = new LiveStream();
//        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
//        streamSnippet.setTitle("New Live Stream");
//        stream.setSnippet(streamSnippet);
//
//        try {
//            // Создайте трансляцию.
//            LiveBroadcast broadcastResponse = client.liveBroadcasts()
//                    .insert("snippet,status", broadcast)
//                    .execute();
//
//            // Создайте поток.
//            LiveStream streamResponse = client.liveStreams()
//                    .insert("snippet,cdn", stream)
//                    .execute();
//
//            // Свяжите трансляцию и поток.
//            client.liveBroadcasts().bind(broadcastResponse.getId(), "id,contentDetails")
//                    .setStreamId(streamResponse.getId())
//                    .execute();
//
//
//            // Трансляция успешно создана и связана с потоком. Теперь начинайте стримить ваше видео на полученный URL потока.
//        } catch (GoogleJsonResponseException e) {
//            // Отлавливайте и обрабатывайте ошибки Google JSON Response.
//            e.printStackTrace();
//        } catch (Throwable t) {
//            // Обработайте остальные возможные ошибки.
//            t.printStackTrace();
//        }
//    }
//    public String getVideos() {
//        try {
//
//
//            YouTube.Channels.List channelRequest = client.channels().list("contentDetails");
//            channelRequest.setMine(true);
//            channelRequest.setKey(youtubeProperties.getApiKey());
//            channelRequest.setFields("items/contentDetails,nextPageToken,pageInfo");
//            ChannelListResponse channelResult = channelRequest.execute();
//
//            channelResult.getItems().forEach(System.out::println);
//
//
//        var search = client.videos().list("id,snippet");
//        search.setKey(youtubeProperties.getApiKey());
//
////        search.setQ("fjfalcon Весны");
//        search.setMaxResults(5L);
//        VideoListResponse searchResponse = null;
//
//            searchResponse = search.execute();
//
//        List<Video> searchResultList = searchResponse.getItems();
//        StringBuilder response = new StringBuilder();
//        for (Video result : searchResultList) {
//            response.append(result);
//        }
//
//        return response.toString();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error";
//        }
//    }
//}
