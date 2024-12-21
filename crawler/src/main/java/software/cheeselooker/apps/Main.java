package software.cheeselooker.apps;

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

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
        IMap<Integer, String> bookMap = hazelcastInstance.getMap("bookMap");  // Obtener el IMap

        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store, bookMap);  // Pasar el IMap al constructor

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        periodicTask(scheduler, crawlerCommand);
    }

    private static void periodicTask(ScheduledExecutorService scheduler, Command crawlerCommand) {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Starting download process...");
            crawlerCommand.download(50);
        }, 0, 20, TimeUnit.MINUTES);
    }
}
