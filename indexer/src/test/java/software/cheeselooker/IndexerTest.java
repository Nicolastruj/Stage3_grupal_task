package software.cheeselooker;

import org.openjdk.jmh.annotations.*;
import software.cheeselooker.apps.MainWithAggregatedStore;
import software.cheeselooker.control.IndexerCommand;
import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.implementations.AggregatedHierarchicalCsvStore;
import software.cheeselooker.implementations.ExpandedHierarchicalCsvStore;
import software.cheeselooker.implementations.GutenbergBookReader;
import software.cheeselooker.ports.IndexerReader;
import software.cheeselooker.ports.IndexerStore;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 5, time = 1)
public class IndexerTest {

    @State(Scope.Thread)
    public static class IndexerPath {
        public Path bookDatalakePath;
        public Path invertedIndexPath;
        public Path stopWordsPath;

        @Setup(Level.Trial)
        public void setup() throws IndexerException {
            bookDatalakePath = Paths.get(System.getProperty("user.dir"), "..", "BookDatalake").normalize();
            invertedIndexPath = Paths.get(System.getProperty("user.dir"), "..", "InvertedIndex").normalize();
            try {
                stopWordsPath = Paths.get(MainWithAggregatedStore.class.getClassLoader()
                        .getResource("stopwords.txt").toURI());
            } catch (URISyntaxException e) {
                throw new IndexerException(e.getMessage(), e);
            }

        }
    }

    @Benchmark
    public void aggregatedIndexer(IndexerPath path) throws IndexerException {
        IndexerReader indexerReader = new GutenbergBookReader(path.bookDatalakePath.toString());
        IndexerStore hierarchicalCsvStore = new AggregatedHierarchicalCsvStore(path.invertedIndexPath, path.stopWordsPath);
        IndexerCommand hierarchicalCsvController = new IndexerCommand(indexerReader, hierarchicalCsvStore);
        hierarchicalCsvController.execute();
    }

    @Benchmark
    public void expandedIndexer(IndexerPath path) throws IndexerException {
        IndexerReader indexerReader = new GutenbergBookReader(path.bookDatalakePath.toString());
        IndexerStore hierarchicalCsvStore = new ExpandedHierarchicalCsvStore(path.invertedIndexPath, path.stopWordsPath);
        IndexerCommand hierarchicalCsvController = new IndexerCommand(indexerReader, hierarchicalCsvStore);
        hierarchicalCsvController.execute();
    }
}
