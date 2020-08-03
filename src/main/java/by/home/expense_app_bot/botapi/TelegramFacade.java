package by.home.expense_app_bot.botapi;

import by.home.expense_app_bot.util.HTTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import by.home.expense_app_bot.cache.UserDataCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class TelegramFacade {
    private BotStateContext botStateContext;
    private UserDataCache userDataCache;

    public TelegramFacade(BotStateContext botStateContext, UserDataCache userDataCache) {
        this.botStateContext = botStateContext;
        this.userDataCache = userDataCache;
    }

    public BotApiMethod<?> handleUpdate(Update update) {
        SendMessage replyMessage = null;

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            log.info("New callbackQuery from User: {}, userId: {}, with data: {}", update.getCallbackQuery().getFrom().getUserName(),
                    callbackQuery.getFrom().getId(), update.getCallbackQuery().getData());
            return processCallbackQuery(callbackQuery);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            log.info("New message from User:{}, userId: {}, chatId: {},  with text: {}",
                    message.getFrom().getUserName(), message.getFrom().getId(), message.getChatId(), message.getText());
            replyMessage = handleInputMessage(message);
        }

        return replyMessage;
    }

    private SendMessage handleInputMessage(Message message) {
        String inputMsg = message.getText();
        int userId = message.getFrom().getId();
        BotState botState;
        SendMessage replyMessage;

        switch (inputMsg) {
            case "/start":
                botState = BotState.ASK_LOGIN;
                break;
            default:
                botState = userDataCache.getUsersCurrentBotState(userId);
                break;
        }

        userDataCache.setUsersCurrentBotState(userId, botState);

        replyMessage = botStateContext.processInputMessage(botState, message);

        return replyMessage;
    }

    private BotApiMethod<?> processCallbackQuery(CallbackQuery buttonQuery) {
        final long chatId = buttonQuery.getMessage().getChatId();
        final int userId = buttonQuery.getFrom().getId();
        BotApiMethod<?> callBackAnswer = null;
        UserProfileData profileData = userDataCache.getUserProfileData(userId);

        if (buttonQuery.getData().equals("buttonBalance")) {

            String link = "https://telegramtest-developer-edition.ap17.force.com/services/apexrest/ttest";
            String result = "";
            String jsonInputString = "{\"id\": \"" + profileData.getForceId() + "\"}";

            result = HTTPUtil.getResponse(link, result, jsonInputString);

            if (result.startsWith("Balance")) {
                callBackAnswer = new SendMessage(chatId, result.replace("Balance", ""));
            } else {
                callBackAnswer = new SendMessage(chatId, result);
            }

        } else if (buttonQuery.getData().equals("buttonCard")) {
            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DATE);
            SendMessage sendMessage = new SendMessage(chatId, "On what date would you like to create a card?" + "\n" + "{ yyyy-mm-dd }");
            sendMessage.setReplyMarkup(getCardMarkup());
            return sendMessage;
        }

        if (buttonQuery.getData().equals("buttonToday")) {
            Date date = Calendar.getInstance().getTime();
            String today = new SimpleDateFormat("yyyy-MM-dd").format(date);

            profileData.setDate(today);
            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_AMOUNT);
            callBackAnswer = new SendMessage(chatId, "For what amount?");

        } else if (buttonQuery.getData().equals("buttonYesterday")) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.DATE, -1);
            String today = new SimpleDateFormat("yyyy-MM-dd").format(date.getTime());

            profileData.setDate(today);
            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_AMOUNT);
            callBackAnswer = new SendMessage(chatId, "For what amount?");
        }

        return callBackAnswer;
    }

    private InlineKeyboardMarkup getCardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton buttonToday = new InlineKeyboardButton().setText("Today");
        InlineKeyboardButton buttonCalendar = new InlineKeyboardButton().setText("Yesterday");

        buttonToday.setCallbackData("buttonToday");
        buttonCalendar.setCallbackData("buttonYesterday");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(buttonToday);
        keyboardButtonsRow1.add(buttonCalendar);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
}
