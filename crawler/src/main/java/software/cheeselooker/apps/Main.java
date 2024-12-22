package software.cheeselooker.apps;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import software.cheeselooker.control.CrawlerCommand;
import software.cheeselooker.control.Command;
import software.cheeselooker.implementations.ReaderFromWeb;
import software.cheeselooker.implementations.StoreInDatalake;
import software.cheeselooker.ports.ReaderFromWebInterface;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Path datalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake").normalize();
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "/data/metadata/metadata.csv").normalize();

        Config config = new Config();
        NetworkConfig networkConfig = config.getNetworkConfig();
        JoinConfig joinConfig = networkConfig.getJoin();

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true).addMember("192.168.1.19").addMember("192.168.1.76"); // Agrega las IPs de los portátiles

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        IMap<String, String> confirmationMap = hazelcastInstance.getMap("confirmationMap"); // Mapa distribuido para la confirmación
        IMap<String, String> bookMap = hazelcastInstance.getMap("bookMap");
        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store, confirmationMap);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        periodicTask(scheduler, crawlerCommand, bookMap);
    }

    private static void periodicTask(ScheduledExecutorService scheduler, Command crawlerCommand, IMap<String, String> confirmationMap) {
        scheduler.scheduleAtFixedRate(() -> {
            // Clave en el mapa para la confirmación
            String confirmationKey = "taskConfirmation";

            // Verificar si ya se ejecutó la tarea
            String status = confirmationMap.get(confirmationKey);
            if ("ok".equals(status)) {
                System.out.println("Task already executed by another node. Skipping this time.");
                return;
            }

            // Marcar la tarea como en ejecución
            confirmationMap.put(confirmationKey, "ok");

            System.out.println("Starting download process...");
            crawlerCommand.download(50);

            // Marcar la tarea como completada después de la ejecución
            confirmationMap.put(confirmationKey, "completed");

        }, 0, 20, TimeUnit.MINUTES);
    }
}