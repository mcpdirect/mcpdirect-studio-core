package ai.mcpdirect.studio;


import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceEngineFactory;
import appnet.hstp.annotation.ServiceScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceScan("appnet.hstp.labs")
public class MCPDirectStudioApplication {
    private static final Logger LOG = LoggerFactory.getLogger(MCPDirectStudioApplication.class);
    public static void main(String[] args) throws Exception {
//        System.setProperty(HttpServerProviderFactory.PROVIDER_PROPERTY_KEY, JettyServer.class.getName());
        ServiceEngine serviceEngine = ServiceEngineFactory.getServiceEngine();
        // Check if the system supports the system tray
//        if (!SystemTray.isSupported()) {
//            System.err.println("SystemTray is not supported on this OS!");
//            return;
//        }
//        try {
//            UIManager.setLookAndFeel( new FlatLightLaf() );
//        } catch( Exception ex ) {
//            System.err.println( "Failed to initialize LaF" );
//        }
        // Create a popup menu for the tray icon
//        JPopupMenu popup = new JPopupMenu();
//
//        // Add menu items
//        JMenuItem openItem = new JMenuItem("Open");
//        JMenuItem exitItem = new JMenuItem("Exit");
//
//        // Add action listeners
//        openItem.addActionListener(e -> {
//            JOptionPane.showMessageDialog(null, "Application opened!");
//        });
//
//        exitItem.addActionListener(e -> {
//            System.exit(0);
//        });
//
//        popup.add(openItem);
//        popup.addSeparator();
//        popup.add(exitItem);

        // Load an image for the tray icon
//
//        Image trayImage = Toolkit.getDefaultToolkit().getImage(
//                MCPWingsWorkshopApplication.class.getResource("/webapp/mcpwings-icon_32.png")); // Replace with your icon path
//        TrayIcon trayIcon = new TrayIcon(trayImage, "MCPwings Workshop", null);
//
//        // Adjust auto-size if needed
//        trayIcon.setImageAutoSize(true);
//
//        // Add a double-click listener
//        trayIcon.addActionListener(e -> {
//            JOptionPane.showMessageDialog(null, "Tray icon clicked!");
//        });
//        trayIcon.addMouseListener(new MouseAdapter() {
//            public void mouseReleased(MouseEvent e) {
////                if (e.isPopupTrigger()) {
//                    popup.setLocation(e.getX(), e.getY());
//                    popup.setInvoker(popup);
//                    popup.setVisible(true);
////                }
//            }
//        });
//
//        trayIcon.setImageAutoSize(true);
//        // Get the system tray instance and add the icon
//        SystemTray tray = SystemTray.getSystemTray();
//        try {
//            tray.add(trayIcon);
//        } catch (AWTException e) {
//            System.err.println("Failed to add tray icon: " + e.getMessage());
//        }
        LOG.info("ServiceEngine {} started", serviceEngine);
    }
}
