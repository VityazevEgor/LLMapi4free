package com.vityazev_egor.LLMs.Copilot.Modules;

import java.util.List;
import java.util.Optional;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.WebElements.WebElement;
import com.vityazev_egor.LLMs.Shared;
import com.vityazev_egor.Models.ChatAnswer;

public class Ask {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Ask(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(Ask.class.getName());
    }

    public ChatAnswer askCopilot(String promt, Integer timeOutForAnswer){
        if (enterPromt(promt) && waitForAnswer(timeOutForAnswer)){
            return new ChatAnswer(
                getLastAnswerText(), 
                getLastAnswerHtml(),
                driver.getMisc().captureScreenshot()
            );
        } else{
            return new ChatAnswer();
        }
    }

    private Boolean enterPromt(String promt){
        var userInput = driver.findElement(By.id("userInput"));
        var sendButton = driver.findElement(By.cssSelector("button[title='Submit message']"));
        var continueButton = driver.findElement(By.cssSelector("button[title='Continue']"));

        if (!Shared.waitForElements(false, userInput)){
            logger.warning("Can't find user input!");
            return false;
        }

        if (continueButton.isExists()){
            driver.getInput().emulateClick(continueButton);
        }
        driver.getInput().emulateClick(userInput);
        driver.getInput().enterText(userInput, promt);

        if (!Shared.waitForElements(false, sendButton)){
            logger.warning("Can't find send button");
            return false;
        }
        // надо немного подождать, а то просто не успевает кнопочка отобразиться
        com.vityazev_egor.Core.Shared.sleep(1000);
        driver.getInput().emulateClick(sendButton);
        return true;
    }

    private Boolean waitForAnswer(Integer timeOutForAnswer){
        var waitTask = new WaitTask() {
            private String html = driver.getHtml().map(result -> {return result;}).orElse("");

            @Override
            public Boolean condition() {
                // если текущий штмл равен предыдущему то возвращаем да (копайлот перестал печатать)
                return driver.getHtml().map(currentHtml ->{
                    if (currentHtml.equals(html)){
                        return true;
                    }
                    else{
                        html = currentHtml;
                        return false;
                    }
                }).orElse(false);
            }
            
        };
        com.vityazev_egor.Core.Shared.sleep(1000);
        return waitTask.execute(timeOutForAnswer, 2 * 1000);
    }

    private Optional<WebElement> getLastAnswerElement(){
        List<WebElement> elements = driver.findElements(By.cssSelector("div[data-content='ai-message']"));
        if (elements.isEmpty()) {
            logger.error("Can't find last answer elements", null);
            return Optional.empty();
        }
        return Optional.of(elements.get(elements.size()-1));
    }

    private Optional<String> getLastAnswerHtml(){
        var answerElement = getLastAnswerElement();
        return answerElement.map(element ->{
            return element.getHTMLContent();
        }).orElse(Optional.empty());
    }

    private Optional<String> getLastAnswerText(){
        return getLastAnswerElement().map(element ->{
            return element.getText();
        }).orElse(Optional.empty());
    }
}
