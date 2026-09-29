package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.LiteralContext;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextRangeCodingTest {

    private static final TripletFieldId LENGTH = new TripletFieldId(0, 4, TripletFieldKind.LENGTH);
    private static final TripletFieldId LITERAL = new TripletFieldId(1, 8, TripletFieldKind.LITERAL);

    @Test
    void literalsComeBackWithTheContextsTheyWereWrittenWith() throws MalformedStreamException {
        LiteralContext[] contexts = {LiteralContext.none(), new LiteralContext('a', -1), new LiteralContext('a', 'b'),
                new LiteralContext(0, 255), new LiteralContext(255, 0), new LiteralContext('z', 'y')};
        int[] literals = {'q', 'c', 'c', 0, 255, 'x'};
        ContextRangeTripletWriter writer = new ContextRangeTripletWriter(LengthFrequencies.flat());
        for (int i = 0; i < literals.length; i++) {
            writer.write(LENGTH, i, LiteralContext.none());
            writer.write(LITERAL, literals[i], contexts[i]);
        }
        ContextRangeTripletReader reader = new ContextRangeTripletReader(writer.finish(), LengthFrequencies.flat());

        for (int i = 0; i < literals.length; i++) {
            assertThat(reader.read(LENGTH)).isEqualTo(i);
            assertThat(reader.read(LITERAL, contexts[i])).isEqualTo(literals[i]);
        }
    }

    @Test
    void aLiteralCannotBeTheByteItExcludes() {
        ContextRangeTripletWriter writer = new ContextRangeTripletWriter(LengthFrequencies.flat());

        assertThatThrownBy(() -> writer.write(LITERAL, 'b', new LiteralContext('a', 'b')))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aByteThatAlwaysFollowsTheSameByteCostsAlmostNothing() {
        ContextRangeTripletWriter contextual = new ContextRangeTripletWriter(LengthFrequencies.flat());
        RangeTripletWriter plain = new RangeTripletWriter(LengthFrequencies.flat());
        for (int i = 0; i < 2000; i++) {
            int previous = i % 2 == 0 ? 'a' : 'b';
            int literal = i % 2 == 0 ? 'x' : 'y';
            contextual.write(LITERAL, literal, new LiteralContext(previous, -1));
            plain.write(LITERAL, literal);
        }

        contextual.finish();
        plain.finish();

        long contextualBits = contextual.costs().getFirst().bits();
        long plainBits = plain.costs().getFirst().bits();
        assertThat(contextualBits).isLessThan(plainBits / 2);
    }
}
