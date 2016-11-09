/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.coding;

import java.util.ArrayList;
import java.util.Collection;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class AdaptiveArithmeticEncoder extends TripletToByteConverter<AdaptiveArithmeticCompress> {

    private final Collection<Runnable> onTerminate = new ArrayList<>();

    @Override
    protected byte[] getArray(AdaptiveArithmeticCompress object) {
        return object.array();
    }

    @Override
    protected AdaptiveArithmeticCompress createNew(TripletFieldId fieldId) {
        AdaptiveArithmeticCompress inst = new AdaptiveArithmeticCompress(fieldId.getBitSize());
        onTerminate.add(inst::terminate);
        return inst;
    }

    @Override
    protected void compress(AdaptiveArithmeticCompress object, int value) {
        object.compress(value);
    }

    @Override
    protected void terminate() {
        onTerminate.forEach(Runnable::run);
        super.terminate();
    }
}
