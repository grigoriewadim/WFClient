package WildFlyClient;

/*
  Проект для удаленного управления и конфигурирования серверами на базе WildFly (JBoss Application Server),
  проект в процессе создания.

  Main класс, вызывает форму авторизации.

* */

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static WildFlyClient.Functions.GetDate;

public class Main {

    static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception, "Ошибка", JOptionPane.ERROR_MESSAGE); //Вывод окна с эксепшеном
        File filename = new File("exception_" + GetDate() + "_.log"); // Обявляем файл для записи эксепшенов
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(GetDate() + "_" + exception.toString() + "\n");
            writer.write(GetDate() + "_" + Arrays.toString(exception.getStackTrace()) + "\n");
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