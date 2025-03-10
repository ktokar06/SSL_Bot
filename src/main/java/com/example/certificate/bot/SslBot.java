package com.example.certificate.bot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.certificate.configuration.HostItem;
import com.example.certificate.configuration.HostProperties;
import com.example.certificate.exception.ServiceException;
import com.example.certificate.service.SslService;
import com.example.certificate.service.SslServiceImpl;

@Component
@Configuration
@EnableScheduling
public class SslBot extends TelegramLongPollingBot {

    private static final String USERS_FILE_PATH = "active_users.txt";
    private static final Logger LOG = LoggerFactory.getLogger(SslBot.class);
    private static final String START = "/start";
    private static final String SSL = "/ssl";

    @Autowired
    private SslService sslService;

    @Autowired
    private HostProperties configProperties;

    private Set<Long> activeUsers = new HashSet<>();

    public SslBot(@Value("${bot.token}") String botToken, SslServiceImpl sslService) {
        super(botToken);
        loadActiveUsersFromFile();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String message = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();

        activeUsers.add(chatId);

        switch (message) {
            case START -> {
                String userName = update.getMessage().getChat().getUserName();
                startCommand(chatId, userName);
            }
            case SSL -> {
                try {
                    sslCommand(chatId);
                    saveActiveUsersToFile();

                } catch (ServiceException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "@telegramname";
    }

    private void startCommand(Long chatId, String userName) {
        var text = """
                Добро пожаловать в бот, %s!
                
                Команда /start - приветственное сообщение
                
                Команда /ssl - Отправляет данные о SSL - Сертификатах
                
                Здесь вы сможете проверить, просрочен ли SSL-сертификат.
                """;
        var formattedText = String.format(text, userName);
        sendMessage(chatId, formattedText);
    }

    private void sslCommand(Long chatId) throws ServiceException {
        String formattedText = createFormattedText();
        sendMessage(chatId, formattedText);
    }

    @Scheduled(fixedRate = 1800000)// Время установил на 30 мин
    public void scheduledTask() throws ServiceException {
        for (Long chatId : activeUsers) {
            if (!activeUsers.contains(chatId)) {
                continue;
            }
            String formattedText = createFormattedText();
            sendMessage(chatId, formattedText);
        }
        saveActiveUsersToFile();
    }

    private String createFormattedText() throws ServiceException {
        String sslInfo = "";

        List<HostItem> items = configProperties.getItems();
        if (items != null && !items.isEmpty()) {
            for (HostItem item : items) {
                sslInfo += sslService.getSslCertificatePay(item.getUrl(), item.getName());
            }
        } else {
            sslInfo = "Список пуст.";
        }

        if (sslInfo != null && !sslInfo.isEmpty()) {
            return String.format("Дата запроса: %s\n\n%s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    sslInfo);
        } else {
            return "Сертификаты не получены.";
        }
    }

    private void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки сообщения", e);
        }
    }

    private void loadActiveUsersFromFile() {
        try {
            File file = new File(USERS_FILE_PATH);
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    long chatId = Long.parseLong(scanner.nextLine());
                    activeUsers.add(chatId);
                }
                scanner.close();
            }
        } catch (IOException | NumberFormatException e) {
            LOG.error("Ошибка загрузки активного списка пользователей", e);
        }
    }

    private void saveActiveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(USERS_FILE_PATH)))) {
            for (long chatId : activeUsers) {
                writer.println(chatId);
            }
        } catch (IOException e) {
            LOG.error("Ошибка сохранения активного списка пользователей", e);
        }
    }

}