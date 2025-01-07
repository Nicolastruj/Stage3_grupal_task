package software.cheeselooker.apps;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
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
        tcpIpConfig.setEnabled(true).addMember("192.168.1.44").addMember("192.168.1.33");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ITopic<String> topic = hazelcastInstance.getTopic("indexerTopic");
        String machineId = System.getenv("MACHINE_ID");
        IMap<String, String> bookMap = hazelcastInstance.getMap("bookMap");
        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store, bookMap, topic, machineId);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        periodicTask(scheduler, crawlerCommand, topic, machineId);
    }

    private static void periodicTask(ScheduledExecutorService scheduler, Command crawlerCommand, ITopic topic, String machineId) {
        scheduler.scheduleAtFixedRate(() -> {

            crawlerCommand.download(50);
            topic.publish("download_complete:" + 50 + ":" + machineId);

        }, 0, 20, TimeUnit.MINUTES);
    }
}
