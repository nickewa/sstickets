/*
 Modreq Minecraft/Bukkit server ticket system
 Copyright (C) 2013 Sven Wiltink

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package modreq;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import modreq.Metrics.Graph;
import modreq.managers.CommandManager;
import modreq.managers.TicketHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ModReq extends JavaPlugin {

    public CommandManager cmdManager;
    public YamlConfiguration Messages;
    private static ModReq plugin;
    public static final Logger logger = Logger.getLogger("Minecraft");
    public File configFile;
    private File messages;
    private Metrics metrics;
    private String currentVersion;
    public String latestVersion;
    public String DownloadLink;
    private TicketHandler ticketHandler;

    @Override
    public void onEnable() {
        plugin = this;
        cmdManager = new CommandManager(this);
        ticketHandler = new TicketHandler();
        messages = new File(getDataFolder().getAbsolutePath() + "/messages.yml");
        
        checkConfigFile();
        loadMessages();

        cmdManager.initCommands();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new ModReqListener(this), this);

        PluginDescriptionFile pdfFile = this.getDescription();
        currentVersion = pdfFile.getVersion();

        if (plugin.getConfig().getBoolean("check-updates", true)) {
            startVersionChecker();
        } else {
            logger.info("[ModReq] Not using update feature");
        }
        startNotify();
        startMetrics();

        logger.log(Level.INFO, "{0} version {1} is enabled.", new Object[]{pdfFile.getName(), currentVersion});
    }
    @Override
    public void onDisable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        logger.log(Level.INFO, "{0} is now disabled ", pdfFile.getName());
    }
    private void startMetrics() {
        if (ModReq.plugin.getConfig().getBoolean("metrics")) {
            try {
                metrics = new Metrics(ModReq.plugin);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startGraphs();
            logger.info("[ModReq] Using metrics");
        }

    }
    public void checkConfigFile() {
        configFile = new File(getDataFolder().getAbsolutePath() + "/config.yml");
        if (!configFile.exists()) {
            firstrun();
        }
        try {	
        	YamlConfiguration pluginYML = YamlConfiguration.loadConfiguration(this
        			.getResource("plugin.yml"));
        	if (!pluginYML.getString("config-version", "failed-to-read-version").equals(
                getConfig().getString("version"))) {
        		logger.info("[ModReq] Your plugin version does not match the config version. Please visit the bukkitdev page for more information");
        	}
        } catch (Exception e) {
        	// Something went very wrong, let's log it.
        	logger.warning("[ModReq] Unable to compare config version with plugin version");
        }
    }
    public void reload() {
        messages = new File(getDataFolder().getAbsolutePath() + "/messages.yml");
        configFile = new File(getDataFolder().getAbsolutePath() + "/config.yml");
        if (!configFile.exists()) {
            firstrun();
        }
        YamlConfiguration pluginYML = YamlConfiguration.loadConfiguration(this
                .getResource("plugin.yml"));
        if (!pluginYML.getString("config-version").equals(
                getConfig().getString("version"))) {
            logger.info("[ModReq] Your plugin version does not match the config version. Please visit the bukkitdev page for more information");
        }
        loadMessages();
        ticketHandler = new TicketHandler();
        plugin.reloadConfig();

    }
    public String getCurrentVersion() {
        return currentVersion;
    }
    public TicketHandler getTicketHandler() {
        return ticketHandler;
    }
    private void startVersionChecker() {
        long hour = 60 * 60 * 20;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                new VersionChecker(this), 60L, hour);
    }
    private void startGraphs() {
        metrics.start();
        try {// test chart
            Metrics metrics = new Metrics(plugin);
            Graph graph = metrics.createGraph("Tickets");
            graph.addPlotter(new Metrics.Plotter("Open") {
                @Override
                public int getValue() {
                    return ticketHandler.getTicketAmount(Status.OPEN);
                }
            });
            graph.addPlotter(new Metrics.Plotter("Claimed") {
                @Override
                public int getValue() {
                    return ticketHandler.getTicketAmount(Status.CLAIMED);
                }
            });
            graph.addPlotter(new Metrics.Plotter("Closed") {
                @Override
                public int getValue() {
                    return ticketHandler.getTicketAmount(Status.CLOSED);
                }
            });
            metrics.start();
        } catch (IOException e) {
        }

    }
    private boolean checkTranslate() {
        if (!messages.exists()) {
            return false;
        } else {
            return true;
        }
    }
    private void loadMessages() {
        logger.info("[ModReq] Looking for messages.yml");
        if (checkTranslate()) {
            logger.info("[ModReq] messages.yml found. Trying to load messages.yml");
            Messages = YamlConfiguration.loadConfiguration(messages);
            logger.info("[ModReq] messages.yml loaded!");
        } else {
            logger.info("[ModReq] messages.yml not found, using default messages");
            saveDefaultMessages();
            Messages = getDefaultMessages();
        }
    }
    private void saveDefaultMessages() {
        plugin.saveResource("messages.yml", true);
    }
    public YamlConfiguration getDefaultMessages() {
        YamlConfiguration pluginYML = YamlConfiguration.loadConfiguration(this
                .getResource("messages.yml"));
        return pluginYML;
    }
    private void startNotify() {
        if (this.getConfig().getBoolean("notify-on-time")) {
            logger.info("[ModReq] Notifying on time enabled");
            long time = this.getConfig().getLong("time-period");
            time = time * 1200;
            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    TicketHandler th = getTicketHandler();
                    int opentickets = th.getOpenTicketsAmount();
                    if (opentickets > 0) {
                        Player[] online = Bukkit.getOnlinePlayers();
                        for (int i = 0; i < online.length; i++) {
                            if (online[i].hasPermission("modreq.check")) {
                                online[i].sendMessage(ChatColor.GOLD
                                        + "[ModReq]"
                                        + ChatColor.GREEN
                                        + Integer.toString(opentickets)
                                        + " "
                                        + plugin.Messages
                                        .getString("notification",
                                        "open tickets are waiting for you"));
                            }
                        }
                    }
                    
                    // Let's notify under performing staff
                    /*
                    Player[] online = Bukkit.getOnlinePlayers();
                    for (int i = 0; i < online.length; i++) {
                    	if (online[i].hasPermission("modreq.check") && (!online[i].hasPermission("modreq.exempt"))) {
                    		Calendar calendar = Calendar.getInstance();
                        	int curDay = calendar.get(Calendar.DAY_OF_MONTH);
                        	
                        	// Let's give everyone a grace period
                        	if (curDay <= plugin.getConfig().getInt("days-grace", 7))
                        		return;
                        	
                        	int closedTickets = th.getStaffClosedMonth(online[i].getName());
                        	int quotaTickets = plugin.getConfig().getInt("ticket-quota", 10);
                        	
                        	if (closedTickets < quotaTickets) {
                        		online[i].sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " You are under your monthly ticket quota");
                        		online[i].sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " You've completed " +ChatColor.RED+ closedTickets + ChatColor.AQUA+" which is below " + ChatColor.RED+ quotaTickets);
                        	}
                    	}
                    }
                    */ 
                    // removed for now.
                    
                    
                }
            }, 60L, time);
        }
    }
    private void firstrun() {
        this.saveDefaultConfig();
    }
    public static ModReq getInstance() {
        return plugin;
    }
    public static String getTimeString() {
    	
    	//String timezone = ModReq.getInstance().getConfig()
        //        .getString("timezone");
        //DateFormat df = new SimpleDateFormat(ModReq.getInstance().getConfig()
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        //        .getString("timeformat", "YY-MM-dd HH:mm:ss"));
        //TimeZone tz = TimeZone.getTimeZone(timezone);

        //Calendar cal = Calendar.getInstance(Calendar.getInstance()
        //        .getTimeZone(), Locale.ENGLISH);
        //cal.add(Calendar.MILLISECOND, -(cal.getTimeZone().getRawOffset()));
        //cal.add(Calendar.MILLISECOND, tz.getRawOffset());
        //Date dt = new Date(cal.getTimeInMillis());
        Date dt = new Date();
        return df.format(dt);
    }
    public static String format(String input, String player, String number, String comment){
        input = input.replace("&player", player);
        input = input.replace("&number", number);
        input = input.replace("&comment", comment);
	input = ChatColor.translateAlternateColorCodes('&', input);
        return input;    
    }
}