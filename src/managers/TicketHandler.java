package managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import modreq.Status;
import modreq.Ticket;
import modreq.modreq;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TicketHandler {
	public static modreq plugin = (modreq) Bukkit.getPluginManager().getPlugin("ModReq");
	public final Logger logger = Logger.getLogger("Minecraft");
	
	private Connection getConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			if(plugin.getConfig().getBoolean("use-mysql")) {
				String ip = plugin.getConfig().getString("mysql.ip");
				String user = plugin.getConfig().getString("mysql.user"); 
				String pass = plugin.getConfig().getString("mysql.pass");
				
				Connection conn = DriverManager.getConnection("jdbc:mysql://"+ip, user, pass);
				Statement stat = conn.createStatement();
				stat.execute("CREATE TABLE IF NOT EXISTS ticket ( `id` INTEGER NOT NULL AUTO_INCREMENT,`player` VARCHAR(128) NOT NULL,`content` VARCHAR(1024),`status` VARCHAR(64),`comment` VARCHAR(1024),`world` VARCHAR(64),`x` INTEGER,`y` INTEGER,`z` INTEGER,`assigned` VARCHAR(128),`server` VARCHAR(128),`created_at` DATETIME,PRIMARY KEY (`id`)) ENGINE=InnoDB");
				return conn;
			}
			else {
				Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/ModReq/DataBase.sql");
				Statement stat = conn.createStatement();
				stat.execute("CREATE TABLE IF NOT EXISTS ticket (id int, submitter String, message String, date String, status String, comment String, location String, staff String, server String)");
				return conn;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("[ModReq] no connection could be made with the database. Shutting down plugin D:");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			return null;
		}
	}
	public void clearTickets() {
		try {
			Connection conn = getConnection();
			Statement stat = conn.createStatement();
			stat.execute("DROP TABLE ticket");
		}
		catch(Exception e) {
			
		}
	}
	public int getTicketsFromPlayer(Player p, String target, Status status) throws SQLException {//returns the amount of tickets send by a player
		
		Connection conn = getConnection();
		Statement stat = conn.createStatement();
		
		ArrayList<Integer> tickets = new ArrayList<Integer>();
		ResultSet result = stat.executeQuery("SELECT * FROM ticket WHERE player = '"+target+"' AND status = '"+status.getStatusString()+"' AND server = '"+Bukkit.getServerName()+"'");
		while(result.next()) {
			
				tickets.add(result.getInt(1));
			
		}
		int i=0;
		for(; i<tickets.size(); i++) {
			
		}
		conn.close();
		return i;
	}
	public ArrayList<Ticket> getTicketsByPlayer(Player p, String target) throws SQLException{//returns an arraylist containing all the tickets that a player has submitted
		Connection conn = getConnection();
		Statement stat = conn.createStatement();		
		ArrayList<Integer> tickets = new ArrayList<Integer>();
		ArrayList<Ticket> value = new ArrayList<Ticket>();
		ResultSet result = stat.executeQuery("SELECT * FROM ticket WHERE player = '"+target+"' AND server = '"+Bukkit.getServerName()+"'");
		
		
		while(result.next()) {
			if(tickets.size() >= 5) {
				tickets.remove(0);
				tickets.add(result.getInt(1));
			}
			else {
				tickets.add(result.getInt(1));
			}	
			
		}
		int i=0;
		for(; i<tickets.size(); i++) {
			value.add(getTicketById(tickets.get(i)));
		}
		conn.close();
		return value;
	}
	public boolean hasClaimed(Player p) {
		try {
		Connection conn = getConnection();
		Statement stat = conn.createStatement();p.getName();
		
		
		ResultSet result = stat.executeQuery("SELECT * FROM ticket WHERE assigned = '"+p.getName()+"' AND status = '"+Status.CLAIMED.getStatusString()+ "' AND server = '"+Bukkit.getServerName()+"' limit 5");
		
			if(result.next()) {
				return true;
			}
		conn.close();
		} catch (SQLException e) {
		}

		return false;
	}
	public void sendPlayerPage(int page, Status status, Player p) throws ParseException {//send the -----List-of-STATUS-Requests----- 
		try {
		    	Connection conn = getConnection();
			Statement stat = conn.createStatement();
			ArrayList<Integer> tickets = new ArrayList<Integer>();
			int nmbr = page *10;
			ResultSet result;
			if(status.getStatusString().equals("open")) {
				if(plugin.getConfig().getBoolean("show-claimed-tickets-in-open-list") == true) {
				    	result = stat.executeQuery("SELECT * FROM ticket WHERE status = 'open' AND server = '"+Bukkit.getServerName()+"' or status = 'claimed' limit "+nmbr);
				}
				else {
					result = stat.executeQuery("SELECT * FROM ticket WHERE status = 'open' AND server = '"+Bukkit.getServerName()+"' limit "+nmbr);
					}
			}
			else {
			result = stat.executeQuery("SELECT * FROM ticket WHERE status = '"+status.getStatusString()+"' AND server = '"+Bukkit.getServerName()+"' limit "+nmbr);
			}
			while(result.next()) {
				if(result.getRow() > nmbr-10) {
					tickets.add(result.getInt(1));
				}
			}
			p.sendMessage(ChatColor.GOLD+"-----List-of-"+status.getStatusString()+"-Requests-----");
			for(int i=0; i<tickets.size(); i++) {
				getTicketById(tickets.get(i)).sendSummarytoPlayer(p);
			}
			p.sendMessage(ChatColor.GOLD + "do /check <page> to see more");
			conn.close();
			return;
		} catch (SQLException e) {
			e.printStackTrace();
		
		
		}	
	
	}
    public int getTicketCount() {//get the total amount of tickets
		try {
			Connection conn = getConnection();
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("SELECT id FROM ticket");
			int i = 0;
			while(rs.next()) {
			 i++;
			}
			rs.close();
			conn.close();
			return i;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
		
	}
    public int getTicketAmount(Status status) {
    	String statusString = status.getStatusString();
    	try {
    		Connection conn = getConnection();
    		Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("SELECT id FROM ticket WHERE status = '"+statusString+"' AND server = '"+Bukkit.getServerName()+"'");
			int i = 0;
			while(rs.next()) {
			 i++;
			}
			rs.close();
			conn.close();
			return i;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
    	
    }
	public void addTicket(String submitter, String serverr, String message, java.sql.Timestamp date, Status status, String world, int x, int y, int z) throws SQLException {//add a new ticket to the database
		Connection conn = getConnection();
		PreparedStatement prep = conn.prepareStatement("INSERT INTO ticket VALUES (NULL,?,?,?,?,?,?,?,?,NULL,?,?)");
		prep.setString(1, submitter);
		prep.setString(2, message);
		prep.setString(3, status.getStatusString());
		prep.setString(4, "no comments yet");
		prep.setString(5, world);
		prep.setInt(6, x);
		prep.setInt(7, y);
		prep.setInt(8, z);
		prep.setString(9, serverr);
		prep.setTimestamp(10, date);
		prep.addBatch();
		
		prep.executeBatch(); 
		conn.close();
		
	}
	public Ticket getTicketById(int i) {//returns the Ticket WHERE id=i
			try {
				Connection conn = getConnection();
				Statement stat = conn.createStatement();
				
				ResultSet result = stat.executeQuery("SELECT * FROM ticket WHERE id = '"+i+"'");
				result.next();
				String status = result.getString(4);
				String submitter = result.getString(2);
				String date = result.getString(12);
				String world = result.getString(6);
				int x = result.getInt(7);
				int y = result.getInt(8);
				int z= result.getInt(9);
				String message = result.getString(3);
				String comment = result.getString(5);
				String staff = result.getString(10);
				String server = result.getString(11);
				Ticket ticket = new Ticket(plugin,i, submitter, server, message, date, Status.getByString(status), comment, world, x, y, z, staff);
				result.close();
				conn.close();
				return ticket;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			
			
			}	
			
		return null;
		
	}
	public void updateTicket(Ticket t) throws SQLException {//updates the status, staff AND comment of tickt t
		Connection conn = getConnection();
		
		int id = t.getId();
		PreparedStatement prep = conn.prepareStatement("UPDATE ticket SET status = ?, assigned = ?, comment = ? WHERE id = "+id+"");
		
		String status = t.getStatus().getStatusString();
		String comment = t.getComment();
		String staff = t.getStaff();
		
		prep.setString(1, status);
		prep.setString(2, staff);
		prep.setString(3, comment);
		prep.addBatch();
		prep.executeBatch();
		conn.close();
	}
	public int getOpenTicketsAmount() {
		int i = 0;
			try {
				Connection conn = getConnection();
				Statement stat = conn.createStatement();
				ResultSet result = stat.executeQuery("SELECT id FROM ticket WHERE status = 'open' AND server = '"+Bukkit.getServerName()+"'");
				while(result.next()) {
					i++;
				}
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			
			
			}	
		return i;
	}
	public String getStaffTicketsAmount(String staffname, Player p) {
		int i = 0;
			try {
				Connection conn = getConnection();
				Statement stat = conn.createStatement();
				ResultSet result = stat.executeQuery("SELECT id FROM ticket WHERE assigned = '"+staffname+"' AND status = '"+Status.CLOSED.getStatusString()+ "'");
				while(result.next()) {
					i++;
				}
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			
			
			}
			String test = Integer.toString(i);
			p.sendMessage(ChatColor.GOLD+""+staffname+" has completed "+test+" ticket on "+ChatColor.RED+"ALL"+ChatColor.GOLD+" servers");
			int c = 0;
			try {
				Connection conn = getConnection();
				Statement stat = conn.createStatement();
				ResultSet result = stat.executeQuery("SELECT id FROM ticket WHERE assigned = '"+staffname+"' AND server = '"+Bukkit.getServerName()+"' AND status = '"+Status.CLOSED.getStatusString()+ "'");
				while(result.next()) {
					c++;
				}
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			
			
			}
			String test1 = Integer.toString(c);
			p.sendMessage(ChatColor.GOLD+""+staffname+" has completed "+test1+" tickets on THIS server");
		return test;
	

		
	}
}
