package com.fjfalcon.youtube;

import com.fjfalcon.obs.ObsController;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.LiveStreamStatus;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class YoutubeClient {
    private final Logger logger = LoggerFactory.getLogger(YoutubeClient.class);

    private final YouTube client;

    private boolean streamStarted;

    private final HttpTransport httpTransport = new NetHttpTransport();
    private final JsonFactory jsonFactory = new GsonFactory();

    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

    private final ObsController obsController;

    public YoutubeClient(ObsController obsController) throws IOException {
        this.obsController = obsController;
        this.client = new YouTube.Builder(httpTransport, jsonFactory, authorize()).setApplicationName("fj-stream-bot")
                .build();
    }

    public Credential authorize() throws IOException {
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube", "https://www.googleapis.com/auth/youtube.force-ssl");

        Reader clientSecretReader = new InputStreamReader(YoutubeClient.class.getResourceAsStream("/client_secrets.json"));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, clientSecretReader);

        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore("fj_stream_bot");

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, scopes).setCredentialDataStore(datastore)
                .build();

        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
    }

    public void createStream() {
        logger.info("Запуск стрима начат");
        LiveBroadcast broadcast = new LiveBroadcast();
        LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
        String title = "Онлайн игра Мафия " + LocalDate.now();
        broadcastSnippet.setTitle(title);
        broadcast.setSnippet(broadcastSnippet);
        broadcastSnippet.setScheduledStartTime(new DateTime(LocalDateTime.now().minusHours(3).toString()));
        broadcastSnippet.setScheduledEndTime(new DateTime(LocalDateTime.now().minusHours(3).plusHours(5).toString()));
        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("public");
        status.setSelfDeclaredMadeForKids(false);
        broadcast.setStatus(status);
        LiveBroadcastContentDetails contentDetails =new LiveBroadcastContentDetails();
        contentDetails.set("enableMonitorStream", false);
        broadcast.setContentDetails(contentDetails);

        LiveStream stream = new LiveStream();
        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
        streamSnippet.setTitle(title);
        stream.setSnippet(streamSnippet);

        CdnSettings cdnSettings = new CdnSettings();
        cdnSettings.setFormat("1080p");
        cdnSettings.setIngestionType("rtmp");
        cdnSettings.setResolution("1080p");
        cdnSettings.setFrameRate("60fps");
        stream.setCdn(cdnSettings);

        try {
            LiveBroadcast broadcastResponse = client.liveBroadcasts()
                    .insert("snippet,status, contentDetails", broadcast)
                    .execute();

            LiveStream streamResponse = client.liveStreams()
                    .insert("snippet,cdn", stream)
                    .execute();

            var key = streamResponse.getCdn().getIngestionInfo().getStreamName();
            obsController.setStreamKey(key);
            obsController.startStreaming();

            Thread.sleep(10000L);

            var id = client.liveBroadcasts().bind(broadcastResponse.getId(), "id,contentDetails")
                    .setStreamId(streamResponse.getId())
                    .execute();

            var response = client.liveBroadcasts().transition("testing", id.getId(),  "id, snippet, contentDetails").execute();

            Thread.sleep(60000L);

            response = client.liveBroadcasts().transition("live", id.getId(),  "id, snippet, contentDetails").execute();

            logger.info("Stream started with id {}", id.getId());
            streamStarted = true;
        } catch (GoogleJsonResponseException e) {
            logger.error("Google json response exception", e);
        } catch (Throwable t) {
            logger.error("Some exception in youtube client", t);
        }
        logger.info("Запуск стрима закончен");
    }

    public void checkStreamStarted() {
        if (!streamStarted) {
            createStream();
            streamStarted = true;
        }
    }

    public void stopStreaming() {
        String id = findLiveBroadcast();
        if (!id.equals("-1")) {
            try {
                var response = client.liveBroadcasts().transition("complete", id,  "id, snippet, contentDetails").execute();
                logger.info("{}", response);
            } catch (IOException e) {
                logger.error("Google json response exception", e);
            }
        }
        streamStarted = false;
    }
    public String findLiveBroadcast() {
        try {
            var broadcastResponse = client.liveBroadcasts()
                    .list("id,status").setMine(true).execute();

            return broadcastResponse.getItems().stream().filter( it -> it.getStatus().getLifeCycleStatus().equals("live")).map(
                    LiveBroadcast::getId).findAny().orElse("-1");
        } catch (IOException e) {
            logger.error("Failed to find live broadcasts", e);
            return "-1";
        }
    }

    public String listStream() {
        YouTube.LiveStreams.List livestreamRequest = null;
        try {
            livestreamRequest = client.liveStreams().list("id,snippet,cdn,status");
            livestreamRequest.setMine(true);
            LiveStreamListResponse returnedListResponse = livestreamRequest.execute();
            List<LiveStream> returnedList = returnedListResponse.getItems();

            StringBuilder z = new StringBuilder();

            z.append("\n================== Returned Streams ==================\n");
            for (LiveStream stream : returnedList) {
                z.append("  - Id: ").append(stream.getId())
                        .append("  - Status: ").append(stream.getStatus().getStreamStatus())
                        .append("  - Title: ").append(stream.getSnippet().getTitle())
                        .append("  - Description: ").append(stream.getSnippet().getDescription())
                        .append("  - Published At: ").append(stream.getSnippet().getPublishedAt())
                        .append("\n-------------------------------------------------------------\n");
            }
            return z.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String listBroadcasts() {
        try {
            YouTube.LiveBroadcasts.List liveBroadcastRequest =
                    client.liveBroadcasts().list("id,snippet, status");

            // Indicate that the API response should not filter broadcasts
            // based on their type or status.
            liveBroadcastRequest.setBroadcastType("all").setBroadcastStatus("all");

            // Execute the API request and return the list of broadcasts.
            LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();
            List<LiveBroadcast> returnedList = returnedListResponse.getItems();
            StringBuilder builder = new StringBuilder();

            // Print information from the API response.
            builder.append("\n================== Returned Broadcasts ==================\n");
            for (LiveBroadcast broadcast : returnedList) {
                builder.append("  - Id: ").append(broadcast.getId())
                        .append("  - Status: ").append(broadcast.getStatus().getLifeCycleStatus())
                        .append("  - Title: ").append(broadcast.getSnippet().getTitle()).append(
                                "  - Description: ").append(
                                broadcast.getSnippet().getDescription()).append(
                                "  - Published At: ").append(broadcast.getSnippet().getPublishedAt()).append(
                                "  - Scheduled Start Time: ").append(broadcast.getSnippet().getScheduledStartTime()).append(
                                "  - Scheduled End Time: ").append(broadcast.getSnippet().getScheduledEndTime())
                        .append("\n-------------------------------------------------------------\n");
            }

            return builder.toString();
        }
         catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void disableStream() {
        streamStarted = false;
    }
}


