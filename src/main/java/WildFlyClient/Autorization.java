package WildFlyClient;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import static WildFlyClient.Main.MessageBox;

class Autorization {
    static JTextField loginTxt = new JTextField(10);
    static JPasswordField passwordTxt = new JPasswordField(10);

    Autorization() throws Exception {
        ArrayList hostList = UI.hostList;
        JTextField portField = new JTextField(5);
        JFrame autorizationFrame = new JFrame();
        new UI.ReadConfig();
        final boolean[] success = new boolean[1];
        autorizationFrame.setSize(450, 210);
        autorizationFrame.setResizable(false);
        autorizationFrame.setLocation(300, 100);
        autorizationFrame.setTitle("Авторизация!");
        autorizationFrame.setLayout(null);
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBounds(1, 1, autorizationFrame.getWidth() - 10, autorizationFrame.getHeight() - 10);
        formPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JPanel selector = new JPanel();
        JLabel selectorLabel = new JLabel("IP адрес сервера для авторизации: ");
        JComboBox<String> comboBox = new JComboBox<>();
        for (Object aHostList : hostList) {
            comboBox.addItem(String.valueOf(aHostList));
        }
        selector.add(selectorLabel);
        selector.add(comboBox);
        portField.setText("9990");
        portField.setForeground(Color.blue);
        selector.add(portField);
        JPanel loginPanel = new JPanel();
        JLabel loginLabel = new JLabel("Логин: ");
        loginTxt.setForeground(Color.blue);
        loginPanel.add(loginLabel);
        loginTxt.setText("admin");
        loginPanel.add(loginTxt);
        JPanel passPanel = new JPanel();
        JLabel passwordLabel = new JLabel("Пароль: ");
        passwordTxt.setForeground(Color.blue);
        passPanel.add(passwordLabel);
        passwordTxt.setText("admin");
        passPanel.add(passwordTxt);
        JPanel buttonPanel = new JPanel();
        JButton submitButton = new JButton("Подтвердить");
        buttonPanel.add(submitButton);
        JPanel fill = new JPanel();
        formPanel.add(selector);
        formPanel.add(loginPanel);
        formPanel.add(passPanel);
        formPanel.add(buttonPanel);
        formPanel.add(fill);
        formPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        autorizationFrame.add(formPanel);
        autorizationFrame.setVisible(true);
        autorizationFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        submitButton.addActionListener(e -> {
            String serverSelector = comboBox.getSelectedItem().toString();
            try {
                final ModelNode check = Operations.createOperation("status");
                final ModelControllerClient checkHost = ModelControllerClient.Factory.create(
                        InetAddress.getByName(serverSelector), Integer.parseInt(portField.getText()),
                        callbacks -> {
                            for (Callback current : callbacks) {
                                if (current instanceof NameCallback) {
                                    NameCallback ncb = (NameCallback) current;
                                    ncb.setName(loginTxt.getText());
                                } else if (current instanceof PasswordCallback) {
                                    PasswordCallback pcb = (PasswordCallback) current;
                                    pcb.setPassword(passwordTxt.getText().toCharArray());
                                } else if (current instanceof RealmCallback) {
                                    RealmCallback rcb = (RealmCallback) current;
                                    rcb.setText(rcb.getDefaultText());
                                } else {
                                    throw new UnsupportedCallbackException(current);
                                }
                            }
                        });
                checkHost.execute(check);
                success[0] = true;

            } catch (IOException e1) {
                MessageBox(new Exception("Ошибка установки соединения с сервером " + serverSelector + " или неверный пароль! "));
                success[0] = false;
            }

            if (success[0]) {
                new UI.Window();
                autorizationFrame.setVisible(false);
            }
        });
    }
}

