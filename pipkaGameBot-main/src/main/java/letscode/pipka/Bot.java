package letscode.pipka;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineQueryResultArticle;
import com.pengrad.telegrambot.request.AnswerInlineQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot {
    private final TelegramBot bot = new TelegramBot(System.getenv("BOT_TOKEN"));//getrnv-подтягивает из envirement
    private final String PROCESSING_LABEL = "...";
    private final static List<String> opponentWins = new ArrayList<String>() {{//варианты победы 2го
        add("01");
        add("12");
        add("20");
    }};
    private final static Map<String, String> items = new HashMap<String, String>() {{
        put("0", "\uD83D\uDD95");//выбор соперника в сообщении в результате
        put("1", "\uD83D\uDE1E");
        put("2", "\uD83E\uDD0F");
    }};

    public void serve() {
        bot.setUpdatesListener(updates -> {
            updates.forEach(this::process);//обрабатывает поштучно все, что прилетает
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void process(Update update) {//обрабатываем приход
        Message message = update.message(); //переменная для сообщений
        CallbackQuery callbackQuery = update.callbackQuery();//события----------------
        InlineQuery inlineQuery = update.inlineQuery();//переменная для обработки инлайнов

        BaseRequest request = null;

        if (message != null && message.viaBot() != null && message.viaBot().username().equals("pipkaGameq_bot")) {
            InlineKeyboardMarkup replyMarkup = message.replyMarkup();
            if (replyMarkup == null) {//проверяем наличие маркапа
                return;
            }

            InlineKeyboardButton[][] buttons = replyMarkup.inlineKeyboard();

            if (buttons == null) {//проверяем наличие кнопок
                return;
            }

            InlineKeyboardButton button = buttons[0][0];
            String buttonLabel = button.text();

            if (!buttonLabel.equals(PROCESSING_LABEL)) {//проверяем наличие надписи процессенш(...)
                return;
            }

            Long chatId = message.chat().id();  //-----------------------
            String senderName = message.from().firstName();
            String senderChose = button.callbackData();
            Integer messageId = message.messageId();

            request = new EditMessageText(chatId, messageId, message.text())//объект, который все хранит
                    .replyMarkup(//использование клавиатуры
                            new InlineKeyboardMarkup(//выбор соперника в собощениии
                                    new InlineKeyboardButton("\uD83D\uDD95")
                                            .callbackData(String.format("%d %s %s %s %d", chatId, senderName, senderChose, "0", messageId)),
                                    new InlineKeyboardButton("✌")
                                            .callbackData(String.format("%d %s %s %s %d", chatId, senderName, senderChose, "1", messageId)),
                                    new InlineKeyboardButton("\uD83E\uDD0F")
                                            .callbackData(String.format("%d %s %s %s %d", chatId, senderName, senderChose, "2", messageId))
                            )
                    );
        } else if (inlineQuery != null) {//если инлайн, артикл подходит для эмоджи(Выводит кнопки)
            InlineQueryResultArticle pipka = buildInlineButton("pipka", "\uD83D\uDD95 Писюн", "0");
            InlineQueryResultArticle scissors = buildInlineButton("scissors", "\uD83D\uDE18 Ножницы", "1");
            InlineQueryResultArticle ruler = buildInlineButton("ruler", "\uD83E\uDD0F Бумага", "2");

            request = new AnswerInlineQuery(inlineQuery.id(), pipka, scissors, ruler).cacheTime(1);
            //без кашетайма не работает при отправке инлайна
        } else if (callbackQuery != null) {
            String[] data = callbackQuery.data().split(" ");
            if (data.length < 4) {
                return;
            }
            Long chatId = Long.parseLong(data[0]);
            String senderName = data[1];
            String senderChose = data[2];
            String opponentChose = data[3];
            int messageId = Integer.parseInt(data[4]);
            String opponentName = callbackQuery.from().firstName();
//определение победителя
            if (senderChose.equals(opponentChose)) {//если ничья
                request = new EditMessageText(//редактируем сообщение
                        chatId, messageId,
                        String.format(
                                "%s и %s выбрали %s. Пипки примерно равны",
                                senderName,
                                opponentName,
                                items.get(senderChose)
                        )
                );
            } else if (opponentWins.contains(senderChose + opponentChose)) {//1й победил
                request = new EditMessageText(
                        chatId, messageId,
                        String.format(
                                "%s выбрал %s и отхватил от %s, выбравшего %s",
                                senderName, items.get(senderChose),
                                opponentName, items.get(opponentChose)//%s берет соотвествующую переменную
                        )
                );
            } else {//2й победил
                request = new EditMessageText(
                        chatId, messageId,
                        String.format(
                                "%s выбрал %s и отхватил от %s, выбравшего %s",
                                opponentName, items.get(opponentChose),
                                senderName, items.get(senderChose)
                        )
                );
            }
        }

        if (request != null) {
            bot.execute(request);
        }
    }

    private InlineQueryResultArticle buildInlineButton(String id, String title, String callbackData) {
        return new InlineQueryResultArticle(id, title, "Готов меряться пипкой!")//опрос соперника
                .replyMarkup(//использование клаивиатуры
                        new InlineKeyboardMarkup(
                                new InlineKeyboardButton(PROCESSING_LABEL).callbackData(callbackData)
                        )
                );
    }
}
