package WildFlyClient;

/*
    Класс для всех функциональных классов и методов задействованных в UI
* */

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static WildFlyClient.Main.MessageBox;
import static WildFlyClient.UI.*;

class Functions {

    static ArrayList<String> hostList = new ArrayList<>(); // Коллекция серверов (ip адресов) добавленная из конфига
    static HashMap<String, String> applicationMap = new HashMap<>(); //Коллекция deployment'ов с выбранного сервера
    static String startDate = GetDate();
    static String endDate = GetDate();
    static String selectedHost = null; //Выбранный сервер (для операций)
    static int serverPanelSize = 0;

    static String GetDate() { //Метод для получения текущей даты и времени
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyy_HH-mm-ss");
        Date data = new Date();
        return dateFormat.format(data);
    }

    static class ReadConfig { //Читаем конфиг
        ReadConfig() throws Exception {
            try {
                String filename = "hostlist.cfg";
                BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
                String line;
                while ((line = reader.readLine()) != null) {
                    hostList.add(line); //Добавляем в коллекцию
                    serverPanelSize++; //Для прорисовки длинны JScroll панели с серверами JButton
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

    static class FindText {  //Класс для поиска запрашиваемого текста в JTextArea
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

    static class ShutdownServer {  // Класс для вызова команды перезагрузки сервера
        ShutdownServer(String option) throws IOException {
            String request = "";
            if (option.equals("SHUTDOWN")) {
                request = "/:shutdown(timeout=0)";
            }
            if (option.equals("RESET")) {
                request = "/:shutdown(restart=true,timeout=0)";
            }
            if (option.equals("RELOAD")) {
                request = "/:reload";
            }
            try {
                Connection.client.close();
                Connection.ctx.connectController();
                Connection.ctx.handle(request);
            } catch (CommandFormatException e1) {
                MessageBox(new Exception("Не корректно введено название!\n" + e1));
            } catch (CommandLineException e1) {
                MessageBox(new Exception(e1));
            }
        }
    }


    static String ServerParameters(String currentHost) throws IOException { // Метод выгрузки конфига в JTextArea, выбранного сервера
        ArrayList<ModelNode> attributesList = new ArrayList<>();
        ArrayList<ModelNode> result = new ArrayList<>();
        try {
            new Connection(currentHost);
            final ModelNode address = new ModelNode().setEmptyList();
            final ModelNode serverState = org.jboss.as.controller.client.helpers.Operations.createReadAttributeOperation(address, "server-state");
            final ModelNode attr = org.jboss.as.controller.client.helpers.Operations.createOperation("read-config-as-xml");
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

    static class ServerLogs { // Класс для получения логов с выбранного сервера, запись производится в файл в /homedir/ с екущей датой
        ServerLogs(String currentHost) throws IOException, InvocationTargetException, InterruptedException {
            File filename = new File("wildfly_server_" + GetDate() + "_" + currentHost + "_.log");
            FileOutputStream outputStream = new FileOutputStream(filename);
            new Connection(currentHost);
            final ModelNode address = org.jboss.as.controller.client.helpers.Operations.createAddress("subsystem", "logging", "log-file", "server.log");
            final ModelNode operation = org.jboss.as.controller.client.helpers.Operations.createReadResourceOperation(address);
            operation.get("include-runtime").set(true);
            final OperationResponse response = Connection.client.executeOperation(OperationBuilder.create(operation).build(), OperationMessageHandler.logging);
            final ModelNode result = response.getResponseNode();
            final String line = org.jboss.as.controller.client.helpers.Operations.readResult(result).get("stream").asString();
            final InputStream stream = response.getInputStream(line).getStream();
            final byte[] buffer = new byte[64];
            while (((stream.read(buffer)) != -1) && (!stopTrigger)) {
                outputStream.write(buffer);
            }
            outputStream.close();
        }
    }

    static class GetLogs {
        GetLogs() throws IOException {
            Object length = Integer.parseInt(PopupsWindows("LOG", "Введите количество строк: ", "Запрос"));
            String request = "/subsystem=logging/log-file=server.log/:read-log-file(lines=" + length + ",skip=0)";
            try {
                Connection.client.close();
                Connection.ctx.connectController();
                Connection.ctx.handle(request);

            } catch (CommandLineException e1) {
                MessageBox(new Exception(e1));
            }
        }
    }

    static class DeploymentsButtons { //Класс для прорисовки JButton's из HashMap c именем модуля и статусом вкл/выкл.
        // Функция disable/enable в процессе реализации
        static JPanel appContainer = new JPanel();

        DeploymentsButtons(String key, String value) throws IOException, CliInitializationException {
            String appName = key.replace("[", "").replace("]", "").replace("\"", "");
            if (value.contains("true")) {
                final JButton deploymentsButton = new JButton();
                deploymentsButton.setText(appName);
                deploymentsButton.setBackground(Color.GREEN);
                appContainer.add(deploymentsButton);
                String request = "/subsystem=/deployment= " + appName + ":read-resource";
                deploymentsButton.addActionListener(e -> {
                    try {
                        Connection.client.close();
                        Connection.ctx.connectController();
                        Connection.ctx.handle(request);
                    } catch (CommandLineException | IOException e1) {
                        MessageBox(new Exception(e1));
                    }

                });
            } else if (value.contains("false")) {
                final JButton deploymentsButton = new JButton();
                deploymentsButton.setText(appName);
                deploymentsButton.setBackground(Color.red);
                appContainer.add(deploymentsButton);
                String request = "/deployment=" + appName + ":read-resource";
                deploymentsButton.addActionListener(e -> {
                    try {
                        Connection.client.close();
                        Connection.ctx.connectController();
                        Connection.ctx.handle(request);
                    } catch (CommandLineException | IOException e1) {
                        MessageBox(new Exception(e1));
                    }
                });
            }

        }
    }

    static class StatusDeployments { // Класс для проверки доступности deployment'a, добавление его в HashMap со значением enabled или disabled
        StatusDeployments() throws IOException {
            new Connection(selectedHost);
            try {
                final ModelNode names = org.jboss.as.controller.client.helpers.Operations.createOperation("read-children-resources");
                names.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
                final ModelNode nameResult = Connection.client.execute(names);
                final ModelNode deployments = org.jboss.as.controller.client.helpers.Operations.readResult(nameResult);
                for (String name : deployments.keys()) {
                    final ModelNode namesList = deployments.get(name);
                    applicationMap.put(Arrays.toString(namesList.get("runtime-name").toString().split(",")),
                            Arrays.toString(namesList.get("enabled").toString().split(",")) + "\n");
                }
                for (Map.Entry<String, String> entry : applicationMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    new DeploymentsButtons(key, value);
                    //System.out.println(key +"  --  "+ value);
                }
            } catch (IllegalArgumentException | CliInitializationException e) {
                MessageBox(new Exception(e));
            }

        }
    }

    static class DeployApplication {
        DeployApplication() throws IOException {
            DeployApplicationUI deployUI = new DeployApplicationUI();
            String deployment_name = deployUI.nameField.getText();
            String deployment_runtime_name = deployUI.runtimenameFild.getText();
            String status;
            if (deployUI.status) {
                status = "enabled";
            } else {
                status = "disabled";
            }
            File warFile = UI.warFile;

            String request = "deploy " + warFile.getAbsolutePath() +
                    " --name=" + deployment_name + " --runtime-name=" + deployment_runtime_name + " --" + status;
            try {
                Connection.client.close();
                Connection.ctx.connectController();
                Connection.ctx.handle(request);
            } catch (CommandFormatException e1) {
                MessageBox(new Exception("Не корректно введено название!\n" + e1));
            } catch (CommandLineException e1) {
                MessageBox(new Exception(e1));
            }
        }
    }

    static class UndeployApplication {
        UndeployApplication() throws IOException {
            String nameApplication = PopupsWindows("UNDEPLOY", "Runtime-Name удаляемого приложения: ", "" + selectedHost);
            String request = "undeploy --name " + nameApplication;
            try {
                Connection.client.close();
                Connection.ctx.connectController();
                Connection.ctx.handle(request);
            } catch (CommandFormatException e1) {
                MessageBox(new Exception("Не корректно введено название!\n" + e1));
            } catch (CommandLineException e1) {
                MessageBox(new Exception(e1));
            }
        }
    }
}
