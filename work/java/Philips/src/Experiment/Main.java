package Experiment;

import java.lang.reflect.Parameter;
import java.util.List;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class Main {
	public static final PHHueSDK philips = PHHueSDK.getInstance();
	public static List<PHLight> lights;
	public static int red = 255, green = 255 , blue = 255;
	public static boolean philipsSwitch = true;
	public static PHSDKListener listener = new PHSDKListener() {

		@Override
		public void onParsingErrors(List<PHHueParsingError> arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onError(int arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnectionResumed(PHBridge arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnectionLost(PHAccessPoint arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
			// TODO Auto-generated method stub
			if (arg0.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
				System.out.println("Lights Cache Updated ");
				System.exit(0);
			}
		}

		@Override
		public void onBridgeConnected(PHBridge arg0, String arg1) {
			// TODO Auto-generated method stub
			System.out.println("on bridge connected");
			philips.setSelectedBridge(arg0);
			philips.enableHeartbeat(arg0, PHHueSDK.HB_INTERVAL);
			Main.lights = philips.getSelectedBridge().getResourceCache().getAllLights();

			// System.out.println(lights.size());
			PHLightState lightState = new PHLightState();
			float xy[] = PHUtilities.calculateXYFromRGB(red, green, blue, lights.get(0).getModelNumber());
			
			lightState.setOn(philipsSwitch);
			lightState.setX(xy[0]);
			lightState.setY(xy[1]);
			//lightState.setHue(color);
			// lightState.setOn(false);
			{
				System.out.println("light support brightness : " + lights.get(0).supportsBrightness());
				System.out.println("light support CT : " + lights.get(0).supportsCT());
				System.out.println("light support color : " + lights.get(0).supportsColor());
				System.out.println("light support last known light statate : " + lights.get(0).getLastKnownLightState());
			}

			arg0.updateLightState(lights.get(0).getIdentifier(), lightState, null);

		}

		@Override
		public void onAuthenticationRequired(PHAccessPoint arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAccessPointsFound(List<PHAccessPoint> arg0) {
			// TODO Auto-generated method stub
		}
	};

	public static void main(String[] args) {
		if (args.length != 1 && args.length != 3) {
			System.err.println("Error for parameter ." );
			System.exit(1);
		}
		if(args.length == 1)
		{
			philipsSwitch = new Boolean(args[0]);
			System.out.println("Mode for switch.");
		}
		else
		{
			red= new Integer(args[0]);
			green = new Integer(args[1]);
			blue = new Integer(args[2]);
			System.out.println("Mode for color.");
		}
		// System.out.println(color);
		PHHueSDK philips = PHHueSDK.getInstance();
		philips.getNotificationManager().registerSDKListener(listener);
		PHBridgeSearchManager sm = (PHBridgeSearchManager) philips.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		sm.search(true, true);
		PHAccessPoint accessPoint = new PHAccessPoint();
		accessPoint.setIpAddress("192.168.1.8");
		accessPoint.setUsername("pehNjKT6cdWEaSk-uC9T6c4mpw6ecI8kz2zyjRuY");
		philips.connect(accessPoint);

		/*
		 * PHBridge bridge = philips.getSelectedBridge();
		 * 
		 * List<PHLight> lights = bridge.getResourceCache().getAllLights();
		 * 
		 * System.out.println(lights.size());
		 * 
		 * philips.disableAllHeartbeat(); philips.destroySDK();
		 */
		/*
		 * PHHeartbeatManager beatManager = PHHeartbeatManager.getInstance();
		 * PHBridgeResourcesCache cache =
		 * philips.getSelectedBridge().getResourceCache(); List <PHLight> lights
		 * = cache.getAllLights(); PHBridge bridge =
		 * PHHueSDK.getInstance().getSelectedBridge(); PHLightState status = new
		 * PHLightState(); status.setHue(12345);
		 */

	}
}