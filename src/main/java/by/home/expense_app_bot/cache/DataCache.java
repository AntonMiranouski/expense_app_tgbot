package by.home.expense_app_bot.cache;

import by.home.expense_app_bot.botapi.BotState;
import by.home.expense_app_bot.botapi.UserProfileData;

public interface DataCache {
    void setUsersCurrentBotState(int userId, BotState botState);

    BotState getUsersCurrentBotState(int userId);

    UserProfileData getUserProfileData(int userId);

    void saveUserProfileData(int userId, UserProfileData userProfileData);
}
