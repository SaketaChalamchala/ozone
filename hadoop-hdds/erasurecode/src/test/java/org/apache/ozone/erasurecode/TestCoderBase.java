/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ozone.erasurecode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;

/**
 * Test base of common utilities for tests not only raw coders but also block
 * coders.
 */
@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:HiddenField"})
public abstract class TestCoderBase {
  private static int fixedDataGenerator = 0;
  protected boolean allowDump = true;
  protected int numDataUnits;
  protected int numParityUnits;
  protected int baseChunkSize = 1024;
  // Indexes of erased data units.
  protected int[] erasedDataIndexes = new int[]{0};
  // Indexes of erased parity units.
  protected int[] erasedParityIndexes = new int[]{0};
  // Data buffers are either direct or on-heap, for performance the two cases
  // may go to different coding implementations.
  protected boolean usingDirectBuffer = true;
  protected boolean usingFixedData = true;
  protected byte[][] fixedData;
  private int chunkSize = baseChunkSize;
  private BufferAllocator allocator;
  private byte[] zeroChunkBytes;
  private boolean startBufferWithZero = true;
  // Using this the generated data can be repeatable across multiple calls to
  // encode(), in order for troubleshooting.
  private ConfigurationSource conf;

  protected int getChunkSize() {
    return chunkSize;
  }

