package main;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SolarPannel implements MqttCallback{ 
	static MqttClient mqttClient;
	String pannelID = "solarPannel4"; //태양광 발전기의 고유한 아이디
	Double angle = 0.0; //패널 각도
	Double powerGeneration = 0.0; //발전량
	Double lat = 37.8666847; //발전기의 위도
	Double lon = 127.737401; //발전기의 경도
    public static void main(String[] args) {
    	SolarPannel obj = new SolarPannel();
    	obj.run();
    }
    
    public void run() {    	
    	connectBroker(); 
    	try { 
    		mqttClient.subscribe("publishSun"); // sun 리소스 구독
		} catch (MqttException e1) {
			e1.printStackTrace();
		}
    	while(true) {
    		try {
    			int power = getPowerGeneration();
    			publish_data("powerGeneration", "{"+"\"pannelID\": "+"\""+pannelID+"\""+", \"power\": "+power +", \"angle\" :"+angle+"}");
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
    
    public void connectBroker() {
        String broker = "tcp://127.0.0.1:1883"; 
        String clientId = pannelID; 
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
        int qos = 0; // QoS level
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
	
	//발전기 각도 조정 함수
	public void calcPannelAngle(Double azimuth, Double altitude, Double sunLat, Double sunLon) {
		//위치 조회한 센서까지의 거리 미터로 표시. 
		Double theta = sunLon - lon;
		Double dist = Math.sin(deg2rad(sunLat)) * Math.sin(deg2rad(lat))
				+ Math.cos(deg2rad(sunLat)) * Math.cos(deg2rad(lat)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		dist = dist * 1609.344;
		angle = Math.round(((altitude-(dist/20))+90.0)*100)/100.0 ;
	}
	
	// This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
	
	//발전량은 랜덤으로
	public int getPowerGeneration() {
		int power = (int)(Math.random()*100);
		return power;
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		// TODO Auto-generated method stub
		if (topic.equals("publishSun")){
			System.out.println("--------------------Actuator Function--------------------");
			System.out.println("get sun info");
			System.out.println("sun: " + msg.toString());
			System.out.println("---------------------------------------------------------");
			//데이터 받으면 각도 수정
			//json형태로 받은 데이터 파싱 후 저장
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(msg.toString());
			Double azimuth = Double.parseDouble(jsonObject.get("azimuth").toString());
			Double altitude = Double.parseDouble(jsonObject.get("altitude").toString());
			Double sunSensorLat = Double.parseDouble(jsonObject.get("lat").toString());
			Double sunSensorLon = Double.parseDouble(jsonObject.get("lon").toString());
			System.out.println(azimuth);
			System.out.println(altitude);
			calcPannelAngle(azimuth, altitude,sunSensorLat,sunSensorLon);
		}		
	}
	
	
}
