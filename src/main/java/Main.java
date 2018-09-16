/**
 * The project of remote configuration and control of servers on the WildFly platform (JBoss Application Server),
 * so far in development. For authorization in the application registration data of WildFly are used,
 * the server can be chosen from the list.
 * In a consequence I plan to add a feature for the indication of individual registration data for each of servers.
 *
 */

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static ArrayList<String> hostList = new ArrayList<>();
    static ArrayList<String> exportList = new ArrayList<>();
    static JTextArea textArea = new JTextArea();
    static String currentHost;
    static ModelControllerClient client;

    private static JTextField loginTxt = new JTextField(10);
    private static JPasswordField passwordTxt = new JPasswordField(10);

    static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception);
    }

    static class ReadConfig {
        ReadConfig() throws Exception {
            try {
                String filename = "hostlist.cfg";
                BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
                String line;
                while ((line = reader.readLine()) != null) {
                    hostList.add(line);
                }
                reader.close();
            } catch (FileNotFoundException exf) {
                MessageBox(new Exception("Не найден файл!"));
                System.exit(0);
            } catch (NullPointerException exn) {
                MessageBox(new Exception(exn));
                System.exit(0);
            }
        }
    }

    static class ConnectToHost {
        ConnectToHost(String host) throws IOException {
            int port = 9990;
            try {
                client = ModelControllerClient.Factory.create(
                        InetAddress.getByName(host), port,
                        callbacks -> {
                            for (Callback current : callbacks) {
                                if (current instanceof NameCallback) {
                                    NameCallback ncb = (NameCallback) current;
                                    ncb.setName(loginTxt.getText());
                                } else if (current instanceof PasswordCallback) {
                                    PasswordCallback pcb = (PasswordCallback) current;
                                    //pcb.setPassword(String.valueOf(passwordTxt.getPassword().toCharArray()));
                                    pcb.setPassword(passwordTxt.getText().toCharArray());
                                } else if (current instanceof RealmCallback) {
                                    RealmCallback rcb = (RealmCallback) current;
                                    rcb.setText(rcb.getDefaultText());
                                } else {
                                    throw new UnsupportedCallbackException(current);
                                }
                            }
                        });


            } catch (java.net.UnknownHostException e) {
                MessageBox(new Exception("Ошибка установки соединения с сервером " + currentHost + " " + e));
            } catch (java.lang.NullPointerException nullEx) {
                MessageBox(new Exception("Не выбран сервер для загрузки конфигурации! "));
            }
        }
    }

    static class Deployments {
        Deployments(final String currentHost) throws IOException {
            exportList.clear();
            new ConnectToHost(currentHost);
            try {
                final ModelNode op = Operations.createOperation("read-children-resources");
                op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
                final ModelNode result = client.execute(op);
                if (Operations.isSuccessfulOutcome(result)) {
                    final ModelNode deployments = Operations.readResult(result);
                    exportList.add("Установлено подключение к серверу: " + currentHost + "\n");
                    exportList.add("Список задеплоеных приложений:  " + "\n");
                    for (String deploymentName : deployments.keys()) {
                        final ModelNode deploymentDetails = deployments.get(deploymentName);
                        exportList.add("NAME DEPLOYMENT: " + deploymentDetails.get("runtime-name") + "\n");
                        exportList.add("ENABLED: " + deploymentDetails.get("enabled") + "\n");

                    }
                } else {
                    MessageBox(new Exception("Failed to list deployments: " + Operations.getFailureDescription(result).asString()));
                }
            } catch (IOException ex) {
                MessageBox(new Exception(ex));
            }
        }
    }

    static class ShutdownServer {
        ShutdownServer(final String currentHost) throws IOException {
            new ConnectToHost(currentHost);
            final ModelNode op = Operations.createOperation("shutdown");
            op.get("restart").set(true);
            client.execute(op);
        }
    }


    private static String ServerParameters(String currentHost) throws IOException {
        ArrayList<ModelNode> attributesList = new ArrayList<>();
        ArrayList<ModelNode> result = new ArrayList<>();
        try {
            new ConnectToHost(currentHost);
            final ModelNode address = new ModelNode().setEmptyList();
            final ModelNode serverState = Operations.createReadAttributeOperation(address, "server-state");
            final ModelNode attr = Operations.createOperation("read-config-as-xml");
            attributesList.add(serverState);
            attributesList.add(attr);
            for (ModelNode anAttributesList : attributesList) {
                result.add(client.execute(anAttributesList));
            }
            return result.toString().replace("[", "").replace("]", "").replace(",", "") + "\n";

        } catch (IOException serverParametersException) {
            MessageBox(new Exception("Невозможно загрузить параметры сервера ", serverParametersException));
        } catch (java.lang.NullPointerException nullEx) {
            MessageBox(new Exception("Выбери для начала сервер, а потом уже тыкай!"));
        } finally {
            client.close();
        }
        return null;
    }

    private static class ServerLogs {
        ServerLogs(String currentHost) throws IOException {
            new ConnectToHost(currentHost);
            final ModelNode logging = Operations.createReadResourceOperation(Operations.createAddress( "subsystem", "logging", "log-file", "server.log"));
            logging.get("standalone-runtime").set(true);
            final ModelNode opLogging = Operations.createReadResourceOperation(logging);


            System.out.println("Logging-->  " + opLogging);
        }
    }

    private void ServerEnvironment() {
    }

    private static void TriggerEnabled(JButton button) {
        button.setEnabled(true);
    }

    private static void ClearingWorkPlace() {
        textArea.selectAll();
        textArea.replaceSelection(" ");
    }

    static class UI extends JFrame {
        UI() {
            setTitle("WildFly Client");
            setSize(1045, 550);
            setLocation(500, 300);
            setLayout(null);
            final JPanel serverPanel = new JPanel();
            serverPanel.setBackground(Color.LIGHT_GRAY);
            serverPanel.setBounds(0, 5, 150, 490);
            serverPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            final JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(700, 490));
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            final JPanel propertiesPanel = new JPanel();
            propertiesPanel.setBounds(155, 0, 700, 495);
            propertiesPanel.add(scrollPane);
            scrollPane.setVisible(true);
            final JPanel servicePanel = new JPanel();
            servicePanel.setBackground(Color.lightGray);
            servicePanel.setBounds(860, 5, 175, 430);
            servicePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            final JPanel checkedServerPanel = new JPanel();
            checkedServerPanel.setBounds(860, 440, 175, 55);
            checkedServerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            final JLabel checkedServerLabel = new JLabel();
            checkedServerLabel.setFont(new Font("Arial", Font.BOLD, 16));
            checkedServerLabel.setForeground(Color.RED);
            checkedServerPanel.add(checkedServerLabel);
            final JPanel progressBarPanel = new JPanel();
            progressBarPanel.setBounds(0, 500, 1035, 20);
            progressBarPanel.setBackground(Color.GRAY);
            progressBarPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            final JProgressBar progressBar = new JProgressBar();
            progressBar.setBorderPainted(true);

            final String[] localHost = {null};

            for (final String aHostList : hostList) {
                final JButton serverButton = new JButton();
                serverButton.setSize(90, 30);
                serverButton.setText(aHostList);
                serverPanel.add(serverButton);
                localHost[0] = serverButton.getText();

                serverButton.addActionListener(e -> {
                    localHost[0] = e.getActionCommand();
                    checkedServerLabel.setText(localHost[0]);
                });
            }

            JButton findButton = new JButton();
            findButton.setText("Найти текст");
            findButton.setEnabled(false);
            findButton.addActionListener(e -> {
                new FindText();
            });

            JButton listDeployments = new JButton();
            listDeployments.setText("Список приложений");
            listDeployments.addActionListener(e -> {
                TriggerEnabled(findButton);
                ClearingWorkPlace();
                try {
                    new Deployments(localHost[0]);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                textArea.append(exportList.toString().replace("[", "").replace("]", "").replace(",", ""));

            });

            JButton clearButton = new JButton();
            clearButton.setText("Очистить окно");
            clearButton.addActionListener(e -> {
                ClearingWorkPlace();
            });

            JButton shutdownButton = new JButton();
            shutdownButton.setText("Выключить!");
            shutdownButton.addActionListener(e -> {
                try {
                    new ShutdownServer(localHost[0]);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });


            JButton params = new JButton();
            params.setText("Параметры сервера");
            params.addActionListener(e -> {
                ClearingWorkPlace();
                TriggerEnabled(findButton);
                try {
                    textArea.append(ServerParameters(localHost[0]));
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            JButton logButton = new JButton();
            logButton.setText("Логи WildFly");
            logButton.addActionListener(e -> {
                ClearingWorkPlace();
                TriggerEnabled(findButton);
                try {
                    ServerLogs serverLogs = new ServerLogs(localHost[0]);
                    textArea.append(serverLogs.toString());
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }

            });

            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(new TitledBorder(new EtchedBorder(), "Текущиий СТАТУС: "));
            textArea.setForeground(Color.BLACK);

            servicePanel.add(findButton);
            servicePanel.add(listDeployments);
            servicePanel.add(params);
            servicePanel.add(clearButton);
            servicePanel.add(logButton);
            servicePanel.add(shutdownButton);

            add(checkedServerPanel);
            add(serverPanel);
            add(propertiesPanel);
            add(servicePanel);
            add(progressBarPanel);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setVisible(true);
        }

    }

    private static class FindText {
        FindText() {
            try {
                Highlighter hightLight = textArea.getHighlighter();
                Object findText = JOptionPane.showInputDialog("Поиск текста", "");
                Pattern pattern = Pattern.compile("\b" + findText + "\b");
                Matcher matcher = pattern.matcher(String.valueOf(findText));
                boolean matchFound = matcher.matches();
                System.out.println(matchFound);
                System.out.println(pattern.toString());


            } catch (NullPointerException nEx) {
                MessageBox(new Exception(nEx));
            }
        }
    }


    private static class Autorization extends JFrame {
        Autorization() throws Exception {
            new ReadConfig();
            final boolean[] success = {false};
            setSize(450, 210);
            setResizable(false);
            setLocation(300, 100);
            setTitle("Авторизация!");
            setLayout(null);
            JPanel formPanel = new JPanel(new BorderLayout());
            formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
            formPanel.setBounds(1, 1, getWidth() - 10, getHeight() - 10);
            formPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JPanel selector = new JPanel();
            JLabel selectorLabel = new JLabel("IP адрес сервера для авторизации: ");

            JComboBox<String> comboBox = new JComboBox<>();
            for (String aHostList : hostList) {
                comboBox.addItem(aHostList);
            }

            selector.add(selectorLabel);
            selector.add(comboBox);
            JPanel loginPanel = new JPanel();
            JLabel loginLabel = new JLabel("Логин: ");
            loginTxt.setForeground(Color.blue);
            loginPanel.add(loginLabel);
            loginPanel.add(loginTxt);
            JPanel passPanel = new JPanel();
            JLabel passwordLabel = new JLabel("Пароль: ");
            passwordTxt.setForeground(Color.blue);
            passPanel.add(passwordLabel);
            passPanel.add(passwordTxt);
            JPanel buttonPanel = new JPanel();
            JButton submitButton = new JButton("Подтвердить");
            buttonPanel.add(submitButton);
            formPanel.add(selector);
            formPanel.add(loginPanel);
            formPanel.add(passPanel);
            formPanel.add(buttonPanel);
            add(formPanel);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            String serverSelector = comboBox.getSelectedItem().toString();
            submitButton.addActionListener(e -> {
                try {
                    final ModelNode check = Operations.createOperation("status");
                    ModelControllerClient checkHost = ModelControllerClient.Factory.create(
                            InetAddress.getByName(serverSelector), 9990,
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
                    new UI();
                    setVisible(false);
                }
            });
        }
    }


    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            MessageBox(e);
        }
        new Autorization();
        //new UI();
    }
}