import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class Main {
    private static final String TENANT_SHORT = "foobar";
    private static final String TENANT_LONG = "foobar.onmicrosoft.com";

    public static final String CLIENT_ID = "<id>";
    public static final String CLIENT_SECRET = "<secret>";

    private static final String COMPLETE_URL = "https://" + TENANT_SHORT + ".sharepoint.com/";

    private static final CloseableHttpClient client = HttpClientBuilder.create().build();

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectReader reader = mapper.reader();
    private static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    public static void main(String[] args) throws Exception {
        final String requestUrl = COMPLETE_URL + "_api/web/getchanges";
        final HttpPost request = new HttpPost(requestUrl);
        request.setEntity(new StringEntity(
            "{ 'query': " +
                "{ " +
                "'__metadata': { 'type': 'SP.ChangeQuery' }, " +
                "'Web': true, " +
                "'Update': true, " +
                "'Add': true " +
                "} " +
                "}"
        ));

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthToken());
        request.addHeader(HttpHeaders.ACCEPT, "application/json;odata=verbose");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=verbose");
        // request.addHeader("X-RequestDigest", fetchRequestDigest(requestUrl));


        final HttpResponse response = client.execute(request);
        System.out.println(response.getStatusLine().getStatusCode());
        System.out.println(response.getStatusLine().getReasonPhrase());
        Arrays.stream(response.getAllHeaders()).forEach(System.out::println);
    }

    private static String getAuthToken() throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", TENANT_LONG));

        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.addHeader("cache-control", "no-cache");

        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("client_id", CLIENT_ID));
        nvps.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));

        // works
        // nvps.add(new BasicNameValuePair("scope", "https://graph.microsoft.com/.default"));

        // tells me: invalid scope
        // nvps.add(new BasicNameValuePair("scope", COMPLETE_URL + "Sites.ReadWrite.All"));

        // tells me: The provided value for the input parameter 'scope' is not valid. The scope https://tenant.sharepoint.com/ is not valid.
        nvps.add(new BasicNameValuePair("scope", COMPLETE_URL));

        // tells me: The 'resource' request parameter is not supported.
        // nvps.add(new BasicNameValuePair("resource", COMPLETE_URL));
        nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));

        request.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

        HttpResponse response = client.execute(request);
        final JsonNode jsonNode = reader.readTree(response.getEntity().getContent());

        System.out.println(writer.writeValueAsString(jsonNode));

        String accessToken = jsonNode.get("access_token").textValue();

        return accessToken;
    }
}
