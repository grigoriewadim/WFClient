package WildFlyClient;
/*
    Класс отвечающий за прорисовывание формы и всех ее атрибутов
* */

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static WildFlyClient.Main.MessageBox;
import static WildFlyClient.Functions.*;
import static WildFlyClient.Functions.DeploymentsButtons.appContainer;

class UI {

    static JTextArea textArea = new JTextArea();
    static boolean stopTrigger = false;

    private static void TriggerEnabled(JMenuItem item) {
        item.setEnabled(true);
    }

    private static void ClearingWorkPlace() {
        textArea.selectAll();
        textArea.replaceSelection(" ");
    }

    static class WorkPanel { // Внешний вид панелей
        WorkPanel(int x, int y, int width, int height, JPanel panel, boolean border) {
            panel.setBounds(x, y, width, height);
            panel.setBackground(Color.lightGray);
            if (border) {
                panel.setBorder(BorderFactory.createLineBorder(Color.black));
            }
        }
    }

    static class Window extends JFrame { // Главный класс основного окна
        static JTextField portField = new JTextField(5);

        Window() {
            setTitle("WildFly Client");
            setSize(1075, 600);
            setLocation(500, 300);
            setLayout(null);
            final JPanel serverPanel = new JPanel();
            final JPanel serversContainer = new JPanel();
            final JPanel checkedServerPanel = new JPanel();
            final JPanel progressBarPanel = new JPanel();
            final JPanel portPanel = new JPanel();
            final JPanel deploymentsPanel = new JPanel();

            new WorkPanel(755, 5, 300, 495, appContainer, false);
            new WorkPanel(755, 5, 310, 490, deploymentsPanel, false);
            new WorkPanel(5, 5, 180, 490, serverPanel, true);
            new WorkPanel(933, 500, 135, 38, checkedServerPanel, true);
            new WorkPanel(5, 500, 748, 38, progressBarPanel, true);
            new WorkPanel(755, 500, 175, 38, portPanel, true);

            JScrollPane appScrollPane = new JScrollPane(appContainer,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            appScrollPane.setPreferredSize(new Dimension(300, 480));
            serversContainer.setBackground(Color.lightGray);
            appContainer.setPreferredSize(new Dimension(290, serverPanelSize * 55)); //470 - appPanelSize
            deploymentsPanel.add(appScrollPane);

            JScrollPane serverScrollPane = new JScrollPane(serversContainer,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            serverScrollPane.setPreferredSize(new Dimension(180, 480));
            serversContainer.setBackground(Color.lightGray);
            serversContainer.setPreferredSize(new Dimension(170, serverPanelSize * 35)); //
            serverPanel.add(serverScrollPane);

            final JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(570, 495));
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            final JPanel propertiesPanel = new JPanel();
            propertiesPanel.setBounds(185, 0, 570, 495);
            propertiesPanel.add(scrollPane);

            final JLabel checkedServerLabel = new JLabel();
            checkedServerLabel.setFont(new Font("Arial", Font.BOLD, 18));
            checkedServerLabel.setForeground(Color.RED);
            checkedServerPanel.add(checkedServerLabel);
            JLabel portLabel = new JLabel("Порт подключения:");
            portLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            portPanel.add(portLabel);
            portField.getText();
            portField.setText("9990");
            portField.setBounds(855, 500, 180, 20);
            portPanel.add(portField);

            JMenuBar windowMenuBar = new JMenuBar();
            JMenu commandMenu = new JMenu("Операции");
            JMenu logsMenu = new JMenu("Логирование");
            JMenu findMenu = new JMenu("Поиск");
            JMenuItem clearPlaceItem = new JMenuItem("Очистить");
            clearPlaceItem.addActionListener(e -> ClearingWorkPlace());
            JMenuItem findTextItem = new JMenuItem("Натйи текст");
            findTextItem.setEnabled(false);
            findTextItem.addActionListener(e -> new Functions.FindText());
            JMenuItem propertiesItem = new JMenuItem("Параметры сервера");
            propertiesItem.addActionListener(e -> {
                ClearingWorkPlace();
                TriggerEnabled(findTextItem);
                try {
                    textArea.append(ServerParameters(selectedHost));
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }
            });
            JMenuItem resetServer = new JMenuItem("Перезагрузить!");
            resetServer.addActionListener(e -> {
                try {
                    new Functions.ShutdownServer();
                } catch (IOException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            JMenuItem openFileItem = new JMenuItem("Открыть файл");
            openFileItem.addActionListener(e -> new Functions.OpenFileDialog());

            JMenuItem stopLogItem = new JMenuItem("Остановить загрузку");
            JMenuItem startLogItem = new JMenuItem("Получить лог");
            startLogItem.addActionListener(e -> {
                ClearingWorkPlace();
                textArea.append("[" + startDate + "]: Загрузка логфайла началась. \n");
                try {
                    stopLogItem.setEnabled(true);
                    startLogItem.setEnabled(false);
                    new Functions.ServerLogs(selectedHost);
                } catch (IOException | InterruptedException | InvocationTargetException e1) {
                    MessageBox(new Exception(e1));
                }
            });
            stopLogItem.addActionListener(e -> {
                stopTrigger = true;
                textArea.append("[" + endDate + "]: Загрузка логфайла закончена. \n");
                startLogItem.setEnabled(true);
                stopLogItem.setEnabled(false);
            });
            commandMenu.add(clearPlaceItem);
            commandMenu.add(propertiesItem);
            commandMenu.add(resetServer);
            commandMenu.add(openFileItem);
            logsMenu.add(startLogItem);
            logsMenu.add(stopLogItem);
            findMenu.add(findTextItem);
            windowMenuBar.add(commandMenu);
            windowMenuBar.add(logsMenu);
            windowMenuBar.add(findMenu);
            setJMenuBar(windowMenuBar);

            findTextItem.setEnabled(false);
            openFileItem.setEnabled(false);

            for (final String aHostList : hostList) { // Добавляем ActionListener к каждой JButton с сервером
                final JButton serverButton = new JButton();
                serverButton.setText(aHostList);
                serversContainer.add(serverButton);
                selectedHost = serverButton.getText();
                serverButton.addActionListener(e -> {
                    selectedHost = e.getActionCommand(); // Получаем ip
                    checkedServerLabel.setText(selectedHost); // в панель с выбранным сервером добавляем его ip
                    try {
                        findTextItem.setEnabled(true);
                        openFileItem.setEnabled(true);
                        appContainer.removeAll(); // Очищаем панель deployment'ов с выбранного сервера
                        new Functions.StatusDeployments(); // Вызываем класс с параметрами приложения
                        //Connection.client.close();
                    } catch (IOException e1) {
                        MessageBox(new Exception(e1));
                    }
                });
            }

            textArea.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Highlighter highlighter = textArea.getHighlighter(); // Сброс выделения в случае MouseClick на JTextArea
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

            add(checkedServerPanel);
            add(serverPanel);
            add(propertiesPanel);
            add(deploymentsPanel);
            add(progressBarPanel);
            add(portPanel);

            setResizable(false);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setVisible(true);
        }
    }
}