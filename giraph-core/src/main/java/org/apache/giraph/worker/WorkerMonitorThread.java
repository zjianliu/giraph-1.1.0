package org.apache.giraph.worker;

import net.iharder.Base64;
import org.apache.giraph.bsp.BspService;
import org.apache.giraph.graph.GraphTaskManager;
import org.apache.giraph.monitor.Monitor;
import org.apache.giraph.partition.PartitionStats;
import org.apache.giraph.utils.WritableUtils;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;

public class WorkerMonitorThread<I extends WritableComparable, V extends Writable,
        E extends Writable> extends Thread {
    private final Logger LOG = Logger.getLogger(WorkerMonitorThread.class);

    private GraphTaskManager<I, V, E> graphTaskManager;
    private BspService<I, V, E> bspService;
    private Mapper<?, ?, ?, ?>.Context context;

    public WorkerMonitorThread(GraphTaskManager<I, V, E> graphTaskManager, BspService<I, V, E> bspService,
                               Mapper<?, ?, ?, ?>.Context context){
        this.graphTaskManager = graphTaskManager;
        this.bspService = bspService;
        this.context = context;
    }

    @Override
    public void run() {
        try{
            if(LOG.isInfoEnabled()){
                LOG.info("WorkerMonitorThread starts to monitor the giraph system.");
            }
            Socket socket = graphTaskManager.getMonitorSocket();
            final PrintWriter pw = new PrintWriter(socket.getOutputStream());


            Monitor monitor = new Monitor(context);

            /*
            //the time the current superstep starts
            double startSecond = System.currentTimeMillis() / 1000d;
            double superStepSecond;
            double intervalSecond;
            long superstep = -2;
            */

            while(!graphTaskManager.isApplicationFinished()){
                String systemStatus = graphTaskManager.getWorkerSystemStatus(monitor);
                pw.println(systemStatus);
                pw.flush();

                /*
                if(graphTaskManager.getGraphFunctions().isWorker()) {
                    long currentSuperstep = bspService.getSuperstep();
                    if(superstep != currentSuperstep){
                        superstep = currentSuperstep;
                        startSecond = System.currentTimeMillis() / 1000d;
                    }

                    superStepSecond = System.currentTimeMillis() / 1000d;
                    String finishedWorkerPathDir = bspService.getWorkerFinishedPath(bspService.getApplicationAttempt(),
                            bspService.getSuperstep());
                    String currentFinishedWorkerPath = finishedWorkerPathDir + bspService.getHostnamePartitionId();
                    try {
                        List<String> finishedHostnameIdList = bspService.getZkExt().getChildrenExt(finishedWorkerPathDir,
                                true,
                                false,
                                false);
                        for (String finishedHostnameId : finishedHostnameIdList) {
                            intervalSecond = superStepSecond - startSecond;
                            if (finishedHostnameId.equals(currentFinishedWorkerPath)) {
                                long workerSentMessages = 0;
                                long workerSentMessageBytes = 0;
                                long workerComputedVertex = 0;

                                byte[] zkData = bspService.getZkExt().getData(finishedHostnameId, false, null);
                                JSONObject workerFinishedInfoObj = new JSONObject(new String(zkData, Charset.defaultCharset()));
                                List<PartitionStats> statsList =
                                        WritableUtils.readListFieldsFromByteArray(
                                                Base64.decode(workerFinishedInfoObj.getString(
                                                        bspService.JSONOBJ_PARTITION_STATS_KEY)),
                                                new PartitionStats().getClass(),
                                                bspService.getConfiguration());
                                for (PartitionStats partitionStats : statsList) {
                                    workerComputedVertex += partitionStats.getComputedVertexCount();
                                    workerSentMessageBytes += partitionStats.getMessageBytesSentCount();
                                    workerSentMessages += partitionStats.getMessagesSentCount();
                                }

                                PartitionStatusReport report = new PartitionStatusReport(workerComputedVertex, workerSentMessages,
                                        workerSentMessageBytes, intervalSecond, superStepSecond, bspService.getHostname(), pw);
                                report.start();
                                break;
                            }
                        }
                    } catch (KeeperException e) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("WorkerMonitorThread: KeeperException - Couldn't get " +
                                    "children of " + finishedWorkerPathDir);
                        }
                    }
                }
                */
                Thread.sleep(200);
            }
            socket.shutdownOutput();
            socket.close();

        } catch (Exception e){
            if(LOG.isInfoEnabled()) {
                LOG.info("WorkerMonitorThread run failed: " + e);
            }
        }
    }

    /**
     * The thread used to report the partition status to the monitor machine
     */
    class PartitionStatusReport extends Thread {
        private long workerComputedVertex;
        private long workerSentMessages;
        private long workerSentMessageBytes;
        private double intervalSecond;
        private double endTime;
        private String hostname;
        private  PrintWriter pw;

        /**
         * Constructor
         * @param workerComputedVertex the number of active vertex
         * @param workerSentMessages the number of sent messages
         * @param workerSentMessageBytes the bytes of sent messages
         * @param intervalSecond the time the current superstep spends
         * @param endTime the time the current superstep ends
         * @param hostname the worker hostname
         * @param pw the printWriter used to send the info to the socket server
         */
        public PartitionStatusReport(long workerComputedVertex, long workerSentMessages,
                                     long workerSentMessageBytes, double intervalSecond, double endTime,
                                     String hostname, PrintWriter pw){
            this.workerComputedVertex = workerComputedVertex;
            this.workerSentMessages = workerSentMessages;
            this.workerSentMessageBytes = workerSentMessageBytes;
            this.intervalSecond = intervalSecond;
            this.endTime = endTime;
            this.hostname = hostname;
            this.pw = pw;
        }

        @Override
        public void run() {
            long currentTime;
            while(((currentTime = System.currentTimeMillis() / 1000) - endTime) < intervalSecond){
                StringBuffer reporter = new StringBuffer();
                reporter.append("giraph." + hostname + ".computedVertex " + workerComputedVertex + " " + (currentTime - intervalSecond));
                reporter.append("giraph." + hostname + ".sentMessages " + workerSentMessages + " " + (currentTime - intervalSecond));
                reporter.append("giraph." + hostname + "sentMessageBytes " + workerSentMessageBytes + " " + (currentTime - intervalSecond));
                pw.println(reporter.toString());
                pw.flush();
            }
        }
    }
}


