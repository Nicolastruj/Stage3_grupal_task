package software.cheeselooker.apps;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import software.cheeselooker.control.IndexerCommand;
import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.implementations.ExpandedHierarchicalCsvStore;
import software.cheeselooker.implementations.GutenbergBookReader;
import software.cheeselooker.ports.IndexerReader;
import software.cheeselooker.ports.IndexerStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MainWithExpandedStore {
    public static void main(String[] args) {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake");
        Path invertedIndexPath = Paths.get(System.getProperty("user.dir"), "/data/datamart");
        //Path stopWordsPath = Paths.get("indexer/src/main/resources/stopwords.txt"); // to execute in IntelliJ
        Path stopWordsPath = Paths.get("/app/resources/stopwords.txt");
        String indexedBooksFilePath = Paths.get(System.getProperty("user.dir"), "data/indexed_books.txt").toString();

        Config config = new Config();
        NetworkConfig networkConfig = config.getNetworkConfig();
        JoinConfig joinConfig = networkConfig.getJoin();
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true)
                .addMember("10.26.14.217")
                .addMember("10.26.14.218")
                .addMember("10.26.14.219")
                .addMember("10.26.14.220")
                .addMember("10.26.14.221")
                .addMember("10.26.14.222")
                .addMember("10.26.14.223");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ITopic<String> topic = hazelcastInstance.getTopic("indexerTopic");

        String machineId = System.getenv("MACHINE_ID");

        IndexerReader indexerReader = new GutenbergBookReader(bookDatalakePath.toString());
        IndexerStore hierarchicalCsvStore = new ExpandedHierarchicalCsvStore(invertedIndexPath, stopWordsPath);
        IndexerCommand hierarchicalCsvController = new IndexerCommand(indexerReader, hierarchicalCsvStore, indexedBooksFilePath);

        topic.addMessageListener(message -> {
            String receivedMessage = message.getMessageObject();
            if (receivedMessage.equalsIgnoreCase("download_complete:" + 50 + ":" + machineId)) {
                try {
                    hierarchicalCsvController.execute();
                    System.out.println("Indexing process executed.");
                } catch (IndexerException e) {
                    throw new RuntimeException("Error while indexing books.", e);
                }
            }
        });

        System.out.println("Indexer is listening for download completion messages...");
    }
}
