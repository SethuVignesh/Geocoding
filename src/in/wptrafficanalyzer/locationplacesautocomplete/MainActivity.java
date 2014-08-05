package in.wptrafficanalyzer.locationplacesautocomplete;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MainActivity extends Activity {

	AutoCompleteTextView atvPlaces;
	PlacesTask placesTask;
	ParserTask parserTask;
	GetLatLong getLatLng;
	Button btnSearch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

			btnSearch = (Button) findViewById(R.id.btnSearch);
			btnSearch.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Toast.makeText(getApplicationContext(),
							atvPlaces.getText().toString(), 0).show();
					
					
					//GEOCODING VIA GOOGLE MAP
					getLatLng = new GetLatLong();
					getLatLng.execute(atvPlaces.getText().toString());
					
					
					//GEOCODING VIA ANDROID LOCATION
					 List coordinates = getLatLngInGeoCode(atvPlaces.getText()
					 .toString());
					 if (coordinates != null & !coordinates.isEmpty()) {
					 Toast.makeText(getApplicationContext(),
					 (CharSequence) coordinates, 0).show();
					 }
				}
			});
			
			
			//AUTOCOMPLETE TEXT VIEW
			atvPlaces = (AutoCompleteTextView) findViewById(R.id.atv_places);
			atvPlaces.setThreshold(1);

			atvPlaces.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					
					//GOOGLE PLACES API
					placesTask = new PlacesTask();
					placesTask.execute(s.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					
				}

				@Override
				public void afterTextChanged(Editable s) {
					
				}
			});
		} catch (Exception e) {
		}
	}

	/** A method to download json data from url */
	private String downloadUrl(String strUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(strUrl);

			// Creating an http connection to communicate with url
			urlConnection = (HttpURLConnection) url.openConnection();

			// Connecting to url
			urlConnection.connect();

			// Reading data from url
			iStream = urlConnection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					iStream));

			StringBuffer sb = new StringBuffer();

			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			data = sb.toString();

			br.close();

		} catch (Exception e) {
			Log.d("Exception while downloading url", e.toString());
		} finally {
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}

	// Fetches all places from GooglePlaces AutoComplete Web Service
	private class PlacesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... place) {
			// For storing data from web service
			String data = "";

			// Obtain browser key from https://code.google.com/apis/console
			String key = "key=AIzaSyDs1SibAHORxgrR39lDDDJsE1xH87EOWYI";

			String input = "";

			try {
				input = "input=" + URLEncoder.encode(place[0], "utf-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			// place type to be searched
			String types = "types=geocode";

			// Sensor enabled
			String sensor = "sensor=false";

			// Building the parameters to the web service
			String parameters = input + "&" + types + "&" + sensor + "&" + key;

			// Output format
			String output = "json";

			// Building the url to the web service
			String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"
					+ output + "?" + parameters;

			try {
				// Fetching the data from web service in background
				data = downloadUrl(url);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			// Creating ParserTask
			parserTask = new ParserTask();

			// Starting Parsing the JSON string returned by Web Service
			parserTask.execute(result);
		}
	}

	/** A class to parse the Google Places in JSON format */
	private class ParserTask extends
			AsyncTask<String, Integer, List<HashMap<String, String>>> {

		JSONObject jObject;

		@Override
		protected List<HashMap<String, String>> doInBackground(
				String... jsonData) {

			List<HashMap<String, String>> places = null;

			PlaceJSONParser placeJsonParser = new PlaceJSONParser();

			try {
				jObject = new JSONObject(jsonData[0]);

				// Getting the parsed data as a List construct
				places = placeJsonParser.parse(jObject);

			} catch (Exception e) {
				Log.d("Exception", e.toString());
			}
			return places;
		}

		@Override
		protected void onPostExecute(List<HashMap<String, String>> result) {

			String[] from = new String[] { "description" };
			int[] to = new int[] { android.R.id.text1 };

			// Creating a SimpleAdapter for the AutoCompleteTextView
			SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result,
					android.R.layout.simple_list_item_1, from, to);

			// Setting the adapter
			atvPlaces.setAdapter(adapter);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}


	//GEOCODER WITH GOOGLE MAP

	private class GetLatLong extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... place) {
			// For storing data from web service
			String uri = "http://maps.google.com/maps/api/geocode/json?address="
					+ place[0] + "&sensor=false";

			HttpGet httpGet = new HttpGet(uri);
			HttpClient client = new DefaultHttpClient();
			HttpResponse response;
			StringBuilder stringBuilder = new StringBuilder();
			String latlng = null;
			try {
				response = client.execute(httpGet);
				HttpEntity entity = response.getEntity();
				InputStream stream = entity.getContent();
				int b;
				while ((b = stream.read()) != -1) {
					stringBuilder.append((char) b);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject = new JSONObject(stringBuilder.toString());
				// "status" : "ZERO_RESULTS"
				String status = jsonObject.getString("results").trim();
				if (status.equals("OK")) {
					double lng = ((JSONArray) jsonObject.get("results"))
							.getJSONObject(0).getJSONObject("geometry")
							.getJSONObject("location").getDouble("lng");

					double lat = ((JSONArray) jsonObject.get("results"))
							.getJSONObject(0).getJSONObject("geometry")
							.getJSONObject("location").getDouble("lat");

					Log.d("latitude", "" + lat);
					Log.d("longitude", "" + lng);
					latlng = Double.toString(lng) + ":"+Double.toString(lat);
				} else {
					Toast.makeText(getApplicationContext(),
							"Data not available", 0).show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Try after some time",
						0).show();
			}
			return latlng;

		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, 0).show();
			super.onPostExecute(result);

		}
	}

	//GEOCODER WITH ANDROID LOCATION
	public List getLatLngInGeoCode(String strAddress) {
		Geocoder coder = new Geocoder(this);
		List<Address> address = null;

		try {
			address = coder.getFromLocationName(strAddress, 5);

			// Toast.makeText(getApplicationContext(), , 0).show();
			if (address.isEmpty()) {
				Toast.makeText(getApplicationContext(), "data not available", 0)
						.show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!address.isEmpty()) {
			Address location = address.get(0);
			double lat = location.getLatitude();
			double lng = location.getLongitude();
			// String latLng = Double.toString(lng) + Double.toString(lat);
			// Toast.makeText(getApplicationContext(), latLng, 0).show();
			// GeoPoint p1 = new GeoPoint((int) (location.getLatitude() * 1E6),
			// (int) (location.getLongitude() * 1E6));
			//
			// return p1;
			List ltlng = new ArrayList();
			ltlng.add(lat);
			ltlng.add(lng);
			return ltlng;

		}
		return null;
	}
}