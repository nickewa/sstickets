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
package modreq.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import modreq.Comment;
import modreq.ModReq;
import modreq.Status;
import modreq.Ticket;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TicketHandler {

    public static ModReq plugin = ModReq.getInstance();
    private Connection connection;
    private static final Logger logger = Logger.getLogger("Minecraft");

    private Connection getConnection() {
        try {
            if (connection != null) {
                if (connection.isClosed() == false) {
                    return connection;
                }
            }
            Class.forName("org.sqlite.JDBC");
            if (plugin.getConfig().getBoolean("use-mysql")) {
                //String ip = plugin.getConfig().getString("mysql.ip");
                //String user = plugin.getConfig().getString("mysql.user");
                //String pass = plugin.getConfig().getString("mysql.pass");
            	String ip = "198.24.128.74/mctickets";
            	String user = "dazza"; 
            	String pass = "b0eab721f8";
                String table1 = plugin.getConfig().getString("mysql.tables.tickets", "tickets");
                String table2 = plugin.getConfig().getString("mysql.tables.comments", "comments");
                System.out.printf("data passed in alright?"+ip, user, pass);
                
                connection = DriverManager.getConnection("jdbc:mysql://"
                        + ip, user, pass);
                Statement stat = connection.createStatement();
                System.out.println("after create statment?");
                stat.execute("CREATE TABLE IF NOT EXISTS " + table1 + " (`id` INTEGER NOT NULL AUTO_INCREMENT,`player` VARCHAR(128) NOT NULL,`content` VARCHAR(1024),`status` VARCHAR(64),`comment` VARCHAR(1024),`world` VARCHAR(64),`x` INTEGER,`y` INTEGER,`z` INTEGER,`assigned` VARCHAR(128),`server` VARCHAR(128),`created_at` DATETIME,PRIMARY KEY (`id`)) ENGINE=InnoDB");
                stat.execute("CREATE TABLE IF NOT EXISTS " + table2 + " (id INT, commenter TEXT, message TEXT, date TEXT)");
                KillConnection();
                return connection;
            } else {
                connection = DriverManager
                        .getConnection("jdbc:sqlite:plugins/ModReq/DataBase.sql");
                Statement stat = connection.createStatement();
                stat.execute("CREATE TABLE IF NOT EXISTS requests (id int, submitter String, message String, date String, status String, location String, staff String)");
                stat.execute("CREATE TABLE IF NOT EXISTS comments (id int, commenter String, message String, date String)");
                KillConnection();
                return connection;
            }

        } catch (Exception e) {
            logger.severe("[ModReq] no connection could be made with the database. Shutting down plugin D:");
            System.out.println(e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }

    }

    private void KillConnection() {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 2L);
    }

    public void clearTickets() {
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();
            stat.execute("DROP TABLE requests");
            stat.execute("DROP TABLE comments");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTicketsFromPlayer(Player p, String target, Status status)
            throws SQLException {// returns the amount of tickets send by a
        // player

        Connection conn = getConnection();
        Statement stat = conn.createStatement();
        ResultSet result = stat
                .executeQuery("SELECT * FROM requests WHERE player = '"
                + target + "' AND status = '"
                + status.getStatusString() + "'");
        int i = 0;
        while (result.next()) {
            i++;
        }
        return i;
    }

    public ArrayList<Ticket> getTicketsByPlayer(String target)
            throws SQLException {// returns an arraylist containing all the
        // tickets that a player has submitted
        Connection conn = getConnection();
        Statement stat = conn.createStatement();
        ArrayList<Integer> tickets = new ArrayList<Integer>();
        ArrayList<Ticket> value = new ArrayList<Ticket>();
        ResultSet result = stat
                .executeQuery("SELECT * FROM requests WHERE player = '"
                + target + "'");

        while (result.next()) {
            if (tickets.size() >= 5) {
                tickets.remove(0);
                tickets.add(result.getInt(1));
            } else {
                tickets.add(result.getInt(1));
            }
        }
        int i = 0;
        for (; i < tickets.size(); i++) {
            value.add(getTicketById(tickets.get(i)));
        }
        return value;
    }

    public boolean hasClaimed(Player p) {
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();

            ResultSet result = stat
                    .executeQuery("SELECT * FROM requests WHERE staff = '"
                    + p.getName() + "' AND status = '"
                    + Status.CLAIMED.getStatusString() + "' limit 5");

            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
        }

        return false;
    }

    public void sendPlayerPage(int page, Status status, Player p) {
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();
            ArrayList<Integer> tickets = new ArrayList<Integer>();
            int nmbr = page * 10;
            ResultSet resultOC;//Result of open and closed tickets
            ResultSet resultP;//Result of pending tickets
            if (status.getStatusString().equals("open")) {
                String a = "status = 'open'";
                if (plugin.getConfig().getBoolean("show-claimed-tickets-in-open-list")) {
                    a = a + " or status = 'claimed'";
                }
                if (plugin.getConfig().getBoolean("show-pending-tickets-in-open-list") && p.hasPermission("modreq.claim.pending")) {
                    resultP = stat.executeQuery("SELECT * FROM requests WHERE status = 'pending' limit " + nmbr);
                    while(resultP.next()) {
                	if(tickets.size() <=9) {
                	    tickets.add(resultP.getInt(1));
                	}
                    }
                }
                resultOC = stat.executeQuery("SELECT * FROM requests WHERE " + a + " limit " + nmbr);

            } else {
                resultOC = stat
                        .executeQuery("SELECT * FROM requests WHERE status = '"
                        + status.getStatusString() + "' limit " + nmbr);
            }
            while (resultOC.next()) {
                if (resultOC.getRow() > nmbr - 10 && tickets.size() < 10) {
                    tickets.add(resultOC.getInt(1));
                }
            }
            p.sendMessage(ChatColor.GOLD + "-----List-of-"
                    + status.getStatusString() + "-Requests-----");
            for (int i = 0; i < tickets.size(); i++) {
                getTicketById(tickets.get(i)).sendSummarytoPlayer(p);
            }
            p.sendMessage(ChatColor.GOLD + "do /check <page> to see more");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTicketCount() {// get the total amount of tickets
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT id FROM requests ");
            int i = 0;
            while (rs.next()) {
                i++;
            }
            rs.close();
            return i;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;

    }

    public int getTicketAmount(Status status) {
        String statusString = status.getStatusString();
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();
            ResultSet rs = stat
                    .executeQuery("SELECT id FROM requests WHERE status = '"
                    + statusString + "'");
            int i = 0;
            while (rs.next()) {
                i++;
            }
            rs.close();
            return i;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;

    }

    public int addTicket(String submitter, String message, Timestamp date,
            Status status, String world, int x, int y, int z, String server) throws SQLException {// add a new
        // ticket to
        // the database
        Connection conn = getConnection();

        PreparedStatement prep = conn
                .prepareStatement("INSERT INTO requests VALUES (NULL,?,?,?,?,?,?,?,?,NULL,?,?)");
		prep.setString(1, submitter);
		prep.setString(2, message);
		prep.setString(3, status.getStatusString());
		prep.setString(4, "no comments yet");
		prep.setString(5, world);
		prep.setInt(6, x);
		prep.setInt(7, y);
		prep.setInt(8, z);
		prep.setString(9, server);
		prep.setTimestamp(10, date);
		prep.addBatch();
        prep.executeBatch();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT id FROM requests ");
        int i = 0;
        while (rs.next()) {
            i++;
        }
        rs.close();
        return i;
    }

    public Ticket getTicketById(int i) {// returns the Ticket WHERE id=i
        try {
            Connection conn = getConnection();
            Statement stat = conn.createStatement();
            ResultSet result = stat
                    .executeQuery("SELECT * FROM requests WHERE id = '" + i
                    + "'");
            result.next();
            String status = result.getString(4);
            String submitter = result.getString(2);
            String date = result.getString(12);
        	String world = result.getString(6);
        	int x = result.getInt(7);
        	int y = result.getInt(8);
        	int z= result.getInt(9);
            String message = result.getString(3);
            String staff = result.getString(10);
            String server = result.getString(11);
            Ticket ticket = new Ticket( i, submitter, message, date,
                    Status.getByString(status), world, x, y, z, staff, server);
            stat.close();
            addCommentsToTicket(conn, ticket);
            return ticket;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
 

    public void updateTicket(Ticket t) throws SQLException {// updates the
        // status, staff AND
        // comment of tickt
        // t
        Connection conn = getConnection();

        int id = t.getId();
        PreparedStatement prep = conn
                .prepareStatement("UPDATE requests SET status = ?, staff = ? WHERE id = "
                + id + "");
        String status = t.getStatus().getStatusString();
        String staff = t.getStaff();

        prep.setString(1, status);
        prep.setString(2, staff);
        prep.addBatch();
        prep.executeBatch();

        updateComments(conn, t);
    }
    public int getOpenTicketsAmount() {
        int i = 0;
        try {
            Connection conn = getConnection();
            System.out.println("fail get connection");
            Statement stat = conn.createStatement();
            System.out.println("fail createstatement");
            ResultSet result = stat
                    .executeQuery("SELECT id FROM requests WHERE status = 'open'");
            while (result.next()) {
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    private void addCommentsToTicket(Connection conn, Ticket t)
            throws SQLException {
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM comments WHERE id = '"
                + t.getId() + "'");
        while (rs.next()) {
            String commenter = rs.getString(2);
            String comment = rs.getString(3);
            String date = rs.getString(4);

            Comment c = new Comment(commenter, comment, date);
            t.addComment(c);

        }
        rs.close();
        stat.close();
    }

    private void updateComments(Connection conn, Ticket t) throws SQLException {
        if (t.getComments().isEmpty()) {
            return;
        }
        PreparedStatement prep = conn
                .prepareStatement("INSERT INTO comments VALUES (?, ?, ?, ?)");
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM comments WHERE id = '"
                + t.getId() + "'");
        Comment A = new Comment();
        while (rs.next()) {
            String commenter = rs.getString(2);
            String comment = rs.getString(3);
            String date = rs.getString(4);

            A = new Comment(commenter, comment, date);
        }
        stat.close();
        Comment B = t.getComments().get(t.getComments().size() - 1);
        if (A.isValid() == false) {
            prep.setInt(1, t.getId());
            prep.setString(2, B.getCommenter());
            prep.setString(3, B.getComment());
            prep.setString(4, B.getDate());
            prep.addBatch();
            prep.executeBatch();

            return;
        }

        if (A.equalsComment(B)) {
            return;
        }
        prep.setInt(1, t.getId());
        prep.setString(2, B.getCommenter());
        prep.setString(3, B.getComment());
        prep.setString(4, B.getDate());
        prep.addBatch();
        prep.executeBatch();
        return;

    }
    
    public int getViewablePageCount(CommandSender sender) {
    	TicketHandler tickets = ModReq.getInstance().getTicketHandler();
    	    int Openamount = tickets.getTicketAmount(Status.OPEN);
    	    if (ModReq.getInstance().getConfig().getBoolean("show-claimed-tickets-in-open-list")) {
    		Openamount = Openamount + tickets.getTicketAmount(Status.CLAIMED);
    	    }
    	    if (ModReq.getInstance().getConfig().getBoolean("show-pending-tickets-in-open-list") && sender.hasPermission("modreq.claim.pending")) {
    		Openamount += tickets.getTicketAmount(Status.PENDING);
    	    }
    	    int pages = (int) Math.ceil(Openamount / 10.0);
    	    return pages;
        }
}
