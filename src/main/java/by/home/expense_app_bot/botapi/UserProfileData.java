package by.home.expense_app_bot.botapi;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Данные анкеты пользователя
 */

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileData {
    String login;
    String pass;
    String forceId;
    String date;
    String amount;
}
