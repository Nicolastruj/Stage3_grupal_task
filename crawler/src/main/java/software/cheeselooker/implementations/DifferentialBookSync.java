package software.cheeselooker.implementations;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DifferentialBookSync {
    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, String> bookMap; // Mapa distribuido con los libros
    private final String datalakePath; // Ruta de persistencia

    public DifferentialBookSync(HazelcastInstance hazelcastInstance, String datalakePath) {
        this.hazelcastInstance = hazelcastInstance;
        this.bookMap = hazelcastInstance.getMap("bookMap");
        this.datalakePath = datalakePath;
    }

    public void startDifferentialSync() {
        // Obtener miembros del clúster de Hazelcast
        Set<Member> members = hazelcastInstance.getCluster().getMembers();
        List<Member> memberList = new ArrayList<>(members);

        // Seleccionar un miembro aleatorio
        Random rand = new Random();
        Member selectedMember = memberList.get(rand.nextInt(memberList.size()));

        // Simula que obtenemos el archivo de metadatos del nodo seleccionado
        Set<String> downloadedBooksIds = getDownloadedBooksIds(selectedMember);

        // Buscar los libros que no están en el archivo de metadatos
        for (String bookId : bookMap.keySet()) {
            if (!downloadedBooksIds.contains(bookId)) {
                // Copiar libro no descargado al nodo de persistencia
                String bookTitle = bookMap.get(bookId);
                copyBookToPersistentNode(bookId, bookTitle);
            }
        }
    }

    private Set<String> getDownloadedBooksIds(Member selectedMember) {
        // Simula la lectura del archivo de metadatos en el nodo seleccionado
        Set<String> downloadedBooksIds = new HashSet<>();
        Path metadataFilePath = Paths.get("/path/to/metadata/directory", selectedMember.getUuid() + "_metadata.txt");

        try {
            List<String> lines = Files.readAllLines(metadataFilePath);
            for (String line : lines) {
                String[] parts = line.split(",");
                String bookId = parts[0]; // Suponiendo que la primera columna tiene el ID del libro
                downloadedBooksIds.add(bookId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return downloadedBooksIds;
    }

    private void copyBookToPersistentNode(String bookId, String bookTitle) {
        try {
            // Lógica para copiar el libro a la persistencia
            System.out.println("Copying book ID " + bookId + " (" + bookTitle + ") to persistent storage...");

            // Por ejemplo, copiamos el libro en el datalake (podrías usar una base de datos o almacenamiento distribuido)
            Path targetPath = Paths.get(datalakePath, bookId + ".txt");
            // Aquí iría el código para copiar el libro al almacenamiento persistente (base de datos, sistema de archivos, etc.)
            System.out.println("Book copied to: " + targetPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
