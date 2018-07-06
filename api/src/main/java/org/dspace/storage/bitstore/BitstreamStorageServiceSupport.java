/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class BitstreamStorageServiceSupport {

	@Autowired
	private BitstreamStorageServiceImpl bitstreamStorage;

	public String absolutePath(Context context, Bitstream bitstream) throws IOException {
		Integer storeNumber = bitstream.getStoreNumber();
		BitStoreService bitStoreService = bitstreamStorage.getStores().get(storeNumber);
		if (bitStoreService instanceof DSBitStoreService) {
			DSBitStoreService dsBitStore = (DSBitStoreService) bitStoreService;
			return dsBitStore.getFile(bitstream).getAbsolutePath();
		} else {
			throw new IllegalStateException(
					"Only the DSBitStoreService allows direct access to the underlyne filesystem");
		}
	}
}
