package cz.cvut.fit.acb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Works out which files a request reads and writes, and refuses a request that would clobber something. */
final class FileJobs {

    /** One input file and where its output goes. */
    record Job(Path source, Path target) {
    }

    private FileJobs() {
    }

    /**
     * A file input maps to the output path, or into it when it is a directory; a directory input
     * maps each regular file inside it to the same name in the output directory. Nothing is
     * written until every target has been checked.
     */
    static List<Job> plan(CliRequest.Work work) throws IOException {
        Path in = work.input();
        Path out = work.output();
        if (!Files.exists(in)) {
            throw new IOException("Input file or directory does not exist: " + in);
        }
        List<Job> jobs = Files.isDirectory(in) ? inDirectory(in, out)
                : List.of(new Job(in, Files.isDirectory(out) ? out.resolve(in.getFileName()) : out));
        for (Job job : jobs) {
            if (Files.exists(job.target())) {
                if (Files.isSameFile(job.source(), job.target())) {
                    throw new IOException("Output would overwrite its input: " + job.target());
                }
                if (!work.force()) {
                    throw new IOException("Output already exists, use -f to overwrite it: " + job.target());
                }
            }
        }
        return jobs;
    }

    private static List<Job> inDirectory(Path in, Path out) throws IOException {
        if (Files.exists(out) && !Files.isDirectory(out)) {
            throw new IOException("Input is a directory, so the output must be one too: " + out);
        }
        try (Stream<Path> files = Files.list(in)) {
            return files.filter(Files::isRegularFile).sorted()
                    .map(file -> new Job(file, out.resolve(file.getFileName()))).toList();
        }
    }
}
