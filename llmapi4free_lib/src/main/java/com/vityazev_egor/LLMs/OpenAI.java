package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.LambdaWaitTask;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.iChat;
import com.vityazev_egor.Models.ChatAnswer;

import java.awt.*;

public class OpenAI implements iChat{

    public static final String url = "https://chatgpt.com/";
    private final CustomLogger logger = new CustomLogger(OpenAI.class.getName());

    private final NoDriver driver;
    public OpenAI(NoDriver driver) {
        this.driver = driver;
    }

    /**
     * Checks if the user is currently logged in.
     *
     * @return TRUE if the user is logged in, FALSE otherwise
     */
    private Boolean isLoggedIn() {
        var profileImage = driver.findElement(By.cssSelector("img[alt='Profile image']"));
        return Shared.waitForElements(false, 10, profileImage);
    }

    /**
     * Authenticates a user with Google account.
     * @return true if authentication is successful, false otherwise.
     */
    @Override
    public Boolean auth() {
        try {
            if (!createNewChat())
                throw new Exception("Can't create new chat");

            if (isLoggedIn())
                return true;

            var welcomeLoginButton = driver.findElement(By.cssSelector("button[data-testid='welcome-login-button']"));
            var loginButton = driver.findElement(By.cssSelector("button[data-testid='login-button']"));
            if (welcomeLoginButton.isExists())
                driver.getInput().emulateClick(welcomeLoginButton);
            else
                driver.getInput().emulateClick(loginButton);

            var googleAuthButton = driver.findElement(By.cssSelector("button[value='google']"));
            googleAuthButton.waitToAppear(10, 100);

            driver.getInput().emulateClick(googleAuthButton);
            return isLoggedIn();
        } catch (Exception ex) {
            logger.error("Authentication failed: " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Sends a prompt to the OpenAI chatbot and waits for an answer.
     *
     * @param prompt The user's input prompt to be sent to the AI.
     * @param timeOutForAnswer The maximum time (in seconds) to wait for an answer from the AI.
     * @return A {@link ChatAnswer} object containing the AI's response, HTML content, and a screenshot of the chat.
     */
    @Override
    public ChatAnswer ask(String prompt, Integer timeOutForAnswer) {
        try{
            // MAKE SURE THAT CHAT IS OPENED
            Boolean chatIsOpened = driver.getHtml().map(html->html.contains("ChatGPT")).orElse(false);
            if (!chatIsOpened){
                if (!auth())
                    throw new Exception("Can't open chat");
            }

            // SEND PROMPT
            var input = driver.findElement(By.id("prompt-textarea"));
            input.waitToAppear(5, 100);
            driver.getInput().insertText(input, prompt);
            var sendButton = driver.findElement(By.id("composer-submit-button"));
            sendButton.waitToAppear(5, 100);
            driver.getInput().emulateClick(sendButton);

            // WAIT FOR ANSWER AND GET TEXT, HTML AND IMAGE OF IT
            if (!Shared.waitForAnswer(driver, timeOutForAnswer, 2000))
                throw new Exception("Can't get answer from AI in time");
            var answerBlocks = driver.findElements(By.cssSelector("div[data-message-author-role='assistant']"));
            if (answerBlocks.isEmpty())
                throw new Exception("Could not get answer from OpenAI");
            var latestAnswer = answerBlocks.getLast();
            return new ChatAnswer(
                    latestAnswer.getText(),
                    latestAnswer.getHTMLContent(),
                    driver.getMisc().captureScreenshot()
            );
        }
        catch (Exception ex){
            logger.error("Error occurred while processing the prompt: " + ex.getMessage(), ex);
            return new ChatAnswer();
        }
    }

    /**
     * Creates a new chat by navigating to the OpenAI URL and attempting to bypass any CAPTCHA challenges.
     *
     * @return true if the chat was successfully created or no CAPTCHA needed, false otherwise
     */
    @Override
    public Boolean createNewChat() {
        driver.getNavigation().loadUrlAndWait(OpenAI.url, 10);
        var cfPleaseWait = driver.findElement(By.id("cf-please-wait"));

        if (!cfPleaseWait.isExists()) {
            logger.info("There is not need to bypass cf challenge");
            return true;
        }

        var bypassCf = new LambdaWaitTask(() -> {
            if (!cfPleaseWait.isExists()) return true;
            var spacer = driver.findElement(By.className("spacer"));
            try {
                Dimension size = spacer.getSize().orElseThrow(() -> new Exception("Could not get size of captcha"));
                Point position = spacer.getPosition().orElseThrow(() -> new Exception("Could not get position of captcha"));
                Double yClick = position.getY();
                Double xClick = position.getX() - size.getWidth() / 2 + 20;
                driver.getXdo().click(xClick, yClick);
                return false;
            } catch (Exception ex) {
                logger.error("Could not click on captcha", ex);
                return false;
            }
        });

        return bypassCf.execute(20, 500);
    }

    @Override
    public String getName() {
        return "OpenAI";
    }
    
}
