package ca.ubc.cpsc210.nextbus.translink;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import ca.ubc.cpsc210.exception.ConnectionException;
import ca.ubc.cpsc210.exception.TranslinkException;
import ca.ubc.cpsc210.nextbus.model.BusStop;

/**
 * Wrapper around a service which gets real time bus information from Translink.
 */
public class TranslinkService extends AbstractTranslinkService {
	/**
	 * Set timeouts on connection and data acquisition from Translink
	 */
	private static int CONNECT_TIMEOUT = 3000;
	private static int READ_TIMEOUT = 3000;
	
	/**
	 * Translink API key - must be included with any request for data from Translink service
	 */
	private final static String APIKEY = "SRSloq7HLc6NlOaxwhZu";  // Replace with your own Translink API key
	
	/**
	 * For filtering LogCat messages
	 */
	private final static String LOG_TAG = "TranslinkService";

	/**
	 * Associated Android activity
	 */
	private Activity activity;

	public TranslinkService(Activity activity) {
		this.activity = activity;
	}
	
	/* (non-Javadoc)
     * @see ca.ubc.cpsc210.nextbus.translink.ITranslinkService#getBusStop(java.lang.String)
     */
	@Override
    public BusStop getBusStop(String stopNum) throws TranslinkException {
		StringBuilder uriBuilder = new StringBuilder(
				"//api.translink.ca/RTTIAPI/V1/");
		uriBuilder.append("stops/" + stopNum + "?");
		uriBuilder.append("apikey=" + APIKEY);

		try {
			String responseBody = makeJSONQuery(uriBuilder);
			return parseBusStopFromJSON(responseBody);
		} catch (Exception e) {
			// Log it, since we will lose some exception details in the
			// conversion to TranslinkException.
			Log.e(LOG_TAG, e.getMessage());

			// Convert other exception types to TranslinkException so clients do not
			// have to worry about the different possibilities.
			throw new TranslinkException(e.getMessage());
		}
	}
	
	
	/* (non-Javadoc)
     * @see ca.ubc.cpsc210.nextbus.translink.ITranslinkService#addWaitTimeEstimatesToStop(ca.ubc.cpsc210.nextbus.model.BusStop)
     */
	// By Shaylene
	@Override
    public void addWaitTimeEstimatesToStop(BusStop stop) throws TranslinkException {
		String stopNum = Integer.toString(stop.getStopNum());
		StringBuilder uriBuilder = new StringBuilder(
				"//api.translink.ca/RTTIAPI/V1/");
		uriBuilder.append("stops/" + stopNum + "/estimates?");
		uriBuilder.append("apikey=" + APIKEY);
		
		try {
			String responseBody = makeJSONQuery(uriBuilder);
			parseWaitTimesFromJSON(responseBody, stop);
		} catch (Exception e) {
			// Log it, since we will lose some exception details in the
			// conversion to TranslinkException.
			Log.e(LOG_TAG, e.getMessage());

			// Convert other exception types to TranslinkException so clients do not
			// have to worry about the different possibilities.
			throw new TranslinkException(e.getMessage());
		}
	}

	/* (non-Javadoc)
     * @see ca.ubc.cpsc210.nextbus.translink.ITranslinkService#getBusLocationsForStop(ca.ubc.cpsc210.nextbus.model.BusStop)
     */
	// By Rumneek 
	@Override
    public void addBusLocationsForStop(BusStop stop)
			throws TranslinkException {
		String stopNum = Integer.toString(stop.getStopNum());
		StringBuilder uriBuilder = new StringBuilder(
				"//api.translink.ca/RTTIAPI/V1/");
		uriBuilder.append("buses?stopNo=" + stopNum + "&"); 
		uriBuilder.append("apikey=" + APIKEY);
		
		try{
			String responseBody = makeJSONQuery(uriBuilder);
			parseBusesFromJSON(responseBody, stop);
		}catch (Exception e){
			Log.e(LOG_TAG, e.getMessage());
			throw new TranslinkException(e.getMessage());
		}
	}
	
	/**
	 * Execute a given query 
	 * 
	 * @param urlBuilder The query with everything but http:
	 * @return The JSON returned from the query 
	 * @throws TranslinkException  
	 * 				when an error occurs trying to connect or get data
	 * 				from Translink service
	 */
	private String makeJSONQuery(StringBuilder urlBuilder) throws TranslinkException {
		HttpURLConnection client = null;
		
		try {
			checkConnection();
			
			URL url = new URL("http:" + urlBuilder.toString());
			client = (HttpURLConnection) url.openConnection();
			client.setRequestProperty("accept", "application/json");
			client.setConnectTimeout(CONNECT_TIMEOUT);
			client.setReadTimeout(READ_TIMEOUT);
			client.connect();
			BufferedReader br;
			InputStream err = client.getErrorStream();
			if( err != null )
				br = new BufferedReader(new InputStreamReader(err));
			else {
				InputStream in = client.getInputStream();
				br = new BufferedReader(new InputStreamReader(in));
			}
			String returnString = br.readLine();
			return returnString;
		} catch (SocketTimeoutException e) {
			throw new TranslinkException("Unable to connect to Translink at this time");
		} catch (ConnectionException e) {
			throw new TranslinkException("Data not available: check network connection");
		} catch (Exception e) {
			throw new TranslinkException("Failed to get data from Translink service");
		} finally {
			if(client != null)
				client.disconnect();
		}
	}
	
	/**
	 * Checks that data connection is available on device
	 * @throws ConnectionException when data services are not available
	 */
	private void checkConnection() throws ConnectionException {
		ConnectivityManager cm = (ConnectivityManager) activity
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnected()) {
			throw new ConnectionException("Check network connection");
		}
	}
}
