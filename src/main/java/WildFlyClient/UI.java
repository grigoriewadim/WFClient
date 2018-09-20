package WildFlyClient;

import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static WildFlyClient.Main.MessageBox;

class UI {

    static ArrayList<String> hostList = new ArrayList<>();
    static ArrayList<String> exportList = new ArrayList<>();
    static JTextArea textArea = new JTextArea();
    static boolean stopTrigger = false;
    private static String startDate = GetDate();
    private static String endDate = GetDate();
    static String selectedHost = null;

    static String GetDate() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyy_HH-mm-ss");
        Date data = new Date();
        return dateFormat.format(data);
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

    static class Deployments {
        Deployments(final String currentHost) throws IOException {
            exportList.clear();
            new Connection(selectedHost);
            try {
                final ModelNode op = Operations.createOperation("read-children-resources");
                op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
                final ModelNode result = Connection.client.execute(op);
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
            new Connection(currentHost);
            final ModelNode op = Operations.createOperation("shutdown");
            op.get("restart").set(true);
            Connection.client.execute(op);
        }
    }


    private static String ServerParameters(String currentHost) throws IOException {
        ArrayList<ModelNode> attributesList = new ArrayList<>();
        ArrayList<ModelNode> result = new ArrayList<>();
        try {
            new Connection(currentHost);
            final ModelNode address = new ModelNode().setEmptyList();
            final ModelNode serverState = Operations.createReadAttributeOperation(address, "server-state");
            final ModelNode attr = Operations.createOperation("read-config-as-xml");
            attributesList.add(serverState);
            attributesList.add(attr);
            for (ModelNode anAttributesList : attributesList) {
                result.add(Connection.client.execute(anAttributesList));
            }
            return result.toString().replace("[", "").replace("]", "").replace(",", "") + "\n";

        } catch (IOException serverParametersException) {
            MessageBox(new Exception("Невозможно загрузить параметры сервера ", serverParametersException));
        } catch (java.lang.NullPointerException nullEx) {
            MessageBox(new Exception("Выбери для начала сервер, а потом уже тыкай!"));
        } finally {
            Connection.client.close();
        }
        return null;
    }

    private static class ServerLogs {
        ServerLogs(String currentHost) throws IOException, InvocationTargetException, InterruptedException {
            File filename = new File("wildfly_server_" + GetDate() + "_" + currentHost + "_.log");
            FileOutputStream outputStream = new FileOutputStream(filename);
            new Connection(currentHost);
            final ModelNode address = Operations.createAddress("subsystem", "logging", "log-file", "server.log");
            final ModelNode operation = Operations.createReadResourceOperation(address);
            operation.get("include-runtime").set(true);
            final OperationResponse response = Connection.client.executeOperation(OperationBuilder.create(operation).build(), OperationMessageHandler.logging);
            final ModelNode result = response.getResponseNode();
            final String line = Operations.readResult(result).get("stream").asString();
            final InputStream stream = response.getInputStream(line).getStream();
            final byte[] buffer = new byte[64];
            while (((stream.read(buffer)) != -1) && (!stopTrigger)) {
                outputStream.write(buffer);
            }
            outputStream.close();
        }
    }

    static class Window extends JFrame {
        static JTextField portField = new JTextField(5);

        Window() {
            setTitle("WildFly Client");
            setSize(1045, 570);
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
            progressBarPanel.setBounds(0, 500, 850, 40);
            progressBarPanel.setBackground(Color.GRAY);
            progressBarPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            final JPanel portPanel = new JPanel();
            portPanel.setBounds(855, 500, 180, 40);
            portPanel.setBackground(Color.GRAY);
            portPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JLabel portLabel = new JLabel("Порт подключения:");
            portLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            portPanel.add(portLabel);
            portField.getText();
            portField.setText("9990");
            portField.setBounds(855, 500, 180, 20);
            portPanel.add(portField);

            for (final String aHostList : hostList) {
                final JButton serverButton = new JButton();
                serverButton.setSize(90, 30);
                serverButton.setText(aHostList);
                serverPanel.add(serverButton);
                selectedHost = serverButton.getText();
                serverButton.addActionListener(e -> {
                    selectedHost = e.getActionCommand();
                    checkedServerLabel.setText(selectedHost);

                });
            }

            JButton findButton = new JButton();
            findButton.setText("Найти текст");
            findButton.setEnabled(false);
            findButton.addActionListener(e -> new FindText());

            JButton listDeployments = new JButton();
            listDeployments.setText("Список приложений");
            listDeployments.addActionListener(e -> {
                TriggerEnabled(findButton);
                ClearingWorkPlace();
                try {
                    new Deployments(selectedHost);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                textArea.append(exportList.toString().replace("[", "").replace("]", "").replace(",", ""));

            });

            JButton clearButton = new JButton();
            clearButton.setText("Очистить окно");
            clearButton.addActionListener(e -> ClearingWorkPlace());

            JButton shutdownButton = new JButton();
            shutdownButton.setText("Выключить!");
            shutdownButton.addActionListener(e -> {
                try {
                    new ShutdownServer(selectedHost);
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
                    textArea.append(ServerParameters(selectedHost));
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            JButton logButton = new JButton();
            JButton stopButton = new JButton();
            stopButton.setEnabled(false);
            stopButton.setText("Остановить загрузку");
            stopButton.addActionListener((ActionEvent e) -> {
                stopTrigger = true;
                textArea.append("[" + endDate + "]: Загрузка логфайла закончена. \n");
                logButton.setEnabled(true);
            });
            logButton.setText("Логи WildFly");
            logButton.addActionListener(e -> {
                ClearingWorkPlace();
                textArea.append("[" + startDate + "]: Загрузка логфайла начата. \n");
                try {
                    stopButton.setEnabled(true);
                    logButton.setEnabled(false);
                    new ServerLogs(selectedHost);
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                } catch (InterruptedException | InvocationTargetException e1) {
                    e1.printStackTrace();
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
            servicePanel.add(stopButton);
            servicePanel.add(shutdownButton);

            add(checkedServerPanel);
            add(serverPanel);
            add(propertiesPanel);
            add(servicePanel);
            add(progressBarPanel);
            add(portPanel);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setVisible(true);
        }

    }

    private static void TriggerEnabled(JButton button) {
        button.setEnabled(true);
    }

    private static void ClearingWorkPlace() {
        textArea.selectAll();
        textArea.replaceSelection(" ");
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
}