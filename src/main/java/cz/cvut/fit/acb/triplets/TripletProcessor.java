package cz.cvut.fit.acb.triplets;

public interface TripletProcessor {

    void set(TripletFieldId fieldId, int value);
    int get(TripletFieldId fieldId);
}
