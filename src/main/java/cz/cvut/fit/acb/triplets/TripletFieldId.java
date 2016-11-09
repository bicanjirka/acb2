/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets;

/**
 * @author jiri.bican
 */
public class TripletFieldId {
    private final int index;
    private final int bitSize;

    public TripletFieldId(int index, int bitSize) {
        this.index = index;
        this.bitSize = bitSize;
    }

    public int getIndex() {
        return index;
    }

    public int getBitSize() {
        return bitSize;
    }
    
    @Override
    public String toString() {
        return "[" + index + ", " + bitSize + ']';
    }
}