  protected void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    this.zeroChunkBytes = new byte[chunkSize]; // With ZERO by default
  }

  protected byte[] getZeroChunkBytes() {
    return zeroChunkBytes;
  }

  protected void prepareBufferAllocator(boolean usingSlicedBuffer) {
    if (usingSlicedBuffer) {
      int roughEstimationSpace =
          chunkSize * (numDataUnits + numParityUnits) * 10;
      allocator = new BufferAllocator.SlicedBufferAllocator(usingDirectBuffer,
          roughEstimationSpace);
    } else {
      allocator = new BufferAllocator.SimpleBufferAllocator(usingDirectBuffer);
    }
  }

  protected boolean isAllowDump() {
    return allowDump;
  }

  /**
   * Prepare before running the case.
   *
   * @param usingFixedData Using fixed or pre-generated data to test instead of
   *                       generating data
   */
  protected void prepare(ConfigurationSource conf,
      int numDataUnits,
      int numParityUnits,
      int[] erasedDataIndexes,
      int[] erasedParityIndexes,
      boolean usingFixedData) {
    this.conf = conf != null ? conf : new OzoneConfiguration();
    this.numDataUnits = numDataUnits;
    this.numParityUnits = numParityUnits;
    this.erasedDataIndexes = erasedDataIndexes != null ?
        erasedDataIndexes : new int[]{0};
    this.erasedParityIndexes = erasedParityIndexes != null ?
        erasedParityIndexes : new int[]{0};
    this.usingFixedData = usingFixedData;
    if (usingFixedData) {
      prepareFixedData();
    }
  }

  /**
   * Prepare before running the case.
   *
   * @param conf
   * @param numDataUnits
   * @param numParityUnits
   * @param erasedDataIndexes
   * @param erasedParityIndexes
   */
  protected void prepare(ConfigurationSource conf, int numDataUnits,
      int numParityUnits, int[] erasedDataIndexes,
      int[] erasedParityIndexes) {
    prepare(conf, numDataUnits, numParityUnits, erasedDataIndexes,
        erasedParityIndexes, false);
  }

  /**
   * Prepare before running the case.
   *
   * @param numDataUnits
   * @param numParityUnits
   * @param erasedDataIndexes
   * @param erasedParityIndexes
   */
  protected void prepare(
      int numDataUnits,
      int numParityUnits,
      int[] erasedDataIndexes,
      int[] erasedParityIndexes) {
    prepare(null, numDataUnits, numParityUnits, erasedDataIndexes,
        erasedParityIndexes, false);
  }

  /**
   * Get the conf the test.
   *
   * @return configuration
   */
  protected ConfigurationSource getConf() {
    return this.conf;
  }

  /**
   * Compare and verify if erased chunks are equal to recovered chunks.
   *
   * @param erasedChunks
   * @param recoveredChunks
   */
  protected void compareAndVerify(ECChunk[] erasedChunks,
      ECChunk[] recoveredChunks) {
    byte[][] erased = toArrays(erasedChunks);
    byte[][] recovered = toArrays(recoveredChunks);
    boolean result = Arrays.deepEquals(erased, recovered);
    assertTrue(result, "Decoding and comparing failed.");
  }

  /**
   * Adjust and return erased indexes altogether, including erased data indexes
   * and parity indexes.
   * @return erased indexes altogether
   */
  protected int[] getErasedIndexesForDecoding() {
    int[] erasedIndexesForDecoding =
        new int[erasedDataIndexes.length + erasedParityIndexes.length];

    int idx = 0;

    for (int erasedDataIndex : erasedDataIndexes) {
      erasedIndexesForDecoding[idx++] = erasedDataIndex;
    }

    for (int erasedParityIndex : erasedParityIndexes) {
      erasedIndexesForDecoding[idx++] = erasedParityIndex + numDataUnits;
    }

    return erasedIndexesForDecoding;
  }

  /**
   * Return input chunks for decoding, which is dataChunks + parityChunks.
   *
   * @param dataChunks
   * @param parityChunks
   * @return
   */
  protected ECChunk[] prepareInputChunksForDecoding(ECChunk[] dataChunks,
      ECChunk[] parityChunks) {
    ECChunk[] inputChunks = new ECChunk[numDataUnits + numParityUnits];

    int idx = 0;

    for (int i = 0; i < numDataUnits; i++) {
      inputChunks[idx++] = dataChunks[i];
    }

    for (int i = 0; i < numParityUnits; i++) {
      inputChunks[idx++] = parityChunks[i];
    }

    return inputChunks;
  }

  /**
   * Erase some data chunks to test the recovering of them. As they're erased,
   * we don't need to read them and will not have the buffers at all, so just
   * set them as null.
   *
   * @param dataChunks
   * @param parityChunks
   * @return clone of erased chunks
   */
  protected ECChunk[] backupAndEraseChunks(ECChunk[] dataChunks,
      ECChunk[] parityChunks) {
    ECChunk[] toEraseChunks = new ECChunk[erasedDataIndexes.length +
        erasedParityIndexes.length];

    int idx = 0;

    for (int erasedDataIndex : erasedDataIndexes) {
      toEraseChunks[idx++] = dataChunks[erasedDataIndex];
      dataChunks[erasedDataIndex] = null;
    }

    for (int erasedParityIndex : erasedParityIndexes) {
      toEraseChunks[idx++] = parityChunks[erasedParityIndex];
      parityChunks[erasedParityIndex] = null;
    }

    return toEraseChunks;
  }

  /**
   * Erase data from the specified chunks, just setting them as null.
   * @param chunks
   */
  protected void eraseDataFromChunks(ECChunk[] chunks) {
    Arrays.fill(chunks, null);
  }

  protected void markChunks(ECChunk[] chunks) {
    for (ECChunk chunk : chunks) {
      if (chunk != null) {
        chunk.getBuffer().mark();
      }
    }
  }

  protected void restoreChunksFromMark(ECChunk[] chunks) {
    for (ECChunk chunk : chunks) {
      if (chunk != null) {
        chunk.getBuffer().reset();
      }
    }
  }

  /**
   * Clone chunks along with copying the associated data. It respects how the
   * chunk buffer is allocated, direct or non-direct. It avoids affecting the
   * original chunk buffers.
   * @param chunks
   * @return
   */
  protected ECChunk[] cloneChunksWithData(ECChunk[] chunks) {
    ECChunk[] results = new ECChunk[chunks.length];
    for (int i = 0; i < chunks.length; i++) {
      results[i] = cloneChunkWithData(chunks[i]);
    }

    return results;
  }

  /**
   * Clone chunk along with copying the associated data. It respects how the
   * chunk buffer is allocated, direct or non-direct. It avoids affecting the
   * original chunk.
   * @param chunk
   * @return a new chunk
   */
  protected ECChunk cloneChunkWithData(ECChunk chunk) {
    if (chunk == null) {
      return null;
    }

    ByteBuffer srcBuffer = chunk.getBuffer();

    byte[] bytesArr = new byte[srcBuffer.remaining()];
    srcBuffer.mark();
    srcBuffer.get(bytesArr, 0, bytesArr.length);
    srcBuffer.reset();

    ByteBuffer destBuffer = allocateOutputBuffer(bytesArr.length);
    int pos = destBuffer.position();
    destBuffer.put(bytesArr);
    destBuffer.flip();
    destBuffer.position(pos);

    return new ECChunk(destBuffer);
  }

  /**
   * Allocate a chunk for output or writing.
   * @return
   */
  protected ECChunk allocateOutputChunk() {
    ByteBuffer buffer = allocateOutputBuffer(chunkSize);

    return new ECChunk(buffer);
  }

  /**
   * Allocate a buffer for output or writing. It can prepare for two kinds of
   * data buffers: one with position as 0, the other with position > 0
   * @return a buffer ready to write chunkSize bytes from current position
   */
  protected ByteBuffer allocateOutputBuffer(int bufferLen) {
    /**
     * When startBufferWithZero, will prepare a buffer as:---------------
     * otherwise, the buffer will be like:             ___TO--BE--WRITTEN___,
     * and in the beginning, dummy data are prefixed, to simulate a buffer of
     * position > 0.
     */
    int startOffset = startBufferWithZero ? 0 : 11; // 11 is arbitrary
    int allocLen = startOffset + bufferLen + startOffset;
    ByteBuffer buffer = allocator.allocate(allocLen);
    buffer.limit(startOffset + bufferLen);
    fillDummyData(buffer, startOffset);
    startBufferWithZero = !startBufferWithZero;

    return buffer;
  }

  /**
   * Prepare data chunks for each data unit, by generating random data.
   * @return
   */
  protected ECChunk[] prepareDataChunksForEncoding() {
    if (usingFixedData) {
      ECChunk[] chunks = new ECChunk[numDataUnits];
      for (int i = 0; i < chunks.length; i++) {
        chunks[i] = makeChunkUsingData(fixedData[i]);
      }
      return chunks;
    }

    return generateDataChunks();
  }

  private ECChunk makeChunkUsingData(byte[] data) {
    ECChunk chunk = allocateOutputChunk();
    ByteBuffer buffer = chunk.getBuffer();
    int pos = buffer.position();
    buffer.put(data, 0, chunkSize);
    buffer.flip();
    buffer.position(pos);

    return chunk;
  }

  private ECChunk[] generateDataChunks() {
    ECChunk[] chunks = new ECChunk[numDataUnits];
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = generateDataChunk();
    }

    return chunks;
  }

  private void prepareFixedData() {
    // We may load test data from a resource, or just generate randomly.
    // The generated data will be used across subsequent encode/decode calls.
    this.fixedData = new byte[numDataUnits][];
    for (int i = 0; i < numDataUnits; i++) {
      fixedData[i] = generateFixedData(baseChunkSize * 2);
    }
  }

  /**
   * Generate data chunk by making random data.
   * @return
   */
  protected ECChunk generateDataChunk() {
    ByteBuffer buffer = allocateOutputBuffer(chunkSize);
    int pos = buffer.position();
    buffer.put(generateData(chunkSize));
    buffer.flip();
    buffer.position(pos);

    return new ECChunk(buffer);
  }

  /**
   * Fill len of dummy data in the buffer at the current position.
   * @param buffer
   * @param len
   */
  protected void fillDummyData(ByteBuffer buffer, int len) {
    byte[] dummy = new byte[len];
    dummy = RandomUtils.secure().randomBytes(dummy.length);
    buffer.put(dummy);
  }

  protected byte[] generateData(int len) {
    byte[] buffer = new byte[len];
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = (byte) RandomUtils.secure().randomInt(0, 256);
    }
    return buffer;
  }

  protected byte[] generateFixedData(int len) {
    byte[] buffer = new byte[len];
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = (byte) fixedDataGenerator++;
      if (fixedDataGenerator == 256) {
        fixedDataGenerator = 0;
      }
    }
    return buffer;
  }

  /**
   * Prepare parity chunks for encoding, each chunk for each parity unit.
   * @return
   */
  protected ECChunk[] prepareParityChunksForEncoding() {
    ECChunk[] chunks = new ECChunk[numParityUnits];
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = allocateOutputChunk();
    }

    return chunks;
  }

  /**
   * Prepare output chunks for decoding, each output chunk for each erased
   * chunk.
   * @return
   */
  protected ECChunk[] prepareOutputChunksForDecoding() {
    ECChunk[] chunks = new ECChunk[erasedDataIndexes.length +
        erasedParityIndexes.length];

    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = allocateOutputChunk();
    }

    return chunks;
  }

  /**
   * Convert an array of this chunks to an array of byte array.
   * Note the chunk buffers are not affected.
   * @param chunks
   * @return an array of byte array
   */
  protected byte[][] toArrays(ECChunk[] chunks) {
    byte[][] bytesArr = new byte[chunks.length][];

    for (int i = 0; i < chunks.length; i++) {
      if (chunks[i] != null) {
        bytesArr[i] = chunks[i].toBytesArray();
      }
    }

    return bytesArr;
  }

  /**
   * Dump all the settings used in the test case if isAllowingVerboseDump
   * is enabled.
   */
  protected void dumpSetting() {
    if (allowDump) {
      StringBuilder sb = new StringBuilder("Erasure coder test settings:\n");
      sb.append(" numDataUnits=").append(numDataUnits);
      sb.append(" numParityUnits=").append(numParityUnits);
      sb.append(" chunkSize=").append(chunkSize).append('\n');

      sb.append(" erasedDataIndexes=").
          append(Arrays.toString(erasedDataIndexes));
      sb.append(" erasedParityIndexes=").
          append(Arrays.toString(erasedParityIndexes));
      sb.append(" usingDirectBuffer=").append(usingDirectBuffer);
      sb.append(" allowVerboseDump=").append(allowDump);
      sb.append('\n');

      System.out.println(sb.toString());
    }
  }

  /**
   * Dump chunks prefixed with a header if isAllowingVerboseDump is enabled.
   */
  protected void dumpChunks(String header, ECChunk[] chunks) {
    if (allowDump) {
      DumpUtil.dumpChunks(header, chunks);
    }
  }

  /**
   * Make some chunk messy or not correct any more.
   */
  protected void corruptSomeChunk(ECChunk[] chunks) {
    int idx = RandomUtils.secure().randomInt(1, chunks.length);
    ByteBuffer buffer = chunks[idx].getBuffer();
    if (buffer.hasRemaining()) {
      buffer.position(buffer.position() + 1);
    }
  }
}
