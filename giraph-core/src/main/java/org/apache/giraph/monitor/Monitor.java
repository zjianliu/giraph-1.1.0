package org.apache.giraph.monitor;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.apache.giraph.utils.SigarUtil;

import java.util.Date;

public class Monitor {
    private Sigar sigar;

    public Monitor() throws Exception{
        sigar = SigarUtil.getSigar();
    }

    public Metrics getMetrics() throws SigarException {
        double cpuUser = sigar.getCpuPerc().getUser();
        double memoryUsed = sigar.getMem().getActualUsed();
        long memoryTotal = sigar.getMem().getTotal();
        int totalNetworkup = sigar.getNetStat().getAllOutboundTotal();
        int totalNetworkdown = sigar.getNetStat().getAllInboundTotal();
        Date time = new Date();

        return new Metrics(cpuUser, memoryUsed, memoryTotal, totalNetworkup, totalNetworkdown, time);
    }
}
