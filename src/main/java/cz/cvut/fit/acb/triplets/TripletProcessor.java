package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

public interface TripletProcessor {
    /**
     * @return the next value of the field, or -1 once the field has ended
     * @throws MalformedStreamException if the stream cannot supply the field
     */
    int read(TripletFieldId fieldId) throws MalformedStreamException;

    void write(TripletFieldId fieldId, int value);

    int getSize();

    void setSize(int size);
}
