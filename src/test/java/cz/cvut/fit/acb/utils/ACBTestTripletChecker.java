/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Jiri.Bican
 */
public class ACBTestTripletChecker {
	
	Queue<TripletCrate> queue = new LinkedList<>();
	int size = -1;
	
	public void t2b(TripletSupplier supplier, Consumer<TripletSupplier> delegate) {
		if (supplier == null) {
			delegate.accept(null);
			return;
		}
		supplier.visit(new TripletProcessor() {
			@Override
			public int read(TripletFieldId fieldId) {
				fail();
				return 0;
//				final int[] i = new int[1];
//				delegate.accept(visitor -> i[0] = visitor.read(fieldId));
//				TripletCrate crate = queue.poll();
//				assertNotNull(crate);
//				assertEquals(new TripletCrate(fieldId, i[0]), crate);
//				return i[0];
			}
			
			@Override
			public void write(TripletFieldId fieldId, int value) {
				queue.add(new TripletCrate(fieldId, value));
				delegate.accept(visitor -> visitor.write(fieldId, value));
			}
			
			@Override
			public int getSize() {
				fail();
				return 0;
//				final int[] i = new int[1];
//				delegate.accept(visitor -> i[0] = visitor.getSize());
//				assertEquals(size, i[0]);
//				size = -1;
//				return i[0];
			}
			
			@Override
			public void setSize(int size1) {
				assertTrue(size == -1);
				size = size1;
				delegate.accept(visitor -> visitor.setSize(size1));
			}
		});
	}
	
	public void b2t(TripletProcessor delegate, Consumer<TripletProcessor> consumer) {
		consumer.accept(new TripletProcessor() {
			@Override
			public int read(TripletFieldId fieldId) {
				final int i = delegate.read(fieldId);
				if (i == -1) {
					return -1;
				}
				TripletCrate crate = queue.poll();
				assertNotNull(crate);
				assertEquals(crate, new TripletCrate(fieldId, i));
				return i;
			}
			
			@Override
			public void write(TripletFieldId fieldId, int value) {
				fail();
			}
			
			@Override
			public int getSize() {
				final int i = delegate.getSize();
				assertEquals(size, i);
				return i;
			}
			
			@Override
			public void setSize(int size) {
				fail();
			}
		});
	}
	
	private static class TripletCrate {
		private final TripletFieldId fieldId;
		private final int value;
		
		public TripletCrate(TripletFieldId fieldId, int value) {
			this.fieldId = fieldId;
			this.value = value;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof TripletCrate)) return false;
			TripletCrate that = (TripletCrate) o;
			return value == that.value &&
					fieldId.isLength() == that.fieldId.isLength() &&
					fieldId.getBitSize() == that.fieldId.getBitSize() &&
					fieldId.getIndex() == that.fieldId.getIndex();
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(fieldId, value);
		}
		
		@Override
		public String toString() {
			return "TripletCrate{" +
					"fieldId=" + fieldId +
					", value=" + value +
					'}';
		}
	}
}
