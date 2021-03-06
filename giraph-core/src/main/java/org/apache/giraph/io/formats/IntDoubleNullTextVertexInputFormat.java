/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.io.formats;

import com.google.common.collect.Lists;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.edge.EdgeFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simple text-based {@link org.apache.giraph.io.VertexInputFormat} for
 * unweighted graphs with int ids.
 *
 * Each line consists of: vertex_id vertex_value neighbor1 neighbor2 ...
 */
public class IntDoubleNullTextVertexInputFormat
    extends
    TextVertexInputFormat<IntWritable, DoubleWritable, NullWritable> {
  /** Separator of the vertex and neighbors */
  private static final Pattern SEPARATOR = Pattern.compile("[\t ]");

  @Override
  public TextVertexReader createVertexReader(InputSplit split,
      TaskAttemptContext context)
    throws IOException {
    return new IntDoubleNullVertexReader();
  }

  /**
   * Vertex reader associated with
   * {@link org.apache.giraph.io.formats.IntIntNullTextVertexInputFormat}.
   */
  public class IntDoubleNullVertexReader extends
    TextVertexReaderFromEachLineProcessed<String[]> {
    /** Cached vertex id for the current line */
    private IntWritable id;
    /** Cached vertex value for the current line */
    private DoubleWritable value;

    @Override
    protected String[] preprocessLine(Text line) throws IOException {
      String[] tokens = SEPARATOR.split(line.toString());
      id = new IntWritable(Integer.parseInt(tokens[0]));
      value = new DoubleWritable(Double.parseDouble(tokens[1]));
      return tokens;
    }

    @Override
    protected IntWritable getId(String[] tokens) throws IOException {
      return id;
    }

    @Override
    protected DoubleWritable getValue(String[] tokens) throws IOException {
      return value;
    }

    @Override
    protected Iterable<Edge<IntWritable, NullWritable>> getEdges(
        String[] tokens) throws IOException {
      List<Edge<IntWritable, NullWritable>> edges =
          Lists.newArrayListWithCapacity(tokens.length - 2);
      for (int n = 2; n < tokens.length; n++) {
        edges.add(EdgeFactory.create(
            new IntWritable(Integer.parseInt(tokens[n]))));
      }
      return edges;
    }
  }
}
