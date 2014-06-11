/*
 * BunnyEmu - A Java WoW sandbox/emulator
 * https://github.com/marijnz/BunnyEmu
 */
package bunnyEmu.main;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.UIManager;

import bunnyEmu.main.db.DatabaseConnection;
import bunnyEmu.main.handlers.ConfigHandler;
import bunnyEmu.main.net.Connection;
import bunnyEmu.main.net.LogonConnection;
import bunnyEmu.main.utils.Logger;

/**
 * 
 * To login: Run and login with a WoW client with any username but make sure to
 * use the password: "password".
 * 
 * @author Marijn
 */
public class Server {

	public static String realmlist = null;
	
	public static Properties prop = null;

	private ServerSocket serverSocket;
	private ArrayList<Connection> connections = new ArrayList<Connection>(10);

	public static void main(String[] args) {
		try {
			prop = ConfigHandler.loadProperties();
			
			realmlist = prop.getProperty("realmlistAddress");

			if (realmlist.isEmpty()) {
				Logger.writeError("No realmlist set in server.conf, unable to start.");
				System.exit(0);
			}
			
			/* does user want a GUI */
			if (Integer.parseInt(prop.getProperty("enableGUI")) != 0) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				ServerWindow.create();
				Thread.sleep(200);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		new Server().launch();
	}

	public void launch() {
		//RealmHandler.addRealm(new Realm(1, "Server test 1", "31.220.24.8", 3344, 1));
		listenSocket();
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	private void listenSocket() {
		try {
			System.out.println("Launched BunnyEmu - listening on " + realmlist);
			
			InetAddress address = InetAddress.getByName(realmlist);
			serverSocket = new ServerSocket(3724, 0, address);

			/* load database connection */
			DatabaseConnection.initConnectionPool(prop);
			
			System.out.println("BunnyEmu is open-source: https://github.com/marijnz/BunnyEmu");
			System.out.println("Remember to create an account before logging in.");
			
			/* console commands are handled by this thread if no GUI */
			if (Integer.parseInt(prop.getProperty("enableGUI")) == 0) {
				Runnable loggerRunnable = new ConsoleLoggerCMD();
				Thread loggerThread = new Thread(loggerRunnable);
				loggerThread.start();
			}

		} catch (IOException e) {
			Logger.writeError("ERROR: port 3724 is not available!");
		}
		
		try {
			while (true) {
				try {
					LogonConnection connection = new LogonConnection(serverSocket.accept());
					System.out.println("Client connected to logon server.");
					connections.add(connection);
				} catch(NullPointerException e) {
					continue;
				}
			}
		} catch (IOException e) {
			Logger.writeError("Accept failed: 3724");
		}
	}
}
