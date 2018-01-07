package org.apache.giraph.worker;

import net.iharder.Base64;
import org.apache.giraph.monitor.Monitor;
import org.apache.giraph.partition.PartitionStats;
import org.apache.giraph.utils.WritableUtils;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
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

    private BspServiceWorker<I, V, E> bspServiceWorker;

    public WorkerMonitorThread(BspServiceWorker<I, V, E> bspServiceWorker){
        this.bspServiceWorker = bspServiceWorker;
    }

    @Override
    public void run() {
        try{
            Socket socket = bspServiceWorker.getMonitorSocket();
            String hostname = bspServiceWorker.getHostname();
            final PrintWriter pw = new PrintWriter(socket.getOutputStream());
            long superstep = -2;



            Monitor monitor = new Monitor();
            //the time the current superstep starts
            long startSecond = System.currentTimeMillis() / 1000;
            long superStepSecond;
            long intervalSecond;

            while(!bspServiceWorker.isApplicationFinished()){
                long currentSuperstep = bspServiceWorker.getSuperstep();
                if(superstep != currentSuperstep){
                    superstep = currentSuperstep;
                    startSecond = System.currentTimeMillis() / 1000;
                }

                superStepSecond = System.currentTimeMillis() / 1000;
                String systemStatus = bspServiceWorker.getWorkerSystemStatus(monitor);
                pw.println(systemStatus);
                pw.flush();

                String finishedWorkerPathDir = bspServiceWorker.getWorkerFinishedPath(bspServiceWorker.getApplicationAttempt(),
                        bspServiceWorker.getSuperstep());
                String currentFinishedWorkerPath = finishedWorkerPathDir + bspServiceWorker.getHostnamePartitionId();
                try{
                    List<String> finishedHostnameIdList = bspServiceWorker.getZkExt().getChildrenExt(finishedWorkerPathDir,
                            true,
                            false,
                            false);
                    for(String finishedHostnameId : finishedHostnameIdList){
                        intervalSecond = superStepSecond - startSecond;
                        if(finishedHostnameId.equals(currentFinishedWorkerPath)){
                            long workerSentMessages = 0;
                            long workerSentMessageBytes = 0;
                            long workerComputedVertex = 0;

                            byte[] zkData = bspServiceWorker.getZkExt().getData(finishedHostnameId, false, null);
                            JSONObject workerFinishedInfoObj = new JSONObject(new String(zkData, Charset.defaultCharset()));
                            List<PartitionStats> statsList =
                                    WritableUtils.readListFieldsFromByteArray(
                                            Base64.decode(workerFinishedInfoObj.getString(
                                                    bspServiceWorker.JSONOBJ_PARTITION_STATS_KEY)),
                                            new PartitionStats().getClass(),
                                            bspServiceWorker.getConfiguration());
                            for(PartitionStats partitionStats : statsList){
                                workerComputedVertex += partitionStats.getComputedVertexCount();
                                workerSentMessageBytes += partitionStats.getMessageBytesSentCount();
                                workerSentMessages += partitionStats.getMessagesSentCount();
                            }

                            PartitionStatusReport report = new PartitionStatusReport(workerComputedVertex, workerSentMessages,
                                    workerSentMessageBytes, intervalSecond, superStepSecond, hostname, pw);
                            report.start();
                            break;
                        }
                    }
                } catch (KeeperException e){
                    if(LOG.isInfoEnabled()){
                        LOG.info("WorkerMonitorThread: KeeperException - Couldn't get " +
                        "children of " + finishedWorkerPathDir);
                    }
                }
                Thread.sleep(500);
            }
            socket.shutdownOutput();
            socket.close();

        }catch (Exception e){
            if(LOG.isInfoEnabled()) {
                LOG.info("WorkerMonitorThread: run failed.");
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
        private long intervalSecond;
        private long endTime;
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
                                     long workerSentMessageBytes, long intervalSecond, long endTime,
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


