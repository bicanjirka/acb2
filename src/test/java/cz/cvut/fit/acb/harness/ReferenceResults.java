package cz.cvut.fit.acb.harness;

import java.util.List;

/**
 * Other compressors on the Calgary corpus (the 14 classic files, 3,141,622 bytes, each compressed
 * on its own and summed), measured on the development machine on 2026-09-28. Shown beside the
 * harness's own rows when the input is that corpus.
 */
final class ReferenceResults {

    static final long CALGARY_BYTES = 3_141_622;

    record Result(String name, long bytes) {
    }

    private ReferenceResults() {
    }

    static List<Result> calgary() {
        return List.of(
                new Result("ACB 2.00a (Mahoney table)", 778_760),
                new Result("bzip2 -9", 828_347),
                new Result("AC.C 1994, Kc 0, 1 MB frame", 837_107),
                new Result("xz -9", 845_952),
                new Result("AC.C, Kc 0, 256 KB frame", 849_813),
                new Result("ExCom acb d=10", 967_714),
                new Result("ExCom acb defaults", 988_420),
                new Result("gzip -9", 1_017_624));
    }
}
