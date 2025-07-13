package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.WebElements.WebElement;
import com.vityazev_egor.Models.ChatAnswer;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.iChat;


public class Gemini implements iChat {
    private final NoDriver driver;
    private final String url = "https://gemini.google.com/app";
    private final CustomLogger logger = new CustomLogger(Gemini.class.getName());
    private final WebElement textField, sendButton;

    public Gemini(NoDriver driver){
        this.driver = driver;
        textField = driver.findElement(By.cssSelector(".ql-editor.ql-blank.textarea[role='textbox']"));
        sendButton = driver.findElement(By.cssSelector(".send-button-icon"));
    }

    @Override
    public Boolean auth() {
        if (driver.getCurrentUrl().map(currentUrl -> currentUrl.contains(url)).orElse(false))
            return true;
        return createNewChat();
    }

    @Override
    public ChatAnswer ask(String prompt, Integer timeOutForAnswer) {
        try {
            if (!auth())
                throw new Exception("Could not open chat");

            driver.getInput().insertText(textField, prompt);
            Shared.sleep(500);
            sendButton.waitToAppear(2, 200);
            driver.getInput().emulateClick(sendButton);
            if (!com.vityazev_egor.LLMs.Shared.waitForAnswer(driver, timeOutForAnswer, 200))
                throw new Exception("Timeout for answer");

            var answerElements = driver.findElements(By.cssSelector("message-content.model-response-text"));
            if (answerElements.isEmpty())
                throw new Exception("No answer found");
            var latestAnswer = answerElements.getLast();
            return new ChatAnswer(
                    latestAnswer.getText(),
                    latestAnswer.getHTMLContent(),
                    driver.getMisc().captureScreenshot()
            );
        }
        catch (Exception ex){
            logger.error("Error during sending message: " + ex.getMessage(), ex);
        }
        return new ChatAnswer();
    }

    @Override
    public Boolean createNewChat() {
        try {
            driver.getNavigation().loadUrlAndWait(url, 10);
            textField.waitToAppear(5, 400);
            return true;
        } catch (Exception ex){
            logger.error("Can't find text field: ", ex);
            return false;
        }
    }

    @Override
    public String getName() {
        return "Gemini";
    }
}
