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
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static WildFlyClient.Main.MessageBox;

class UI {

    static ArrayList<String> hostList = new ArrayList<>();
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
            } catch (FileNotFoundException e) {
                MessageBox(new Exception("Не найден файл!"));
                System.exit(0);
            } catch (NullPointerException e) {
                MessageBox(new Exception(e));
                System.exit(0);
            }
        }
    }

    private static class FindText {
        FindText() {
            try {
                String findText = PopupsWindows("FIND", "Поиск текста", "");
                if (findText.equals("")) {
                    PopupsWindows("ERROR", "Пустая строка!", "Ошибка");

                }
                Highlighter highlighter = textArea.getHighlighter();
                Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
                Document doc = textArea.getDocument();
                String text = doc.getText(0, doc.getLength());
                int position = 0;
                while ((position = text.indexOf(findText, position)) >= 0) {
                    highlighter.addHighlight(position, position + findText.length(), painter);
                    position += findText.length();
                }

            } catch (BadLocationException e) {
                MessageBox(e);
            } catch (NullPointerException e) {
                //
            }
        }
    }

    private static String PopupsWindows(String option, String textInWin, String title) {
        String inputText = null;
        if (option.equals("FIND")) {
            UIManager.put("OptionPane.okButtonText", "ok");
            UIManager.put("OptionPane.cancelButtonText", "cancel");
            inputText = JOptionPane.showInputDialog(null, textInWin, title, JOptionPane.PLAIN_MESSAGE);
        }
        if (option.equals("ERROR")) {
            UIManager.put("OptionPane.okButtonText", "Ok");
            JOptionPane.showMessageDialog(new JFrame(), textInWin, title,
                    JOptionPane.ERROR_MESSAGE);
        }
        return inputText;
    }

    static class StatusDeployments {
        StatusDeployments() throws IOException {
            new Connection(selectedHost);
            HashMap<String, String> map = new HashMap<>();
            try {
                JFrame statusFrame = new JFrame(selectedHost);
                JPanel statusPanel = new JPanel();
                statusFrame.setSize(300, 700);
                final ModelNode names = Operations.createOperation("read-children-resources");
                names.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
                final ModelNode nameResult = Connection.client.execute(names);
                final ModelNode deployments = Operations.readResult(nameResult);
                for (String name : deployments.keys()) {
                    final ModelNode namesList = deployments.get(name);
                    map.put(Arrays.toString(namesList.get("runtime-name").toString().split(",")),
                            Arrays.toString(namesList.get("enabled").toString().split(",")) + "\n");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value.contains("true")) {
                        final JButton deploymentsButton = new JButton();
                        deploymentsButton.setText(key);
                        deploymentsButton.setBackground(Color.GREEN);
                        statusPanel.add(deploymentsButton);
                        deploymentsButton.addActionListener(e -> {
                            ModelNode application = Operations.createAddress(key);
                            ModelNode opDisable = Operations.createOperation("add", application);
                            opDisable.get("enabled").set("false");
                            try {
                                ModelNode disable = Connection.client.execute(opDisable);
                                ModelNode result = Operations.readResult(disable);
                                textArea.append(result.toString());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        });
                    } else if (value.contains("false")) {
                        final JButton deploymentsButton = new JButton();
                        deploymentsButton.setText(key);
                        deploymentsButton.setBackground(Color.red);
                        statusPanel.add(deploymentsButton);
                        deploymentsButton.addActionListener(e -> {
                            ModelNode opEnable = Operations.createOperation("add");
                            opEnable.get("enabled").set("true");
                            try {
                                Connection.client.execute(opEnable);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        });
                    }
                }

                statusFrame.setUndecorated(false);
                statusFrame.add(statusPanel);
                statusFrame.setResizable(false);
                statusFrame.setVisible(true);
                statusFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            } catch (IllegalArgumentException e) {
                MessageBox(new Exception(e));
            }

        }
    }

    static class ShutdownServer {
        ShutdownServer() throws IOException {
            new Connection(selectedHost);
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
        } catch (java.lang.NullPointerException e) {
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

    static class WorkPanel {
        WorkPanel(int x, int y, int width, int height, JPanel panel) {
            panel.setBounds(x, y, width, height);
            panel.setBackground(Color.lightGray);
            panel.setBorder(BorderFactory.createLineBorder(Color.black));
        }
    }

    static class Window extends JFrame {
        static JTextField portField = new JTextField(5);

        Window() {
            setTitle("WildFly Client");
            setSize(1045, 575);
            setLocation(500, 300);
            setLayout(null);
            final JPanel serverPanel = new JPanel();
            final JPanel servicePanel = new JPanel();
            final JPanel checkedServerPanel = new JPanel();
            final JPanel progressBarPanel = new JPanel();
            final JPanel portPanel = new JPanel();
            new WorkPanel(5, 5, 145, 490, serverPanel);
            new WorkPanel(860, 5, 175, 430, servicePanel);
            new WorkPanel(860, 440, 175, 55, checkedServerPanel);
            new WorkPanel(5, 500, 845, 38, progressBarPanel);
            new WorkPanel(855, 500, 180, 38, portPanel);
            final JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(700, 490));
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            final JPanel propertiesPanel = new JPanel();
            propertiesPanel.setBounds(155, 0, 700, 495);
            propertiesPanel.add(scrollPane);
            scrollPane.setVisible(true);
            final JLabel checkedServerLabel = new JLabel();
            checkedServerLabel.setFont(new Font("Arial", Font.BOLD, 16));
            checkedServerLabel.setForeground(Color.RED);
            checkedServerPanel.add(checkedServerLabel);
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

            JButton statusDeployments = new JButton();
            statusDeployments.setText("Status");
            statusDeployments.addActionListener(e -> {
                try {
                    new StatusDeployments();
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            JButton findButton = new JButton();
            findButton.setText("Найти текст");
            findButton.setEnabled(false);
            findButton.addActionListener(e -> new FindText());

            JButton clearButton = new JButton();
            clearButton.setText("Очистить окно");
            clearButton.addActionListener(e -> ClearingWorkPlace());

            JButton shutdownButton = new JButton();
            shutdownButton.setText("Выключить!");
            shutdownButton.addActionListener(e -> {
                try {
                    new ShutdownServer();
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
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
                } catch (IOException | InterruptedException | InvocationTargetException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            textArea.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Highlighter highlighter = textArea.getHighlighter();
                    highlighter.removeAllHighlights();
                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });

            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(new TitledBorder(new EtchedBorder(), "Текущиий СТАТУС: "));
            textArea.setForeground(Color.BLACK);

            servicePanel.add(statusDeployments);
            servicePanel.add(findButton);
            servicePanel.add(params);
            servicePanel.add(clearButton);
            servicePanel.add(logButton);
            servicePanel.add(stopButton);
            servicePanel.add(shutdownButton);
            servicePanel.add(statusDeployments);

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
}