package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.iChat;
import com.vityazev_egor.Models.ChatAnswer;

public class DuckDuck implements iChat{

    private final NoDriver driver;
    private String answerDivClass = "";
    private final String url = "https://duckduckgo.com/?q=DuckDuckGo&ia=chat";
    private final CustomLogger logger = new CustomLogger(DuckDuck.class.getName());

    public DuckDuck(NoDriver driver){
        this.driver = driver;
    }

    /**
     *
     * @return {@code true} if authentication is successful, {@code false} otherwise.
     */
    @Override
    public Boolean auth() {
        try{
            if (!driver.getNavigation().loadUrlAndWait(url, 10))
                throw new Exception("Could not load DuckDuckGo chat in time.");

            var promptInput = driver.findElement(By.name("user-prompt"));
            if (Shared.waitForElements(false, 2, promptInput))
                return true;

            var getStartedButton = driver.findElement(By.cssSelector("button[type='button'][tabindex='1']"));
            if (!getStartedButton.isExists())
                throw new Exception("Can't find 'Get Started' button");

            driver.executeJS("Array.from(document.querySelectorAll('button')).find(el => el.textContent === 'Далее' || el.textContent === 'Next').click();");
            driver.executeJS("Array.from(document.querySelectorAll('button')).find(el => el.textContent === 'Принимаю условия' || el.textContent === 'I Agree').click();");

            promptInput.waitToAppear(5, 100);
            return true;
        } catch (Exception ex){
            logger.error("Error occurred while opening chat: " + ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public ChatAnswer ask(String prompt, Integer timeOutForAnswer) {
        try {
            // MAKE SURE THAT CHAT IS OPENED
            Boolean isChatOpened = driver.getTitle().map(title -> title.contains("Duck.ai")).orElse(false);
            if (!isChatOpened)
                if (!auth() || !createNewChat())
                    throw new Exception("Could not open chat");

            // SEND PROMPT
            var textArea = driver.findElement(By.name("user-prompt"));
            var sendButton = driver.findElement(By.cssSelector("button[type='submit'][aria-label='Отправить'], button[type='submit'][aria-label='Send']"));
            textArea.waitToAppear(5, 100);
            sendButton.waitToAppear(5, 100);
            driver.getInput().insertText(textArea, prompt);
            com.vityazev_egor.Core.Shared.sleep(1000);
            driver.getInput().emulateClick(sendButton);

            // GET ANSWER
            if (!Shared.waitForAnswer(driver, timeOutForAnswer, 2000))
                throw new Exception("Time out while waiting for duck duck answer");
            var answerBlocks = driver.findElements(By.cssSelector(buildCssSelector(answerDivClass)));
            if (answerBlocks.isEmpty())
                throw new Exception("Could not find answer div");
            var latestAnswer = answerBlocks.getLast();
            return new ChatAnswer(
                    latestAnswer.getText(),
                    latestAnswer.getHTMLContent(),
                    driver.getMisc().captureScreenshot()
            );
        }
        catch (Exception ex){
            logger.error("Error occurred while sending prompt: " + ex.getMessage(), ex);
            return new ChatAnswer();
        }
    }

    @Override
    public Boolean createNewChat() {
        try {
            if (!driver.getNavigation().loadUrlAndWait(url, 10))
                throw new Exception("Could not load duck duck in time");

            var textArea = driver.findElement(By.name("user-prompt"));
            var sendButton = driver.findElement(By.cssSelector("button[type='submit'][aria-label='Отправить'], button[type='submit'][aria-label='Send']"));

            if (!Shared.waitForElements(false, textArea, sendButton))
                throw new Exception("Could not find prompt text area or send button!");

            driver.getInput().insertText(textArea, "Напиши слово \"Lol kek\"");
            com.vityazev_egor.Core.Shared.sleep(1000);
            driver.getInput().emulateClick(sendButton);

            if (!Shared.waitForAnswer(driver, 40, 2000))
                throw new Exception("Time out while waiting for duck duck answer");

            return com.vityazev_egor.Core.Shared.readResource("scripts/detectDuckAnswerDiv.js").map(script -> {
                var result = driver.executeJSAndGetResult(script);
                if (result.isPresent() && !result.get().contains("not found")) {
                    answerDivClass = result.get();
                    return true;
                } else {
                    logger.error("Can't find answer div", null);
                    return false;
                }
            }).orElse(false);
        }catch (Exception ex){
            logger.error("Error occurred while sending message: " + ex.getMessage(), ex);
            return false;
        }
    }

    private String buildCssSelector(String classNames) {
        if (classNames == null || classNames.trim().isEmpty()) {
            return "";
        }
        
        String[] classes = classNames.trim().split("\\s+");
        StringBuilder selector = new StringBuilder();
        
        for (String className : classes) {
            if (!className.isEmpty()) {
                selector.append(".").append(className);
            }
        }
        
        return selector.toString();
    }

    @Override
    public String getName() {
        return "DuckDuck";
    }
    
}
