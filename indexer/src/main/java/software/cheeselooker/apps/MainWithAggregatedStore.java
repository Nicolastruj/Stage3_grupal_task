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

public class MainWithAggregatedStore {
    public static void main(String[] args) {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake");
        Path invertedIndexPath = Paths.get(System.getProperty("user.dir"), "/data/datamart");
        Path stopWordsPath = Paths.get("indexer/src/main/resources/stopwords.txt");
        String indexedBooksFilePath = Paths.get(System.getProperty("user.dir"), "data/indexed_books.txt").toString();

        // Configuración de Hazelcast
        Config config = new Config();
        NetworkConfig networkConfig = config.getNetworkConfig();
        JoinConfig joinConfig = networkConfig.getJoin();
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true).addMember("192.168.1.19").addMember("192.168.1.76"); // Agrega las IPs de los portátiles

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ITopic<String> topic = hazelcastInstance.getTopic("indexerTopic"); // El topic donde el crawler enviará los mensajes

        // Map para almacenar los datos serializados por máquina
        IMap<String, String> indexedDataMap = hazelcastInstance.getMap("IndexedDataMap");

        String machineId = System.getenv("MACHINE_ID");

        // Inicialización de las interfaces
        IndexerReader indexerReader = new GutenbergBookReader(bookDatalakePath.toString());
        IndexerStore hierarchicalCsvStore = new ExpandedHierarchicalCsvStore(invertedIndexPath, stopWordsPath);
        IndexerCommand hierarchicalCsvController = new IndexerCommand(indexerReader, hierarchicalCsvStore, indexedBooksFilePath);

        // Escuchar el topic y ejecutar la indexación cuando el crawler termine de descargar
        topic.addMessageListener(message -> {
            String receivedMessage = message.getMessageObject();
            if (receivedMessage.equalsIgnoreCase("download_complete:" + 50 + ":" + machineId)) {
                // Aquí puedes agregar lógica adicional si necesitas validar el mensaje
                try {
                    // Ejecuta el proceso de indexación
                    hierarchicalCsvController.execute();

                    // Serializa el contenido de la indexación
                    String serializedData = hierarchicalCsvStore.serializeData();

                    // Almacena el contenido en el Map
                    indexedDataMap.put(machineId, serializedData);


                    System.out.println("Indexing process executed.");
                } catch (IndexerException e) {
                    throw new RuntimeException("Error while indexing books.", e);
                }
            }
        });

        System.out.println("Indexer is listening for download completion messages...");
    }
}
