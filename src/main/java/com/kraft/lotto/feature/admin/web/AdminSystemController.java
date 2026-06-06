package com.kraft.lotto.feature.admin.web;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/ops/system")
public class AdminSystemController {

    @GetMapping
    public String systemPage(Model model) {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        model.addAttribute("heapUsedMb", heapUsed / 1024 / 1024);
        model.addAttribute("heapMaxMb", heapMax / 1024 / 1024);
        model.addAttribute("heapUsedPct", heapMax > 0 ? (int) (heapUsed * 100L / heapMax) : 0);
        model.addAttribute("nonHeapMb", memBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
        model.addAttribute("threadCount", threadBean.getThreadCount());
        model.addAttribute("peakThreadCount", threadBean.getPeakThreadCount());
        model.addAttribute("daemonThreadCount", threadBean.getDaemonThreadCount());
        model.addAttribute("uptimeMs", runtimeBean.getUptime());
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("osName", osBean.getName() + " " + osBean.getVersion());
        model.addAttribute("availableProcessors", osBean.getAvailableProcessors());

        return "admin/system";
    }
}
