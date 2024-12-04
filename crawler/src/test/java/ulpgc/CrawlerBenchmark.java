package ulpgc;

import org.openjdk.jmh.annotations.*;
import software.cheeselooker.control.Command;
import software.cheeselooker.control.CrawlerCommand;
import software.cheeselooker.implementations.ReaderFromWeb;
import software.cheeselooker.implementations.StoreInDatalake;
import software.cheeselooker.ports.ReaderFromWebInterface;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 5, time = 1)
public class CrawlerBenchmark {
    @Benchmark
    public void crawler() {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "..", "BookDatalake").normalize();
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "..", "metadata.csv").normalize();
        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(bookDatalakePath.toString(), metadataPath.toString(), reader, store);
        crawlerCommand.download();
    }
}
