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
package modreq.commands;

import java.util.Calendar;

import modreq.ModReq;
import modreq.Status;
import modreq.Ticket;
import modreq.korik.SubCommandExecutor;
import modreq.korik.Utils;
import modreq.managers.TicketHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckCommand extends SubCommandExecutor {

    private ModReq plugin;
    private TicketHandler tickets;
    

    public CheckCommand(ModReq instance) {
        plugin = instance;
    }

    @command(maximumArgsLength = 1, permissions = {"modreq.check"}, usage = "/check <page>", description = "shows open tickets")
    public void Integer(CommandSender sender, String[] args) {
        tickets = plugin.getTicketHandler();
        int page = Integer.parseInt(args[0]);
        if (sender instanceof Player) {
            tickets.sendPlayerPage(page, Status.OPEN, (Player) sender);
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }

    }

    @command
    public void Null(CommandSender sender, String[] args) {
        String[] page1 = Utils.addInFront(args, "1");
        Integer(sender, page1);
    }

    @command(minimumArgsLength = 1, maximumArgsLength = 1, usage = "/check id <id>")
    public void id(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            tickets = plugin.getTicketHandler();
            try {
                int id = Integer.parseInt(args[0]);
                if (id > 0 && id <= tickets.getTicketCount()) {
                    Ticket t = tickets.getTicketById(id);
                    t.sendMessageToPlayer((Player) sender);
                }
                else {
                    sender.sendMessage(ModReq.format(ModReq.getInstance().Messages.getString("error.ticket.exist"), "", args[0],""));
                }
            } catch (Exception e) {
        	e.printStackTrace();
                sender.sendMessage(ModReq.format(ModReq.getInstance().Messages.getString("error.number"), "", args[0],""));
            }
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }
    }

    @command(minimumArgsLength = 0, maximumArgsLength = 1, usage = "/check closed <page>")
    public void closed(CommandSender sender, String[] args) {
        tickets = plugin.getTicketHandler();
        int page = 1;
        if (args.length == 1) {
            page = java.lang.Integer.parseInt(args[0]);
        }
        if (sender instanceof Player) {
            tickets.sendPlayerPage(page, Status.CLOSED, (Player) sender);
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }
    }

    @command(minimumArgsLength = 0, maximumArgsLength = 1, usage = "/check claimed <page>")
    public void claimed(CommandSender sender, String[] args) {
        tickets = plugin.getTicketHandler();
        int page = 1;
        if (args.length == 1) {
            page = java.lang.Integer.parseInt(args[0]);
        }
        if (sender instanceof Player) {
            tickets.sendPlayerPage(page, Status.CLAIMED, (Player) sender);
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }
    }

    @command(minimumArgsLength = 0, maximumArgsLength = 1, usage = "/check claimed <page>")
    public void pending(CommandSender sender, String[] args) {
        tickets = plugin.getTicketHandler();
        int page = 1;
        if (args.length == 1) {
            page = java.lang.Integer.parseInt(args[0]);
        }
        if (sender instanceof Player) {
            tickets.sendPlayerPage(page, Status.PENDING, (Player) sender);
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }
    }
    @command(minimumArgsLength = 0, maximumArgsLength = 1, usage = "/check all <page>")
    public void all(CommandSender sender, String[] args) {
        tickets = plugin.getTicketHandler();
        int page = 1;
        if (args.length == 1) {
            page = java.lang.Integer.parseInt(args[0]);
        }
        if (sender instanceof Player) {
            tickets.sendStaffAllPage(page, Status.OPEN, (Player) sender);
        } else {
            sender.sendMessage("This command can only be ran as a player");
        }
    }
    /*
     * Debug purposes
    @command(minimumArgsLength = 0, maximumArgsLength = 1, usage = "/check ticker")
    public void ticker(CommandSender sender, String[] args) {
    	Player[] online = Bukkit.getOnlinePlayers();
        for (int i = 0; i < online.length; i++) {
        	if (online[i].hasPermission("modreq.check")) {
        		Calendar calendar = Calendar.getInstance();
            	int curDay = calendar.get(Calendar.DAY_OF_MONTH);
            	
            	// Let's give everyone a grace period
            	if (curDay <= plugin.getConfig().getInt("days-grace", 0)) {
            		System.out.println("cd: " + curDay);
            		System.out.println("dg: " + plugin.getConfig().getInt("days-grace", 0));
            		System.out.println("within grace");
            		return;
            	}
            	TicketHandler th = plugin.getTicketHandler();
            	int closedTickets = th.getStaffClosedMonth(online[i].getName());
            	int quotaTickets = plugin.getConfig().getInt("ticket-quota", 10);
            	System.out.println("before quota check");
            	
            	if (closedTickets < quotaTickets) {
            		online[i].sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " You are under your monthly ticket quota");
            		online[i].sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " You've completed " +ChatColor.RED+ closedTickets + ChatColor.AQUA+" which is below " + ChatColor.RED+ quotaTickets);
            	}
        	}
        }
    }
    */
    @command(
			minimumArgsLength = 1,
			maximumArgsLength = 1,
			permissions = {"modreq.check"},
			usage = "/check staff <player>")
	public void staff(CommandSender sender, String[] args) {
		tickets = plugin.getTicketHandler();
	    if(sender instanceof Player) {
	    	tickets.getStaffClosed(args[0], (Player) sender);
	    }else {
		sender.sendMessage("This command can only be ran as a player");
	    }
	}
    
    @command(
			minimumArgsLength = 0,
			maximumArgsLength = 0,
			permissions = {"modreq.check"},
			usage = "/check myclosed")
	    public void myclosed (CommandSender sender, String[] args) {
			tickets = plugin.getTicketHandler();
			if(sender instanceof Player) {
					int myClosed = tickets.getStaffClosedMonth(sender.getName().toString(), (Player) sender);
					sender.sendMessage("You've closed: " + myClosed + " tickets this month");
					return;
			}else {
					sender.sendMessage("This command can only be ran as a player");
					return;
			}
		}
    
    /*
    @command(minimumArgsLength = 1, maximumArgsLength = 1, usage = "/check stafftickets <staff>")
    public void stafftickets(CommandSender sender, String[] args) {
    	System.out.println("staff requesting: " + sender.getName().toLowerCase());
    	if (sender instanceof Player) {
    		if (((Player) sender).getName().toLowerCase() == "nickewa") {
    			tickets = plugin.getTicketHandler();
    			System.out.println(tickets);
    			
    			int closedTickets = tickets.getStaffClosedMonth(args[0]);
    			sender.sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " " + args[0]);
        		sender.sendMessage(ChatColor.GOLD + "[ModReq]" + ChatColor.AQUA + " You've completed " +ChatColor.RED+ closedTickets);
        	
    		}
    	}
    }
    */
}
