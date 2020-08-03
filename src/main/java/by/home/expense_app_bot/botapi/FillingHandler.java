package by.home.expense_app_bot.botapi;

import by.home.expense_app_bot.util.HTTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import by.home.expense_app_bot.cache.UserDataCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FillingHandler implements InputMessageHandler {
    private UserDataCache userDataCache;

    public FillingHandler(UserDataCache userDataCache) {
        this.userDataCache = userDataCache;
    }

    @Override
    public SendMessage handle(Message message) {
        if (userDataCache.getUsersCurrentBotState(message.getFrom().getId()).equals(BotState.FILLING_PROFILE)) {
            userDataCache.setUsersCurrentBotState(message.getFrom().getId(), BotState.ASK_LOGIN);
        }
        return processUsersInput(message);
    }

    @Override
    public BotState getHandlerName() {
        return BotState.FILLING_PROFILE;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        String usersAnswer = inputMsg.getText();
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        UserProfileData profileData = userDataCache.getUserProfileData(userId);
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        SendMessage replyToUser = null;

        if (botState.equals(BotState.ASK_LOGIN)) {
            replyToUser = new SendMessage(chatId, "Enter Login");
            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_PASS);
        }

        if (botState.equals(BotState.ASK_PASS)) {
            profileData.setLogin(usersAnswer);
            replyToUser = new SendMessage(chatId, "Enter Password");
            userDataCache.setUsersCurrentBotState(userId, BotState.PRE_AUTHORIZATION);
        }

        if (botState.equals(BotState.PRE_AUTHORIZATION)) {
            profileData.setPass(usersAnswer);
            String link = "https://telegramtest-developer-edition.ap17.force.com/services/apexrest/ttest";
            String result = "";
            String jsonInputString = "{\"login\": \"" + profileData.getLogin() + "\", \"pass\": \"" + profileData.getPass() + "\"}";

            result = HTTPUtil.getResponse(link, result, jsonInputString);

            if (result.startsWith("Id")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.AUTHORIZED);
                profileData.setForceId(result.replace("Id", ""));
                replyToUser = new SendMessage(chatId, "Authorization was successful");
                replyToUser.setReplyMarkup(getMainMenuMarkup());
            } else {
                replyToUser = new SendMessage(chatId, result);
            }
        }

        if (botState.equals(BotState.ASK_DATE)) {
            if (isValidDate(usersAnswer)) {
                profileData.setDate(usersAnswer);
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_AMOUNT);
                replyToUser = new SendMessage(chatId, "For what amount?");
            } else {
                replyToUser = new SendMessage(chatId, "Please enter a valid date");
            }
        }

        if (botState.equals(BotState.ASK_AMOUNT)) {
            if (usersAnswer.matches("[0-9]+") && usersAnswer.length() > 0) {
                if (Integer.parseInt(usersAnswer) > 0) {
                    profileData.setAmount(usersAnswer);
                    userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESCRIPTION);
                    replyToUser = new SendMessage(chatId, "With what description?");
                } else replyToUser = new SendMessage(chatId, "Please enter a valid amount");
            } else replyToUser = new SendMessage(chatId, "Please enter a valid amount");
        }

        if (botState.equals(BotState.ASK_DESCRIPTION)) {
            userDataCache.setUsersCurrentBotState(userId, BotState.AUTHORIZED);
            String link = "https://telegramtest-developer-edition.ap17.force.com/services/apexrest/ttest";
            String result = "";
            String jsonInputString =
                    "{\"day\": \"" + profileData.getDate() + "\", \"amount\": \"" + profileData.getAmount() + "\", \"description\": \"" + usersAnswer + "\", \"id\": \"" + profileData.getForceId() + "\"}";
            System.out.println(jsonInputString);
            result = HTTPUtil.getResponse(link, result, jsonInputString);

            replyToUser = new SendMessage(chatId, result);
        }

        userDataCache.saveUserProfileData(userId, profileData);

        return replyToUser;
    }

    private InlineKeyboardMarkup getMainMenuMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton buttonBalance = new InlineKeyboardButton().setText("Current Balance");
        InlineKeyboardButton buttonCard = new InlineKeyboardButton().setText("Create Card");

        buttonBalance.setCallbackData("buttonBalance");
        buttonCard.setCallbackData("buttonCard");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(buttonBalance);
        keyboardButtonsRow1.add(buttonCard);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }

    private boolean isValidDate(String input) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);

        try {
            format.parse(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}



