package main;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Sun implements MqttCallback{ 
	static MqttClient mqttClient;
	private static final String WEB_DRIVER_ID = "webdriver.chrome.driver";
	private static final String WEB_DRIVER_PATH = "/Users/junhwankim/Desktop/IOT 프로젝트 파일/IOT_MQTT_PROJECT/chromedriver";
	//태양 위치 조회 센서 좌표
	Double lat = 37.8669112;
	Double lon = 127.737210;
	
    public static void main(String[] args) {
    	Sun obj = new Sun();
    	obj.run();
    }
    public void run() {    	
    	connectBroker(); 
    	while(true) {
    		try {
    			String[] sun = getSun();
    	       	publish_data("sun", "{\"azimuth\": "+sun[0] +", \"altitude\": "+sun[1]+", \"lat\": "+lat+", \"lon\": "+lon+"}");
    	       	Thread.sleep(1000); 
    		}catch (Exception e) {
				// TODO: handle exception
    			try {
    				mqttClient.disconnect();
				} catch (MqttException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			e.printStackTrace();
    	        System.out.println("Disconnected");
    	        System.exit(0);
			}
    	}
    }
    
    public String[] getSun() {
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmm");
    	String date = d_format.format(current).substring(0,4)+"."+d_format.format(current)
    	.substring(4,6)+"."+d_format.format(current).substring(6,8); 
    	String time = d_format.format(current).substring(8,10)+":"+d_format.format(current).substring(10,12); 
    	String url = "https://suncalc.org/#/"+lat+","+lon+",17"+"/"+date+"/"+time+"/324.0/2";
    	System.out.println(url);
    	//셀레니엄으로 웹크롤링
    	try {
    		System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
    	}catch(Exception e){
    		System.out.println(e);
    	}
    	ChromeOptions options = new ChromeOptions();
    	options.addArguments("headless");
    	WebDriver driver = new ChromeDriver(options);
    	driver.get(url);
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String azimuth = driver.findElement(By.id("azimuth")).getText();
    	String altitude = driver.findElement(By.id("sunhoehe")).getText();
    	azimuth = azimuth.substring(0, azimuth.length()-1);
    	altitude = altitude.substring(0, altitude.length()-1);
    	System.out.println(azimuth);
    	System.out.println(altitude);
    	
    	driver.close();
    	driver.quit();
    	String[] sun = {azimuth, altitude};
    	return sun;
    }
    
    
    public void connectBroker() {
        String broker = "tcp://127.0.0.1:1883";
        String clientId = "sun"; 
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            mqttClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions(); 
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            mqttClient.connect(connOpts); 
            mqttClient.setCallback(this);
            System.out.println("Connected");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public void publish_data(String topic_input, String data) {
        String topic = topic_input; 
        int qos = 0; 
        try {
            System.out.println("Publishing message: "+data);
            mqttClient.publish(topic, data.getBytes(), qos, false);
            System.out.println("Message published");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		System.out.println("Connection lost");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		// TODO Auto-generated method stub
	}
}