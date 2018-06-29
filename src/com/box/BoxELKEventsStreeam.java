/**
 * 
 */
package com.box;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.box.Consts;
import com.box.sdk.BoxConfig;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxEvent;
import com.box.sdk.EventListener;
import com.box.sdk.EventLog;
import com.box.sdk.EventStream;
import com.box.sdk.IAccessTokenCache;
import com.box.sdk.InMemoryLRUAccessTokenCache;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * @author Jacques van der Merwe Example of Box and ELK (
 *
 */
public class BoxELKEventsStreeam {

	/**
	 * @param args
	 */

	private static BoxDeveloperEditionAPIConnection boxAPI;
	private static String boxAccessToken = "";
	static Properties appProps = new Properties();

	private static final int MAX_CACHE_ENTRIES = Consts.MAX_CACHE_ENTRIES;

	public static void main(String[] args) {
		// setupBox
		
		try {
			//String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
			String appConfigPath = "config/app.properties";
			 
			appProps = new Properties();
			appProps.load(new FileInputStream(appConfigPath));
			
			setupBox();
			startListeningUserEvents();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private static void startListeningUserEvents() {
		try {
			String nextStreamPosition = "";
			String elkUrl=appProps.getProperty("elkUrl");
			String url = "https://api.box.com/2.0/events?stream_type=admin_logs&limit=" + Consts.EVENT_STREAM_LIMIT;

			HttpResponse<JsonNode> response = Unirest.get(url)
					.header("Authorization", "Bearer " + boxAPI.getAccessToken()).header("Cache-Control", "no-cache")
					.asJson();

			JSONObject jso = response.getBody().getObject();
			int chunkSize = jso.getInt("chunk_size");
			nextStreamPosition = jso.getString("next_stream_position");
			JSONArray jsa = jso.getJSONArray("entries");
			while (jsa.length() > 0) {
				for (int i = 0; i < jsa.length(); i++) {
					System.out.println(jsa.get(i).toString());
					HttpResponse<String> elkResponse = Unirest.post(elkUrl)
							.header("Content-Type", "application/json").header("Cache-Control", "no-cache")
							.body(jsa.get(i).toString()).asString();
					System.out.println(elkResponse.getStatus());
				}
				response = Unirest.get(url + "&stream_position=" + nextStreamPosition)
						.header("Authorization", "Bearer " + boxAPI.getAccessToken())
						.header("Cache-Control", "no-cache").asJson();

				jso = response.getBody().getObject();
				chunkSize = jso.getInt("chunk_size");
				nextStreamPosition = jso.getString("next_stream_position");
				jsa = jso.getJSONArray("entries");
			}

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void setupBox() {
		{
			// It is a best practice to use an access token cache to prevent unneeded
			// requests to Box for access tokens.
			// For production applications it is recommended to use a distributed cache like
			// Memcached or Redis, and to
			// implement IAccessTokenCache to store and retrieve access tokens appropriately
			// for your environment.
			try {
				IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(MAX_CACHE_ENTRIES);

				Reader reader = new FileReader(Consts.APP_CONFIG_FILE);
				BoxConfig boxConfig = BoxConfig.readFrom(reader);

				boxAPI = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(boxConfig);
				boxAccessToken = boxAPI.getAccessToken();
				System.out.println(boxAccessToken);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
