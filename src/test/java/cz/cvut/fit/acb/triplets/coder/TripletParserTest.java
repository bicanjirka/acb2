package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.SearchResult;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.Triplet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripletParserTest {

    private final LiteralAfterMatchParser literalAfterMatch = new LiteralAfterMatchParser();
    private final BareMatchParser bare = new BareMatchParser();

    @Test
    void noMatchIsALiteralForEveryParser() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'b'});

        assertThat(this.literalAfterMatch.parse(segment, 1, SearchResult.miss())).isEqualTo(Triplet.literal((byte) 'b'));
        assertThat(this.bare.parse(segment, 1, SearchResult.miss())).isEqualTo(Triplet.literal((byte) 'b'));
    }

    @Test
    void aMatchInsideTheSegmentTakesTheByteAfterItAsItsLiteral() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'b', 'a', 'b', 'z'});

        Triplet parsed = this.literalAfterMatch.parse(segment, 2, SearchResult.hit(1, 2));

        assertThat(parsed).isEqualTo(Triplet.matchWithLiteral(1, 2, (byte) 'z'));
    }

    @Test
    void aOneByteMatchEndingTheSegmentBecomesALiteral() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'b'});

        Triplet parsed = this.literalAfterMatch.parse(segment, 1, SearchResult.hit(0, 1));

        assertThat(parsed).isEqualTo(Triplet.literal((byte) 'b'));
    }

    @Test
    void aLongerMatchEndingTheSegmentGivesUpItsLastByteAsTheLiteral() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'a', 'z'});

        Triplet parsed = this.literalAfterMatch.parse(segment, 1, SearchResult.hit(3, 2));

        assertThat(parsed).isEqualTo(Triplet.matchWithLiteral(3, 1, (byte) 'z'));
    }

    @Test
    void aBareMatchMayEndTheSegment() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'a', 'z'});

        Triplet parsed = this.bare.parse(segment, 1, SearchResult.hit(3, 2));

        assertThat(parsed).isEqualTo(Triplet.match(3, 2));
    }
}
