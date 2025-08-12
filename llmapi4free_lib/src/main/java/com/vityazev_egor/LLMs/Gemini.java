package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.LambdaWaitTask;
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
    private final WebElement textField, sendButton, stopResponseIcon;

    public Gemini(NoDriver driver){
        this.driver = driver;
        textField = driver.findElement(By.cssSelector(".ql-editor.ql-blank.textarea[role='textbox']"));
        sendButton = driver.findElement(By.cssSelector(".send-button-icon"));
        stopResponseIcon = driver.findElement(By.cssSelector("mat-icon[data-mat-icon-name='stop']"));
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

            boolean isPromptSent = false;
            for (int i=0; i<3; i++) {
                try {
                    textField.waitToAppear(5, 100);
                    driver.getInput().insertText(textField, prompt);
                    sendButton.waitToAppear(5, 100);
                    driver.getInput().emulateClick(sendButton);
                    stopResponseIcon.waitToAppear(5, 100);
                    isPromptSent = true;
                    break;
                } catch (Exception ex){
                    logger.warning("Could not send prompt on iteration #" + i);
                    driver.getCurrentUrl().ifPresent(url -> driver.getNavigation().loadUrlAndWait(url, 10));
                }
            }
            if (!isPromptSent)
                throw new Exception("Can't send prompt");
            var waitForResponse = new LambdaWaitTask(() -> !stopResponseIcon.isExists());
            if (!waitForResponse.execute(timeOutForAnswer, 100))
                throw new Exception("Timeout for answer");

            final String codeBlockBannerStyle = ".code-block-decoration.header-formatted.gds-title-s";
            int countCodeBlockBannersCount = driver.findElements(By.cssSelector(codeBlockBannerStyle)).size();
            for (int i=0; i<countCodeBlockBannersCount; i++)
                driver.findElement(By.cssSelector(codeBlockBannerStyle)).removeFromDOM();
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
