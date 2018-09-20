package WildFlyClient;

/**
 * The project of remote configuration and control of servers on the WildFly platform (JBoss Application Server),
 * so far in development. For authorization in the application registration data of WildFly are used,
 * the server can be chosen from the list.
 */

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class Main {

    static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception);
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            MessageBox(e);
        }
        new Autorization();
    }
}