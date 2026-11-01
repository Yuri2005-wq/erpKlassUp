package org.erpklassup.erpklassup.sync;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * SyncApiClient — encapsule les deux appels HTTP du protocole de sync.
 * Timeouts courts + retry exponentiel gérés par l'appelant (SyncOrchestrator),
 * pas ici : ce client reste un simple transport, testable isolément.
 */
public class SyncApiClient {

    private final OkHttpClient http;
    private final String baseUrl;
    private final String ecoleKey;   // clé brute de l'école (jamais loguée)
    private final String idClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public SyncApiClient(String baseUrl, String ecoleKey, String idClient) {
        this.baseUrl = baseUrl;
        this.ecoleKey = ecoleKey;
        this.idClient = idClient;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .writeTimeout(Duration.ofSeconds(20))
                .build();
    }

    private Request.Builder authed(Request.Builder b) {
        return b.header("X-Ecole-Key", ecoleKey).header("X-Client-Id", idClient);
    }

    /** Pousse un lot d'entrées d'outbox. Renvoie le JSON de résultat par entrée. */
    public JsonNode push(List<PendingEntry> batch) throws IOException {
        ArrayNode entries = mapper.createArrayNode();
        for (PendingEntry e : batch) {
            ObjectNode n = mapper.createObjectNode();
            n.put("idFile", e.idFile());
            n.put("nomTable", e.nomTable());
            n.put("idEntity", e.idEntity());
            n.put("actionEnum", e.action());
            n.set("donneesJSON", e.donneesJSON());
            n.put("versionClient", e.versionClient());
            entries.add(n);
        }
        ObjectNode body = mapper.createObjectNode();
        body.set("entries", entries);

        Request req = authed(new Request.Builder())
                .url(baseUrl + "/api/sync/push/")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Push refusé : HTTP " + resp.code() + " — " + safeBody(resp));
            }
            return mapper.readTree(resp.body().string());
        }
    }

    /** Récupère les changements distants depuis lastSyncLogId (curseur local). */
    public JsonNode pull(long since, int limit) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/sync/pull/").newBuilder()
                .addQueryParameter("since", String.valueOf(since))
                .addQueryParameter("limit", String.valueOf(limit))
                .build();
        Request req = authed(new Request.Builder()).url(url).get().build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Pull refusé : HTTP " + resp.code() + " — " + safeBody(resp));
            }
            return mapper.readTree(resp.body().string());
        }
    }

    private String safeBody(Response r) {
        try { return r.body() != null ? r.body().string() : ""; }
        catch (IOException e) { return "<illisible>"; }
    }

    public record PendingEntry(
            String idFile, String nomTable, String idEntity,
            String action, JsonNode donneesJSON, long versionClient) {}
}
