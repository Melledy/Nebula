package emu.nebula.command.commands;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

import java.io.File;
import java.lang.management.ManagementFactory;

@Command(label = "status", permission = "admin.status", desc = "!status = Displays server runtime status.")
public class StatusCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        var sb = new StringBuilder();

        if (args.getRaw().contains("@")) {
            if (args.getTarget() == null || !args.getTarget().hasSession()) {
                return "Error - Targeted player not found or offline";
            }
        }

        var runtime = Runtime.getRuntime();
        var uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        double usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1048576.0;
        double maxMem = runtime.maxMemory() / 1048576.0;

        int players = 0;
        if (Nebula.getGameContext() != null) {
            players = Nebula.getGameContext().getPlayerModule().getCachedPlayers().size();
        }

        var http = Nebula.getHttpServer();
        String addr = "-";
        String scheme = "http";
        int port = 0;
        if (http != null) {
            addr = http.getServerConfig().getPublicAddress();
            port = http.getServerConfig().getPublicPort();
            scheme = http.getServerConfig().isUseSSL() ? "https" : "http";
        }

        sb.append("Server Status\n");
        sb.append("Git: ").append(Nebula.getGitHash()).append('\n');
        sb.append("Game: ").append(GameConstants.getGameVersion()).append('\n');
        sb.append("HTTP: ").append(scheme).append("://").append(addr).append(":").append(port).append('\n');
        sb.append("Uptime: ").append(formatUptime(uptimeMs)).append('\n');
        sb.append("CPU: ").append(getCpu()).append('\n');
        sb.append("Load: ").append(getLoad()).append('\n');
        sb.append("Disk: ").append(getDisk()).append('\n');
        sb.append("Memory: ").append(String.format("%.1fMB (max %.1fMB)", usedMem, maxMem)).append('\n');
        sb.append("Players: ").append(players);

        return sb.toString();
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02dh:%02dm:%02ds", h, m, sec);
    }

    private static String getCpu() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        double v = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            v = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad();
        }
        return v >= 0 ? String.format("%.1f%%", v * 100.0) : "-";
    }

    private static String getLoad() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        double v = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean csm) {
            v = csm.getCpuLoad();
        }
        if (v < 0) {
            double avg = os.getSystemLoadAverage();
            if (avg > 0) {
                int cores = Runtime.getRuntime().availableProcessors();
                v = Math.min(1.0, avg / cores);
            }
        }
        return v >= 0 ? String.format("%.1f%%", v * 100.0) : "-";
    }

    private static String getDisk() {
        var root = new File(".");
        long total = root.getTotalSpace();
        long usable = root.getUsableSpace();
        if (total <= 0) return "-";
        double usedPercent = (double) (total - usable) / (double) total * 100.0;
        return String.format("%.1f%%", usedPercent);
    }
}
