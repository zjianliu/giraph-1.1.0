package org.apache.giraph.counters;

import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.hadoop.mapreduce.Mapper.Context;

import java.net.UnknownHostException;

public class GiraphWorkerStats extends HadoopCountersBase {
    /** Counter group name for the giraph worker stats */
    public static String GROUP_NAME;
    /** sent messages counter name */
    public static final String SENTMESSAGECOUNT_NAME = "SentMessageCount";
    /** sent message bytes counter name */
    public static final String SENTMESSAGEBYTE_NAME = "SentMessageBytes";
    /** receive messages counter name */
    public static final String RECEIVEMESSAGECOUNT_NAME = "ReceiveMessageCount";
    /** sent message bytes counter name */
    public static final String RECEIVEMESSAGEBYTE_NAME = "ReceiveMessageBytes";
    /** computed vertex counter name */
    public static final String COMPUTEDVERTEXCOUNT_NAME = "ComputedVertexCount";
    /** vertex counter name */
    public static final String VERTEXCOUNT_NAME = "VertexCount";

    /** Singleton instance for everyone to use */
    public static GiraphWorkerStats INSTANCE;

    /** SentMessage in number */
    private static final int SENTMESSAGECOUNT = 0;
    /** SentMessage in bytes */
    private static final int SENTMESSAGEBYTE = 1;
    /** ReceiveMessage in number */
    private static final int RECEIVEMESSAGECOUNT = 2;
    /** ReceiveMessage in bytes */
    private static final int RECEIVEMESSAGEBYTE = 3;
    /** ComputedVertex in number */
    private static final int COMPUTEDVERTEXCOUNT = 4;
    /** Vertex in number */
    private static final int VERTEXCOUNT = 5;
    /** How many whole job counters we have */
    private static final int NUM_COUNTERS = 6;

    /** Whole job counters stored in this class */
    private final GiraphHadoopCounter[] jobCounters;

    private GiraphWorkerStats(Context context) throws UnknownHostException{
        super(context, (new ImmutableClassesGiraphConfiguration<>(context.getConfiguration())).getLocalHostname() + " Stats");
        GROUP_NAME = (new ImmutableClassesGiraphConfiguration<>(context.getConfiguration())).getLocalHostname() + " Stats";

        jobCounters = new GiraphHadoopCounter[NUM_COUNTERS];
        jobCounters[SENTMESSAGECOUNT] = getCounter(SENTMESSAGECOUNT_NAME);
        jobCounters[SENTMESSAGEBYTE] = getCounter(SENTMESSAGEBYTE_NAME);
        jobCounters[RECEIVEMESSAGECOUNT] = getCounter(RECEIVEMESSAGECOUNT_NAME);
        jobCounters[RECEIVEMESSAGEBYTE] = getCounter(RECEIVEMESSAGEBYTE_NAME);
        jobCounters[COMPUTEDVERTEXCOUNT] = getCounter(COMPUTEDVERTEXCOUNT_NAME);
        jobCounters[VERTEXCOUNT] = getCounter(VERTEXCOUNT_NAME);
    }

    /**
     * Instantiate with Hadoop Context.
     * @param context
     * @throws UnknownHostException
     */
    public static void init(Context context) throws UnknownHostException{
        INSTANCE = new GiraphWorkerStats(context);
    }

    /**
     * Get singleton instance.
     *
     * @return singleton GiraphWorkerStats instance.
     */
    public static GiraphWorkerStats getInstance() {
        return INSTANCE;
    }

    public GiraphHadoopCounter getSentMessageCount() {
        return jobCounters[SENTMESSAGECOUNT];
    }

    public GiraphHadoopCounter getSentMessageBytes() {
        return jobCounters[SENTMESSAGEBYTE];
    }


}
