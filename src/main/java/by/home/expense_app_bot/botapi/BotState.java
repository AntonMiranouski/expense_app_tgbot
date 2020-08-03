package by.home.expense_app_bot.botapi;

/**Возможные состояния бота
 */

public enum BotState {
    ASK_LOGIN,
    ASK_PASS,
    ASK_AUTHORIZATION,
    ASK_DATE,
    ASK_AMOUNT,
    ASK_DESCRIPTION,
    PRE_AUTHORIZATION,
    FILLING_PROFILE,
    PROFILE_FILLED,
    AUTHORIZED,
    SHOW_MAIN_MENU,
    SHOW_CARD_MENU;
}
