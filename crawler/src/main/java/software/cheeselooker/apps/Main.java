package software.cheeselooker.apps;

import software.cheeselooker.control.Command;
import software.cheeselooker.control.CrawlerCommand;
import software.cheeselooker.implementations.ReaderFromWeb;
import software.cheeselooker.implementations.StoreInDatalake;
import software.cheeselooker.ports.ReaderFromWebInterface;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Path datalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake").normalize();
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "/data/metadata/metadata.csv").normalize();

        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        periodicTask(scheduler, crawlerCommand);
    }

    private static void periodicTask(ScheduledExecutorService scheduler, Command crawlerCommand) {
        scheduler.scheduleAtFixedRate(() -> {
            File confirmationFile = new File(System.getProperty("user.dir") + "/data/confirmation");

            if (confirmationFile.exists()) {
                try {
                    String content = new String(Files.readAllBytes(confirmationFile.toPath())).trim();
                    if (content.equals("ok")) {
                        System.out.println("Task already executed. Skipping this time.");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Crear el archivo de confirmación con "ok"
            try {
                Files.write(confirmationFile.toPath(), "ok".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Starting download process...");
            crawlerCommand.download(3);

            // Borrar el archivo de confirmación después de ejecutar la tarea
            if (confirmationFile.exists()) {
                confirmationFile.delete();
            }

        }, 0, 20, TimeUnit.MINUTES);
    }
}
