package WildFlyClient;

/*
    Класс отвечающий за создание подключения к серверу WildFly
* */

import org.jboss.as.controller.client.ModelControllerClient;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.net.InetAddress;

import static WildFlyClient.Main.MessageBox;

class Connection {
    private final String loginTxt = Autorization.loginTxt.getText();    //Используем учетные данные из полей введенных в поля формы авторизации
    private final String passwordTxt = Autorization.passwordTxt.getText();
    static ModelControllerClient client;

    Connection(String currentHost) throws IOException {
        int localport = Integer.parseInt(UI.Window.portField.getText());
        try {
            client = ModelControllerClient.Factory.create(
                    InetAddress.getByName(currentHost), localport,
                    callbacks -> {
                        for (Callback current : callbacks) {
                            if (current instanceof NameCallback) {
                                NameCallback ncb = (NameCallback) current;
                                ncb.setName(loginTxt);
                            } else if (current instanceof PasswordCallback) {
                                PasswordCallback pcb = (PasswordCallback) current;
                                //pcb.setPassword(String.valueOf(passwordTxt.getPassword().toCharArray()));
                                pcb.setPassword(passwordTxt.toCharArray());
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
        } catch (java.lang.NullPointerException e) {
            MessageBox(new Exception("Не выбран сервер для загрузки конфигурации! "));
        }
    }
}