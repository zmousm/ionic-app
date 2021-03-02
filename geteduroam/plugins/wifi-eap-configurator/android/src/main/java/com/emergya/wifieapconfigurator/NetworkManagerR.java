package com.emergya.wifieapconfigurator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.util.ArrayList;
import java.util.List;

/**
 * NetworkManagerP is the responsable of implement the abstract methods of NetworkManager. This class
 * implements the methods to work in all devices with API greater and equal than API 29.
 */
public class NetworkManagerR extends NetworkManager {

	public NetworkManagerR(ProfileDetails profile) {
		super(profile);
	}

	/**
	 * Configure the network to work in devices with API 29 or greater.
	 * @param context
	 * @param enterpriseConfig
	 * @param call
	 * @param config
	 * @param activity
	 * @param ssid
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	public List connectNetwork(Context context, WifiEnterpriseConfig enterpriseConfig, PluginCall call, PasspointConfiguration config, Activity activity, String ssid) {
		WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		List result = new ArrayList();

		List<WifiNetworkSuggestion> configured = wifiManager.getNetworkSuggestions();

		for (WifiNetworkSuggestion conf: configured) {
			if (conf.getPasspointConfig() != null) {
				List<WifiNetworkSuggestion> sug = new ArrayList();
				sug.add(conf);
				wifiManager.removeNetworkSuggestions(sug);
			}
		}

		if (getPermission(Manifest.permission.CHANGE_NETWORK_STATE, context, activity)) {

			ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();

			// SSID configuration
			WifiNetworkSuggestion suggestion =  new WifiNetworkSuggestion.Builder()
					.setSsid(ssid)
					.setWpa2EnterpriseConfig(enterpriseConfig)
					.setIsAppInteractionRequired(true)
					.build();

			suggestions.add(suggestion);

			// Passpoint Configuration
			WifiNetworkSuggestion.Builder suggestionBuilder =  new WifiNetworkSuggestion.Builder();

			if (config != null) {
				suggestionBuilder.setPasspointConfig(config);
			}

			suggestions.add(suggestionBuilder.build());

			int status = wifiManager.addNetworkSuggestions(suggestions);

			if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {

				final IntentFilter intentFilter =
						new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

				final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if (!intent.getAction().equals(
								WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
							return;
						}
					}
				};

				context.registerReceiver(broadcastReceiver, intentFilter);

				JSObject object = new JSObject();
				object.put("success", true);
				object.put("message", "plugin.wifieapconfigurator.success.network.linked");
				result.add(object);
			} else {
				JSObject object = new JSObject();
				object.put("success", false);
				object.put("message", "plugin.wifieapconfigurator.success.network.reachable");
				result.add(object);
			}

			 /*
			// NEW APPROACH
			WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

			Intent intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
			intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, suggestions);
			getActivity().startActivityForResult(intent, 1000);

			final Activity activity = new Activity() {
				@Override
				protected void onActivityResult(int requestCode, int resultCode, Intent data) {
					super.onActivityResult(requestCode, resultCode, data);

					// check if the request code is same as what is passed  here it is 1
					if (requestCode == 1000) {
						// Make sure the request was successful
						if (resultCode == RESULT_OK) {
							System.out.println("The user agree the configuration");
						}
					}
				}
			};
			 */

			/*
			WifiNetworkSpecifier wifiNetworkSpecifier = suggestionBuilder.build();
			NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
			networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
			networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
			NetworkRequest networkRequest = networkRequestBuilder.build();
			final ConnectivityManager cm = (ConnectivityManager)
					getContext().getApplicationContext()
							.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
					@Override
					public void onAvailable(@NonNull Network network) {
						super.onAvailable(network);
						cm.bindProcessToNetwork(network);
					}});
			}*/
		}

		return result;
	}

}