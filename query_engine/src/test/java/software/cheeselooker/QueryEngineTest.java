package software.cheeselooker;

import org.openjdk.jmh.annotations.*;
import software.cheeselooker.exceptions.QueryEngineException;
import software.cheeselooker.implementations.CommonQueryEngine;
import software.cheeselooker.model.QueryEngine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 5, time = 1)
public class QueryEngineTest {

    @State(Scope.Benchmark)
    public static class QueryEnginePath {
        public Path bookDatalakePath;
        public Path invertedIndexPath;
        public Path metaDataPath;

        @Param({"man", "immediate imminent"})
        public String word;

        @Setup(Level.Trial)
        public void setup() {
            invertedIndexPath = Paths.get(System.getProperty("user.dir"), "..", "InvertedIndex");
            bookDatalakePath = Paths.get(System.getProperty("user.dir"), "..", "BookDatalake");
            metaDataPath = Paths.get(System.getProperty("user.dir"), "..", "metadata.csv");
        }
    }

    @Benchmark
    public void benchmarkQueryEngine(QueryEnginePath path) {
        QueryEngine queryEngine = new CommonQueryEngine(
                path.metaDataPath.toString(),
                path.bookDatalakePath.toString(),
                path.invertedIndexPath.toString()
        );
        try {
            queryEngine.query(new String[]{path.word});
        } catch (QueryEngineException e) {
            throw new RuntimeException(e);
        }
    }
}
