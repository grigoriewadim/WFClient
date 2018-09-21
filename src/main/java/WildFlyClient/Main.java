package WildFlyClient;

/*
  The project of remote configuration and control of servers on the WildFly platform (JBoss Application Server),
  so far in development. For authorization in the application registration data of WildFly are used,
  the server can be chosen from the list.
 */

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static WildFlyClient.UI.GetDate;

public class Main {

    static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception, "Ошибка", JOptionPane.ERROR_MESSAGE);
        File filename = new File("exception_" + GetDate() + "_.log");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(GetDate() + "_" + exception.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("nimbusBlueGrey", new Color(220, 220, 220));
            UIManager.put("nimbusBase", new Color(135, 206, 235));
        } catch (UnsupportedLookAndFeelException e) {
            MessageBox(e);
        }
        new Autorization();
    }
}