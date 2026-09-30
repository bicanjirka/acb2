package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** What one step of an associative coder does, the same on both sides of a stream. */
interface StepRule {

    /**
     * Codes the step that starts at {@code idx}, below the segment's length.
     *
     * @return how many bytes the step coded
     * @throws MalformedStreamException if the stream asks for something no text can have
     */
    int step(int idx) throws MalformedStreamException;
}
