package ar.edu.ahk;

import io.javalin.Javalin;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Server implements MqttCallback {

    private String tempActual = "?";
    private MqttClient client;

    public MqttClient getClient() {
        return client;
    }

    public String getTempActual() {
        return tempActual;
    }

    public void setTempActual(String tempActual) {
        this.tempActual = tempActual;
    }

    public static void main(String[] args) {
        Server mqttServ = new Server();
        Javalin app = Javalin.create().start(getHerokuAssignedPort());
        app.get("/", ctx -> ctx.result("Test AHK MQTT - temometro: " + mqttServ.getTempActual()));
        app.get("/termometro/:temp", ctx -> {
            // Deberia ser un POST, pero para que sea mas facil probar pusimos un GET
                    int qos = 2;
                    String temp = ctx.pathParam("temp");
                    MqttMessage message = new MqttMessage(temp.getBytes());
                    message.setQos(qos);
                    mqttServ.getClient().publish(System.getenv("topic"), message);
                    mqttServ.setTempActual(temp);
                    ctx.result("temometro actualizado: " + mqttServ.getTempActual());
                }
        );
//        app.get("/test", ctx -> {
//
//            CloseableHttpClient httpClient = HttpClients.createDefault();
//            HttpGet request = new HttpGet(System.getenv("urlServidor"));
//            System.out.println(httpClient.execute(request).getEntity().toString());
//
//        });
        mqttServ.subscribe(System.getenv("clientID"));
    }

    private static int getHerokuAssignedPort() {
        String herokuPort = System.getenv("PORT");
        if (herokuPort != null) {
            return Integer.parseInt(herokuPort);
        }
        return 7000;
    }



    /**
     * The topic.
     */


    public void subscribe(String clientId) {
        //    logger file name and pattern to log
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            String brokerUrl =System.getenv("brokerUrl");
            this.client = new MqttClient(brokerUrl, clientId, persistence);
            System.out.println("Starting: " + clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(System.getenv("usuario"));
            connOpts.setPassword(System.getenv("password").toCharArray());
            connOpts.setCleanSession(true);
            System.out.println("checking");
            System.out.println("Mqtt Connecting to broker: " + brokerUrl);
            client.connect(connOpts);
            System.out.println("Mqtt Connected");
            client.setCallback(this);
            client.subscribe(System.getenv("topic"));
            System.out.println("Subscribed");
            System.out.println("Listening");

        } catch (MqttException me) {
            System.out.println(me);
        }
    }

    //Que muestra cuando perdes la conexion con la mensajeria mqtt
    public void connectionLost(Throwable arg0) {
        System.out.println(arg0);
    }

    // se llama cuando enviaste un mensaje correctamente
    public void deliveryComplete(IMqttDeliveryToken arg0) {
    }

    public void messageArrived(String topic, MqttMessage message) {
        this.tempActual = message.toString();
        System.out.println("Topic:" + topic);
        System.out.println("Message: " + message.toString());
    }

}
